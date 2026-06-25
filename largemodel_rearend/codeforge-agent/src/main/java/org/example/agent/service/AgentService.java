/**
 * Agent 工作流服务
 * 功能：工作流 CRUD + 顺序链式执行 + SSE 进度推送
 * 作者：yx
 */
package org.example.agent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.mapper.AgentWorkflowMapper;
import org.example.entity.AgentWorkflow;
import org.example.service.ai.DynamicModelProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class AgentService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AgentWorkflowMapper workflowMapper;
    private final DynamicModelProvider modelProvider;

    /** Agent 角色提示词（默认值，优先从文件加载） */
    private static final Map<String, String> DEFAULT_ROLES = Map.of(
            "analyzer", "你是资深需求分析师。将用户需求拆解为清晰的功能规格说明。输出要点列表，每点一行。",
            "architect", "你是资深系统架构师。根据需求设计系统架构、模块划分和技术选型。输出架构设计文档。",
            "coder", "你是资深全栈工程师。根据架构设计生成完整的可运行代码。使用代码块包裹。",
            "tester", "你是资深测试工程师。为上述代码生成单元测试和集成测试。使用代码块包裹。",
            "reviewer", "你是资深代码审查专家。审查上述所有产出，给出改进建议和最终评分(1-10)。"
    );

    /** 加载 Agent 角色提示词（文件优先，回退默认值） */
    private static String loadRolePrompt(String agent) {
        try {
            var resource = new ClassPathResource("prompt/agent-" + agent + ".txt");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return DEFAULT_ROLES.getOrDefault(agent,
                    "你是 AI 助手。根据上下文执行你的职责。");
        }
    }

    public AgentService(AgentWorkflowMapper workflowMapper, DynamicModelProvider modelProvider) {
        this.workflowMapper = workflowMapper;
        this.modelProvider = modelProvider;
    }

    /** 创建工作流 */
    @Transactional
    public AgentWorkflow create(String name, String description, String agentChain, String requirement, Long userId) {
        AgentWorkflow wf = AgentWorkflow.builder()
                .name(name)
                .description(description)
                .agentChain(agentChain)
                .requirement(requirement)
                .status("PENDING")
                .userId(userId)
                .build();
        workflowMapper.insert(wf);
        log.info("创建工作流, id={}, name={}, chain={}", wf.getId(), name, agentChain);
        return wf;
    }

    /** 更新工作流 */
    @Transactional
    public void update(Long id, Long userId, String name, String description) {
        AgentWorkflow wf = getById(id, userId);
        if (name != null && !name.isBlank()) wf.setName(name);
        if (description != null) wf.setDescription(description);
        workflowMapper.updateById(wf);
    }

    /** 工作流列表 */
    public IPage<AgentWorkflow> listByUser(Long userId, int page, int size) {
        return workflowMapper.findByUserId(Page.of(page, size), userId);
    }

    /** 工作流详情 */
    public AgentWorkflow getById(Long id, Long userId) {
        AgentWorkflow wf = workflowMapper.selectById(id);
        if (wf == null || !wf.getUserId().equals(userId)) {
            throw new RuntimeException("工作流不存在或无权访问");
        }
        return wf;
    }

    /** 删除工作流 */
    @Transactional
    public void delete(Long id, Long userId) {
        getById(id, userId);
        workflowMapper.deleteById(id);
    }

    /** 执行工作流 — SSE 流式返回各阶段进度 */
    public SseEmitter execute(Long workflowId, Long userId) {
        AgentWorkflow wf = getById(workflowId, userId);
        String[] agents = wf.getAgentChain().split(",");
        SseEmitter emitter = new SseEmitter(600_000L);

        wf.setStatus("RUNNING");
        workflowMapper.updateById(wf);

        StringBuilder fullResult = new StringBuilder();
        AtomicBoolean failed = new AtomicBoolean(false);

        Thread.ofVirtual().start(() -> {
            try {
                String context = wf.getRequirement();
                for (int i = 0; i < agents.length; i++) {
                    if (failed.get()) break;
                    final int phaseNum = i + 1;
                    final String agent = agents[i].trim();
                    final String rolePrompt = loadRolePrompt(agent);

                    // 发送阶段开始事件
                    Map<String, Object> phase = new LinkedHashMap<>();
                    phase.put("type", "phase");
                    phase.put("phase", phaseNum);
                    phase.put("total", agents.length);
                    phase.put("agent", agent);
                    phase.put("status", "running");
                    emitter.send(SseEmitter.event().name("phase")
                            .data(MAPPER.writeValueAsString(phase)));

                    // 构建消息
                    List<ChatMessage> messages = new ArrayList<>();
                    messages.add(SystemMessage.from(rolePrompt));
                    messages.add(UserMessage.from(
                            "任务上下文：\n" + context + "\n\n请执行你的职责并输出结果。"));

                    // 流式执行当前 Agent（使用 CountDownLatch 替代忙等）
                    StringBuilder agentOutput = new StringBuilder();
                    CountDownLatch latch = new CountDownLatch(1);
                    AtomicReference<Throwable> agentError = new AtomicReference<>();

                    StreamingChatLanguageModel model = modelProvider.getDefaultStreaming();
                    model.chat(messages, new StreamingChatResponseHandler() {
                        @Override public void onPartialResponse(String token) {
                            if (failed.get()) return;
                            agentOutput.append(token);
                            try {
                                Map<String, Object> progress = new LinkedHashMap<>();
                                progress.put("type", "progress");
                                progress.put("phase", phaseNum);
                                progress.put("agent", agent);
                                progress.put("token", token);
                                emitter.send(SseEmitter.event().name("progress")
                                        .data(MAPPER.writeValueAsString(progress)));
                            } catch (IOException e) { failed.set(true); latch.countDown(); }
                        }

                        @Override public void onCompleteResponse(ChatResponse response) {
                            latch.countDown();
                        }

                        @Override public void onError(Throwable error) {
                            agentError.set(error);
                            latch.countDown();
                        }
                    });

                    // 等待当前 Agent 完成（最多 3 分钟）
                    boolean completed = latch.await(180, TimeUnit.SECONDS);

                    if (agentError.get() != null || !completed) {
                        failed.set(true);
                        Map<String, Object> err = new LinkedHashMap<>();
                        err.put("type", "error");
                        err.put("phase", phaseNum);
                        err.put("agent", agent);
                        err.put("error", agentError.get() != null
                                ? agentError.get().getMessage() : "Agent 超时");
                        emitter.send(SseEmitter.event().name("error")
                                .data(MAPPER.writeValueAsString(err)));
                        break;
                    }

                    context += "\n\n===== " + agent + " 输出 =====\n" + agentOutput;
                    fullResult.append("## ").append(agent).append("\n\n")
                            .append(agentOutput).append("\n\n");
                }

                // 更新工作流状态
                wf.setStatus(failed.get() ? "FAILED" : "COMPLETED");
                wf.setResult(fullResult.toString());
                workflowMapper.updateById(wf);

                Map<String, Object> done = new LinkedHashMap<>();
                done.put("type", "done");
                done.put("workflowId", wf.getId());
                done.put("status", wf.getStatus());
                done.put("result", fullResult.toString());
                emitter.send(SseEmitter.event().name("done")
                        .data(MAPPER.writeValueAsString(done)));
                emitter.complete();

            } catch (Exception e) {
                log.error("工作流执行失败", e);
                wf.setStatus("FAILED");
                wf.setResult(e.getMessage());
                workflowMapper.updateById(wf);
                try {
                    Map<String, Object> err = Map.of("type", "error", "error", e.getMessage());
                    emitter.send(SseEmitter.event().name("error")
                            .data(MAPPER.writeValueAsString(err)));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            wf.setStatus("FAILED");
            workflowMapper.updateById(wf);
            emitter.complete();
        });
        return emitter;
    }
}
