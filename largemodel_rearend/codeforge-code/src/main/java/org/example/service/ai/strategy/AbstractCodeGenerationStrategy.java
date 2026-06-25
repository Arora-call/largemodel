/**
 * 模块：AI代码生成 - 模板方法模式
 * 功能：抽象策略基类，定义代码生成的通用流程，子类实现具体的提示词、解析和持久化逻辑
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.GenerateCodeRequest;
import org.example.entity.Conversation;
import org.example.entity.Message;
import org.example.entity.ProjectFile;
import org.example.mapper.ProjectFileMapper;
import org.example.repository.ConversationRepository;
import org.example.repository.MessageRepository;
import org.example.service.MonitorService;
import org.example.service.ai.DynamicModelProvider;
import org.example.service.ai.PromptTemplateService;
import org.example.util.LanguageUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象代码生成策略 — 模板方法模式。
 * <p>
 * 定义了代码生成的通用流程（validate → selectSystemPrompt → buildMessages →
 * executeStreaming → parseResponse → persistFiles → saveConversation）。
 * 子类实现各钩子方法以适配不同生成模式。
 */
@Slf4j
public abstract class AbstractCodeGenerationStrategy implements CodeGenerationStrategy {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    // ─── 共享依赖（由子类通过构造函数传入或从 Executor 注入） ───

    protected DynamicModelProvider modelProvider;
    protected PromptTemplateService promptService;
    protected ConversationRepository conversationRepo;
    protected MessageRepository messageRepo;
    protected ProjectFileMapper projectFileMapper;
    protected MonitorService monitorService;
    protected org.example.mapper.ModelConfigMapper modelConfigMapper;
    protected org.example.service.DeployService deployService;

    /** 由外部注入共享依赖（Executor 负责调用） */
    public void setDependencies(DynamicModelProvider modelProvider, PromptTemplateService promptService,
                                ConversationRepository conversationRepo, MessageRepository messageRepo,
                                ProjectFileMapper projectFileMapper, MonitorService monitorService,
                                org.example.mapper.ModelConfigMapper modelConfigMapper,
                                org.example.service.DeployService deployService) {
        this.modelProvider = modelProvider;
        this.promptService = promptService;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.projectFileMapper = projectFileMapper;
        this.monitorService = monitorService;
        this.modelConfigMapper = modelConfigMapper;
        this.deployService = deployService;
    }

    // ─── 模板方法 ───

    @Override
    public SseEmitter generate(GenerateCodeRequest request, Long userId) {
        validateRequest(request);
        String systemPrompt = selectSystemPrompt(request);
        List<ChatMessage> messages = buildMessages(systemPrompt, request);
        SseEmitter emitter = doStream(messages, request, userId);
        return emitter;
    }

    // ─── 钩子方法（子类必须/可选实现） ───

    /** 校验请求参数（可选覆盖） */
    protected void validateRequest(GenerateCodeRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
    }

    /** 选择 System Prompt（子类必须实现） */
    protected abstract String selectSystemPrompt(GenerateCodeRequest request);

    /** 解析 AI 响应（子类必须实现） */
    protected abstract Map<String, Object> parseResponse(String raw, Long conversationId,
                                                         GenerateCodeRequest request);

    /** 持久化生成的文件 — DB + 磁盘双写（子类可覆盖） */
    @SuppressWarnings("unchecked")
    protected void persistFiles(Long conversationId, Map<String, Object> parsed,
                                GenerateCodeRequest request) {
        List<Map<String, String>> files = (List<Map<String, String>>) parsed.get("files");
        if (files == null || files.isEmpty()) return;
        // DB 写入
        saveProjectFiles(conversationId, files);
        // 磁盘写入
        if (deployService != null) {
            deployService.writeFilesToDisk(conversationId, null,
                    supportedMode().name().toLowerCase(), files);
        }
    }

