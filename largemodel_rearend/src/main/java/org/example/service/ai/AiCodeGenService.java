/**
 * 模块：AI代码生成
 * 功能：SSE流式代码生成/修改服务
 * 作者：yx
 * 创建时间：2026-06-17
 */
package org.example.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCodeGenService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final StreamingChatLanguageModel streamingModel;
    private final PromptTemplateService promptService;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final org.example.service.ProjectService projectService;

    /** SSE 流式代码生成 */
    public SseEmitter generateStream(GenerateCodeRequest request, Long userId) {
        boolean isEng = "ENGINEERING".equalsIgnoreCase(request.getType());
        List<ChatMessage> messages = promptService.buildMessages(
                getSystemPrompt(request), request.getPrompt(), loadHistory(request.getConversationId()), isEng);
        log.info("开始 SSE 流式生成, prompt={}", truncate(request.getPrompt(), 50));
        return doStream(messages, raw -> {
            Long cid = getOrCreateConversation(request, userId);
            String userMsg = request.getOriginalPrompt() != null && !request.getOriginalPrompt().isBlank()
                    ? request.getOriginalPrompt() : request.getPrompt();
            saveMessages(request, userId, userMsg, raw);
            Map<String, Object> m = new HashMap<>();
            m.put("conversationId", cid); m.put("code", extractCode(raw));
            m.put("text", extractText(raw));
            m.put("segments", extractSegments(raw));
            m.put("language", request.getLanguage() != null ? request.getLanguage() : detectLang(raw));
            return m;
        });
    }

    public SseEmitter modifyCodeStream(CodeModifyRequest request, Long userId) {
        String sp = promptService.getCodeModifySystemPrompt(request.getCurrentCode());
        String up = "修改元素: " + request.getElementInfo() + "\n修改要求: " + request.getModifyPrompt();
        return doStream(promptService.buildMessages(sp, up, null, false), raw -> Map.of());
    }

    /** 工程项目流式生成（解析后在磁盘创建真实文件） */
    public SseEmitter generateProject(GenerateCodeRequest request, Long userId) {
        String sp = promptService.getProjectSystemPrompt();
        List<ChatMessage> messages = promptService.buildMessages(sp, request.getPrompt(),
                loadHistory(request.getConversationId()), true);
        log.info("开始工程项目流式生成, prompt={}", truncate(request.getPrompt(), 50));
        return doStream(messages, raw -> {
            Long cid = getOrCreateConversation(request, userId);
            String userMsg = request.getOriginalPrompt() != null && !request.getOriginalPrompt().isBlank()
                    ? request.getOriginalPrompt() : request.getPrompt();
            saveMessages(request, userId, userMsg, raw);
            Map<String, Object> parsed = parseProjectResponse(raw, cid);
            // 在磁盘上创建真实项目文件
            @SuppressWarnings("unchecked")
            List<Map<String, String>> files = (List<Map<String, String>>) parsed.get("files");
            if (files != null && !files.isEmpty()) {
                projectService.createProjectFiles(cid, files);
            }
            return parsed;
        });
    }

    /** 解析 [PROJECT] + [FILE] 极简标记格式，多层兜底 */
    private Map<String, Object> parseProjectResponse(String raw, Long conversationId) {
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);

        // 提取项目类型（支持 [PROJECT] 和旧 [PROJECT_TYPE]）
        java.util.regex.Matcher ptMatcher = java.util.regex.Pattern.compile(
            "(?i)\\[PROJECT(?:_TYPE)?\\]\\s*(\\w+)").matcher(raw);
        String projectType = ptMatcher.find() ? ptMatcher.group(1).toLowerCase() : "frontend";
        if (projectType.startsWith("back")) projectType = "backend";
        result.put("projectType", projectType);

        // 提取概述
        java.util.regex.Matcher descMatcher = java.util.regex.Pattern.compile(
            "(?i)\\[DESC\\]\\s*([^\\r\\n]+)").matcher(raw);
        String overview = descMatcher.find() ? descMatcher.group(1).trim() : "";
        // 如果没有 [DESC]，用第一行非标记文字
        if (overview.isEmpty()) {
            overview = raw.lines()
                .filter(l -> !l.isBlank() && !l.trim().matches("(?i)\\[PROJECT.*") && !l.trim().matches("(?i)\\[FILE.*") && !l.trim().startsWith("```"))
                .findFirst().orElse("");
            if (overview.length() > 100) overview = overview.substring(0, 100) + "...";
        }
        result.put("text", overview);

        // 核心：按 [FILE] path\n```lang\ncode\n``` 提取
        List<Map<String, String>> files = new ArrayList<>();
        java.util.regex.Matcher fileMatcher = java.util.regex.Pattern.compile(
            "(?i)\\[FILE\\]\\s*([^\\r\\n]+)\\s*[\\r\\n]+```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```"
        ).matcher(raw);
        StringBuilder allCode = new StringBuilder();
        while (fileMatcher.find()) {
            String path = fileMatcher.group(1).trim();
            String lang = fileMatcher.group(2).isEmpty() ? "text" : fileMatcher.group(2);
            String code = fileMatcher.group(3).trim();
            if (code.isEmpty()) continue;
            Map<String, String> file = new HashMap<>();
            file.put("path", path); file.put("language", lang); file.put("content", code);
            files.add(file);
            if (!allCode.isEmpty()) allCode.append("\n\n");
            allCode.append("// ===== ").append(path).append(" =====\n").append(code);
        }

        // 兜底1：没匹配到 → 按标准 ``` 块提取
        if (files.isEmpty()) {
            java.util.regex.Matcher blockMatcher = java.util.regex.Pattern.compile(
                "```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```").matcher(raw);
            int idx = 0;
            while (blockMatcher.find()) {
                String code = blockMatcher.group(2).trim();
                if (code.isEmpty()) continue;
                String lang = blockMatcher.group(1).isEmpty() ? "text" : blockMatcher.group(1);
                String path = (lang.equals("vue") ? "Component" : "file") + (++idx) + getExt(lang);
                // 尝试从代码首行提取 // File: 标记
                String firstLine = code.lines().findFirst().orElse("");
                if (firstLine.matches("^\\s*//\\s*File:\\s*.+")) {
                    path = firstLine.replaceFirst("^\\s*//\\s*File:\\s*", "").trim();
                    code = code.replaceFirst("^\\s*//\\s*File:[^\n]*\n?", "").trim();
                }
                files.add(Map.of("path", path, "language", lang, "content", code));
                if (!allCode.isEmpty()) allCode.append("\n\n");
                allCode.append("// ===== ").append(path).append(" =====\n").append(code);
            }
        }
        // 兜底2：还是空的 → 整个 raw 作为一个文件
        if (files.isEmpty() && !raw.isBlank()) {
            files.add(Map.of("path", "output.txt", "language", "text", "content", raw.trim()));
            allCode.append(raw.trim());
            result.put("projectType", "frontend");
        }

        result.put("files", files);
        result.put("code", allCode.toString());
        result.put("language", projectType.equals("frontend") ? "vue" : "java");
        return result;
    }

    private String getExt(String lang) {
        return switch (lang) {
            case "vue" -> ".vue"; case "java" -> ".java"; case "python" -> ".py";
            case "javascript", "js" -> ".js"; case "html" -> ".html"; case "css" -> ".css";
            case "json" -> ".json"; case "xml" -> ".xml"; case "markdown" -> ".md";
            default -> ".txt";
        };
    }

    private SseEmitter doStream(List<ChatMessage> messages,
                                 java.util.function.Function<String, Map<String, Object>> extra) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Thread.ofVirtual().start(() -> {
            StringBuilder full = new StringBuilder();
            try {
                streamingModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String token) {
                        full.append(token);
                        try { emitter.send(SseEmitter.event().name("token").data(token)); }
                        catch (IOException e) {
                            // 客户端断开连接（正常行为），不再尝试发送
                            log.debug("SSE 客户端已断开, 已接收 {} 字符", full.length());
                            throw new RuntimeException("client-disconnected", e);
                        }
                    }
                    @Override public void onCompleteResponse(ChatResponse r) {
                        try {
                            String raw = full.toString();
                            Map<String, Object> done = new HashMap<>();
                            done.put("code", extractCode(raw));
                            done.put("text", extractText(raw));
                            done.putAll(extra.apply(raw));
                            emitter.send(SseEmitter.event().name("done").data(MAPPER.writeValueAsString(done)));
                            emitter.complete();
                            log.info("SSE 流式完成, 长度={}", raw.length());
                        } catch (IOException e) { log.debug("SSE done 发送失败, 客户端已断开"); }
                    }
                    @Override public void onError(Throwable e) {
                        if ("client-disconnected".equals(e.getMessage())) return; // 已处理
                        log.error("SSE 流式异常: {}", e.getMessage());
                        safeSendError(emitter, "生成失败: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                if ("client-disconnected".equals(e.getMessage())) return;
                log.error("SSE 启动失败: {}", e.getMessage());
                safeSendError(emitter, "服务内部错误: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onCompletion(() -> log.debug("SSE 连接关闭"));
        return emitter;
    }

    private void safeSendError(SseEmitter emitter, String msg) {
        try { emitter.send(SseEmitter.event().name("error").data(msg)); } catch (IOException ignored) {}
    }

    private String getSystemPrompt(GenerateCodeRequest r) {
        return "ENGINEERING".equalsIgnoreCase(r.getType())
                ? promptService.getEngineeringProjectSystemPrompt()
                : promptService.getNativeAppSystemPrompt(r.getLanguage());
    }

    private List<ChatMessage> loadHistory(Long conversationId) {
        if (conversationId == null) return List.of();
        List<Message> msgs = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (msgs.size() > 20) msgs = msgs.subList(msgs.size() - 20, msgs.size());
        List<ChatMessage> his = new ArrayList<>();
        for (Message m : msgs) {
            if ("USER".equals(m.getRole())) his.add(dev.langchain4j.data.message.UserMessage.from(m.getContent()));
            else if ("AI".equals(m.getRole())) his.add(dev.langchain4j.data.message.AiMessage.from(m.getContent()));
        }
        return his;
    }

    private void saveMessages(GenerateCodeRequest r, Long userId, String userPrompt, String aiResponse) {
        Long cid = getOrCreateConversation(r, userId);
        messageRepo.save(Message.builder().conversationId(cid).role("USER").content(userPrompt).build());
        messageRepo.save(Message.builder().conversationId(cid).role("AI").content(aiResponse).build());
    }

    private Long getOrCreateConversation(GenerateCodeRequest r, Long userId) {
        if (r.getConversationId() != null) return r.getConversationId();
        Conversation c = conversationRepo.save(
                Conversation.builder().userId(userId).title(truncate(r.getPrompt(), 50)).model("gpt-4o").status(1).build());
        r.setConversationId(c.getId());
        return c.getId();
    }

    // 代码块正则：匹配 ```语言\n代码内容\n```（兼容有无换行、有无语言标记）
    private static final java.util.regex.Pattern CODE_BLOCK_RE =
        java.util.regex.Pattern.compile("```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```");

    /** 从代码块首行提取 // File: 声明的文件名 */
    private String extractFileName(String code) {
        String firstLine = code.lines().findFirst().orElse("");
        if (firstLine.matches("^\\s*//\\s*File:\\s*.+")) {
            return firstLine.replaceFirst("^\\s*//\\s*File:\\s*", "").trim();
        }
        return "";
    }

    private String extractCode(String raw) {
        java.util.regex.Matcher m = CODE_BLOCK_RE.matcher(raw);
        StringBuilder result = new StringBuilder();
        boolean first = true;
        boolean found = false;
        while (m.find()) {
            String code = m.group(2).trim();
            if (code.isEmpty()) continue;
            // 提取 // File: xxx 作为文件头
            String fileName = extractFileName(code);
            // 移除 // File: 行，避免代码中重复
            if (!fileName.isEmpty()) {
                code = code.replaceFirst("^\\s*//\\s*File:[^\n]*\n?", "").trim();
            }
            if (code.isEmpty()) continue;
            if (!first) result.append("\n\n");
            if (!fileName.isEmpty()) {
                result.append("// ===== ").append(fileName).append(" =====\n");
            }
            result.append(code);
            first = false;
            found = true;
        }
        return found ? result.toString().trim() : raw.trim();
    }

    private String extractText(String raw) {
        // 移除所有代码块，剩余即为文本说明 + 目录树
        String text = raw.replaceAll("```[\\s\\S]*?```", "");
        return text.trim();
    }

    private List<Map<String, String>> extractSegments(String raw) {
        List<Map<String, String>> segments = new ArrayList<>();
        java.util.regex.Matcher m = CODE_BLOCK_RE.matcher(raw);
        int lastEnd = 0;
        while (m.find()) {
            String textBefore = raw.substring(lastEnd, m.start()).trim();
            if (!textBefore.isEmpty()) {
                Map<String, String> seg = new HashMap<>();
                seg.put("type", "text"); seg.put("content", textBefore);
                segments.add(seg);
            }
            String lang = m.group(1).isEmpty() ? "text" : m.group(1);
            String code = m.group(2).trim();
            if (!code.isEmpty()) {
                String fn = extractFileName(code);
                if (!fn.isEmpty()) code = code.replaceFirst("^\\s*//\\s*File:[^\n]*\n?", "").trim();
                if (!code.isEmpty()) {
                    Map<String, String> seg = new HashMap<>();
                    seg.put("type", "code"); seg.put("language", lang);
                    seg.put("filename", fn); seg.put("content", code);
                    segments.add(seg);
                }
            }
            lastEnd = m.end();
        }
        String textAfter = raw.substring(lastEnd).trim();
        if (!textAfter.isEmpty()) {
            Map<String, String> seg = new HashMap<>();
            seg.put("type", "text"); seg.put("content", textAfter);
            segments.add(seg);
        }
        if (segments.isEmpty() && !raw.isBlank()) {
            Map<String, String> seg = new HashMap<>();
            seg.put("type", "text"); seg.put("content", raw.trim());
            segments.add(seg);
        }
        return segments;
    }
    private String detectLang(String code) {
        if (code.matches("(?s).*<template.*|<script.*")) return "vue";
        if (code.matches("(?s).*public\\s+class.*|@RestController.*")) return "java";
        if (code.matches("(?s).*<!DOCTYPE\\s+html.*|<html.*")) return "html";
        if (code.matches("(?s).*def\\s+\\w+\\s*\\(.*")) return "python";
        return "text";
    }

    private String truncate(String s, int max) {
        return s == null ? "新对话" : s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
