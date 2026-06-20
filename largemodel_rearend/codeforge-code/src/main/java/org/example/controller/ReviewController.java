/**
 * 模块：AI代码审查
 * 功能：提供代码审查 SSE 流式接口
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.CodeReviewRequest;
import org.example.service.ai.PromptTemplateService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final StreamingChatLanguageModel streamingModel;
    private final PromptTemplateService promptService;

    /** SSE 流式代码审查 */
    @PostMapping(value = "/review", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter review(@RequestBody CodeReviewRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        String systemPrompt = promptService.getCodeReviewSystemPrompt(request.getDimensions());

        String userPrompt = "请审查以下 %s 代码：\n\n```%s\n%s\n```".formatted(
                request.getLanguage() != null ? request.getLanguage() : "",
                request.getLanguage() != null ? request.getLanguage() : "",
                request.getCode()
        );
        if (request.getContext() != null && !request.getContext().isBlank()) {
            userPrompt = "项目背景：" + request.getContext() + "\n\n" + userPrompt;
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(userPrompt));

        log.info("开始代码审查, codeSize={}, language={}", request.getCode().length(), request.getLanguage());

        StringBuilder full = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                if (!completed.get()) {
                    try {
                        full.append(token);
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (IOException e) { /* client disconnected */ }
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                completed.set(true);
                try {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("review", full.toString());
                    result.put("score", extractScore(full.toString()));
                    emitter.send(SseEmitter.event().name("done").data(MAPPER.writeValueAsString(result)));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("代码审查出错", error);
                completed.set(true);
                try {
                    Map<String, String> err = Map.of("error", error.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(MAPPER.writeValueAsString(err)));
                } catch (IOException ignored) {}
                emitter.completeWithError(error);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> completed.set(true));
        return emitter;
    }

    /** 从审查文本中提取评分 */
    private Integer extractScore(String review) {
        try {
            String[] lines = review.split("\n");
            for (String line : lines) {
                if (line.contains("总体评分") || line.contains("评分")) {
                    String num = line.replaceAll("[^0-9]", "");
                    if (!num.isEmpty() && num.length() <= 3) {
                        return Integer.parseInt(num);
                    }
                }
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