    /** 构建 SSE done 事件的额外数据（子类可选覆盖）。
     *  <p>注意：code/text/files 等核心字段由 parseResponse() 提供，
     *  此处仅放 conversationId 和辅助 segments。</p> */
    protected Map<String, Object> buildDoneExtra(String raw, Long conversationId,
                                                 GenerateCodeRequest request) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("conversationId", conversationId);
        extra.put("segments", extractSegments(raw));
        return extra;
    }

    // ─── 公共方法 ───

    /** 构建消息列表（含多轮历史） */
    public List<ChatMessage> buildMessages(String systemPrompt, GenerateCodeRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        List<ChatMessage> history = loadHistory(request.getConversationId());
        if (!history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(UserMessage.from(request.getPrompt()));
        return messages;
    }

    /** 加载对话历史（最近 20 条） */
    protected List<ChatMessage> loadHistory(Long conversationId) {
        if (conversationId == null) return List.of();
        List<Message> msgs = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (msgs.size() > 20) msgs = msgs.subList(msgs.size() - 20, msgs.size());
        List<ChatMessage> his = new ArrayList<>();
        for (Message m : msgs) {
            if ("USER".equals(m.getRole())) his.add(UserMessage.from(m.getContent()));
            else if ("AI".equals(m.getRole())) his.add(AiMessage.from(m.getContent()));
        }
        return his;
    }

    // ─── SSE 流式执行引擎 ───

    protected SseEmitter doStream(List<ChatMessage> messages, GenerateCodeRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        AtomicBoolean completed = new AtomicBoolean(false);
        Thread streamThread = Thread.ofVirtual().start(() -> {
            StringBuilder full = new StringBuilder();
            long start = System.currentTimeMillis();
            AtomicLong firstTokenAt = new AtomicLong(0);
            try {
                StreamingChatLanguageModel model = modelProvider.getStreaming(request.getModelId());
                model.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        if (completed.get()) return;
                        if (firstTokenAt.get() == 0) firstTokenAt.set(System.currentTimeMillis());
                        full.append(token);
                        // SSE JSON 包裹: {"d":"<escaped>"}
                        String wrapped = toJsonWrap(token);
                        try { emitter.send(SseEmitter.event().name("token").data(wrapped)); }
                        catch (IOException e) {
                            log.debug("SSE 客户端已断开, 已接收 {} 字符", full.length());
                            completed.set(true);
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse r) {
                        if (completed.get()) return;
                        completed.set(true);
                        try {
                            String raw = full.toString();
                            Long cid = getOrCreateConversation(request, userId);
                            saveMessages(request, userId, cid, raw);

                            Map<String, Object> parsed = parseResponse(raw, cid, request);
                            persistFiles(cid, parsed, request);

                            Map<String, Object> done = buildDoneExtra(raw, cid, request);
                            done.putAll(parsed);

                            // done 事件也用 JSON 包裹
                            emitter.send(SseEmitter.event().name("done")
                                    .data(toJsonWrap(MAPPER.writeValueAsString(done))));
                            emitter.complete();
                            log.info("SSE 流式完成, mode={}, 长度={}", supportedMode(), raw.length());
                            logCall(raw, request.getModelId(), start, firstTokenAt, true, null);
                        } catch (IOException e) {
                            log.debug("SSE done 发送失败, 客户端已断开");
                        } catch (Exception e) {
                            log.error("SSE done 处理异常: {}", e.getMessage());
                            logCall(full.toString(), request.getModelId(), start, firstTokenAt, false, e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (completed.get()) return;
                        completed.set(true);
                        log.error("SSE 流式异常: {}", e.getMessage());
                        safeSendError(emitter, toJsonWrap("生成失败: " + e.getMessage()));
                        logCall(full.toString(), request.getModelId(), start, firstTokenAt, false, e.getMessage());
                        try { emitter.completeWithError(e); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                if (completed.get()) return;
                completed.set(true);
                log.error("SSE 启动失败: {}", e.getMessage());
                safeSendError(emitter, toJsonWrap("服务内部错误: " + e.getMessage()));
                logCall("", request.getModelId(), start, firstTokenAt, false, e.getMessage());
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });
        emitter.onTimeout(() -> {
            completed.set(true);
            streamThread.interrupt(); // 服务端取消：超时时中断 AI 请求线程
            log.warn("SSE 连接超时，已中断生成线程");
        });
        emitter.onError(e -> {
            completed.set(true);
            streamThread.interrupt();
            log.warn("SSE 连接异常，已中断生成线程");
        });
        emitter.onCompletion(() -> log.debug("SSE 连接关闭"));
        return emitter;
    }

    /** 将字符串包裹为 JSON {"d":"..."} 格式，转义双引号和反斜杠 */
    private static String toJsonWrap(String raw) {
        String escaped = raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"d\":\"" + escaped + "\"}";
    }

    // ─── 对话持久化 ───

    protected Long getOrCreateConversation(GenerateCodeRequest request, Long userId) {
        if (request.getConversationId() != null) return request.getConversationId();
        Conversation c = conversationRepo.save(Conversation.builder()
                .userId(userId)
                .title(truncate(request.getPrompt(), 50))
                .model("default")
                .status(1)
                .type(supportedMode().name())
                .build());
        request.setConversationId(c.getId());
        return c.getId();
    }

    protected void saveMessages(GenerateCodeRequest request, Long userId, Long conversationId, String aiResponse) {
        String userPrompt = request.getOriginalPrompt() != null && !request.getOriginalPrompt().isBlank()
                ? request.getOriginalPrompt() : request.getPrompt();
        messageRepo.save(Message.builder().conversationId(conversationId).role("USER").content(userPrompt).build());
        messageRepo.save(Message.builder().conversationId(conversationId).role("AI").content(aiResponse).build());
    }

    // ─── 文件持久化（多文件/项目模式使用） ───

    /** 将解析出的文件保存到 project_files 表 */
    protected void saveProjectFiles(Long conversationId, List<Map<String, String>> files) {
        if (files == null || files.isEmpty()) return;
        for (Map<String, String> f : files) {
            String content = f.get("content");
            projectFileMapper.insert(ProjectFile.builder()
                    .conversationId(conversationId)
                    .filePath(f.get("path"))
                    .content(content)
                    .fileSize(content != null ? (long) content.length() : 0L)
                    .build());
        }
    }

    // ─── 代码提取工具方法 ───

    protected String extractCode(String raw) {
        java.util.regex.Matcher m = CODE_BLOCK_RE.matcher(raw);
        StringBuilder result = new StringBuilder();
        boolean first = true;
        while (m.find()) {
            String code = m.group(2).trim();
            if (code.isEmpty()) continue;
            if (!first) result.append("\n\n");
            result.append(code);
            first = false;
        }
        return !result.isEmpty() ? result.toString().trim() : raw.trim();
    }

    protected String extractText(String raw) {
        return raw.replaceAll("```[\\s\\S]*?```", "").trim();
    }

    protected List<Map<String, String>> extractSegments(String raw) {
        List<Map<String, String>> segments = new ArrayList<>();
        java.util.regex.Matcher m = CODE_BLOCK_RE.matcher(raw);
        int lastEnd = 0;
        while (m.find()) {
            String textBefore = raw.substring(lastEnd, m.start()).trim();
            if (!textBefore.isEmpty()) {
                segments.add(Map.of("type", "text", "content", textBefore));
            }
            String lang = m.group(1).isEmpty() ? "text" : m.group(1);
            String code = m.group(2).trim();
            if (!code.isEmpty()) {
                segments.add(Map.of("type", "code", "language", lang, "content", code));
            }
            lastEnd = m.end();
        }
        String textAfter = raw.substring(lastEnd).trim();
        if (!textAfter.isEmpty()) {
            segments.add(Map.of("type", "text", "content", textAfter));
        }
        if (segments.isEmpty() && !raw.isBlank()) {
            segments.add(Map.of("type", "text", "content", raw.trim()));
        }
        return segments;
    }

    // ─── 日志 ───

    protected void logCall(String raw, Long modelId, long start, AtomicLong firstTokenAt,
                           boolean success, String error) {
        try {
            String model = "default";
            if (modelId != null) {
                var mc = modelConfigMapper.selectById(modelId);
                if (mc != null) model = mc.getModelName();
            } else {
                var def = modelConfigMapper.findDefault().orElse(null);
                if (def != null) model = def.getModelName();
            }
            int tokens = raw.isEmpty() ? 0 : raw.length() / 4;
            long latency = firstTokenAt.get() > 0
                    ? firstTokenAt.get() - start
                    : System.currentTimeMillis() - start;
            monitorService.record("/api/codegen/stream", null, model, tokens, latency, success, error);
        } catch (Exception ignored) {}
    }

    // ─── 工具 ───

    protected String truncate(String s, int max) {
        return s == null ? "新对话" : s.length() > max ? s.substring(0, max) + "..." : s;
    }

    protected void safeSendError(SseEmitter emitter, String msg) {
        try { emitter.send(SseEmitter.event().name("error").data(msg)); }
        catch (IOException ignored) {}
    }

    private static final java.util.regex.Pattern CODE_BLOCK_RE =
            java.util.regex.Pattern.compile("```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```");
}
