/**
 * 模块：AI代码生成
 * 功能：AI代码生成核心服务，SSE流式输出
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.service.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.CodeModifyRequest;
import org.example.dto.request.GenerateCodeRequest;
import org.example.entity.Conversation;
import org.example.entity.Message;
import org.example.repository.ConversationRepository;
import org.example.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCodeGenService {

    private final StreamingChatLanguageModel streamingModel;
    private final PromptTemplateService promptService;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;

    /**
     * 流式生成（SSE 逐Token推送）
     */
    public SseEmitter generateStream(GenerateCodeRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        // 构建消息
        String systemPrompt = getSystemPrompt(request);
        List<ChatMessage> history = loadHistory(request.getConversationId());
        List<ChatMessage> messages = promptService.buildMessages(
                systemPrompt, request.getPrompt(), history);

        log.info("开始 SSE 流式生成, prompt={}, type={}",
                request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())),
                request.getType());

        // 使用虚拟线程执行流式调用
        Thread.ofVirtual().start(() -> {
            StringBuilder fullCode = new StringBuilder();

            try {
                streamingModel.chat(messages, new StreamingChatResponseHandler() {

                    @Override
                    public void onPartialResponse(String token) {
                        try {
                            fullCode.append(token);
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (IOException e) {
                            log.error("SSE token 推送失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        try {
                            log.info("流式生成完成, 总长度={}", fullCode.length());
                            emitter.send(SseEmitter.event().name("done").data(fullCode.toString()));
                            emitter.complete();
                            saveMessages(request, userId, request.getPrompt(), fullCode.toString());
                        } catch (IOException e) {
                            log.error("SSE done 发送失败", e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("AI 流式生成异常: {}", error.getMessage(), error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("生成失败: " + error.getMessage()));
                        } catch (IOException ignored) { }
                        emitter.completeWithError(error);
                    }
                });
            } catch (Exception e) {
                log.error("启动流式生成失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("服务内部错误: " + e.getMessage()));
                } catch (IOException ignored) { }
                emitter.completeWithError(e);
            }
        });

        // 超时或客户端断开时回调
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onError(Throwable::printStackTrace);
        emitter.onCompletion(() -> log.debug("SSE 连接关闭"));

        return emitter;
    }

    /**
     * 流式修改代码——基于现有代码 + 选中元素 + 修改需求，SSE 输出
     */
    public SseEmitter modifyCodeStream(CodeModifyRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(300_000L);

        String systemPrompt = promptService.getCodeModifySystemPrompt(request.getCurrentCode());
        String userPrompt = "修改元素: " + request.getElementInfo() + "\n修改要求: " + request.getModifyPrompt();

        List<ChatMessage> messages = promptService.buildMessages(systemPrompt, userPrompt, null);

        log.info("开始 SSE 流式修改, element={}, prompt={}",
                truncate(request.getElementInfo(), 30),
                truncate(request.getModifyPrompt(), 30));

        Thread.ofVirtual().start(() -> {
            StringBuilder fullCode = new StringBuilder();
            try {
                streamingModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String token) {
                        try { fullCode.append(token); emitter.send(SseEmitter.event().name("token").data(token)); }
                        catch (IOException e) { log.error("SSE token 推送失败: {}", e.getMessage()); }
                    }
                    @Override public void onCompleteResponse(ChatResponse completeResponse) {
                        try {
                            emitter.send(SseEmitter.event().name("done").data(fullCode.toString()));
                            emitter.complete();
                        } catch (IOException e) { log.error("SSE done 发送失败", e); }
                    }
                    @Override public void onError(Throwable error) {
                        log.error("AI 流式修改异常: {}", error.getMessage());
                        try { emitter.send(SseEmitter.event().name("error").data("修改失败: " + error.getMessage())); }
                        catch (IOException ignored) {}
                        emitter.completeWithError(error);
                    }
                });
            } catch (Exception e) {
                log.error("启动流式修改失败: {}", e.getMessage(), e);
                try { emitter.send(SseEmitter.event().name("error").data("服务内部错误: " + e.getMessage())); }
                catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> log.warn("SSE 修改连接超时"));
        emitter.onCompletion(() -> log.debug("SSE 修改连接关闭"));
        return emitter;
    }

    /** 根据类型选择 System Prompt */
    private String getSystemPrompt(GenerateCodeRequest request) {
        if ("ENGINEERING".equalsIgnoreCase(request.getType())) {
            return promptService.getEngineeringProjectSystemPrompt();
        }
        return promptService.getNativeAppSystemPrompt(request.getLanguage());
    }

    /** 加载历史对话消息（最近10轮） */
    private List<ChatMessage> loadHistory(Long conversationId) {
        if (conversationId == null) return List.of();

        List<Message> recent = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (recent.size() > 20) {
            recent = recent.subList(recent.size() - 20, recent.size());
        }

        List<ChatMessage> history = new ArrayList<>();
        for (Message msg : recent) {
            if ("USER".equals(msg.getRole())) {
                history.add(dev.langchain4j.data.message.UserMessage.from(msg.getContent()));
            } else if ("AI".equals(msg.getRole())) {
                history.add(dev.langchain4j.data.message.AiMessage.from(msg.getContent()));
            }
        }
        return history;
    }

    /** 持久化用户消息 + AI回复 */
    private void saveMessages(GenerateCodeRequest request, Long userId,
                               String userPrompt, String aiResponse) {
        Long conversationId = getOrCreateConversation(request, userId);

        // 用户消息
        Message userMsg = Message.builder()
                .conversationId(conversationId)
                .role("USER")
                .content(userPrompt)
                .build();
        messageRepo.save(userMsg);

        // AI 消息
        Message aiMsg = Message.builder()
                .conversationId(conversationId)
                .role("AI")
                .content(aiResponse)
                .build();
        messageRepo.save(aiMsg);
    }

    private Long getOrCreateConversation(GenerateCodeRequest request, Long userId) {
        if (request.getConversationId() != null) {
            return request.getConversationId();
        }

        Conversation conv = Conversation.builder()
                .userId(userId)
                .title(truncate(request.getPrompt(), 50))
                .model("gpt-4o")
                .status(1)
                .build();
        conv = conversationRepo.save(conv);
        request.setConversationId(conv.getId());
        return conv.getId();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "新对话";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
