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
import org.example.util.LanguageUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCodeGenService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DynamicModelProvider modelProvider;
    private final PromptTemplateService promptService;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final org.example.service.ProjectService projectService;
    private final org.example.service.MonitorService monitorService;
    private final org.example.mapper.ModelConfigMapper modelConfigMapper;

    /** SSE 流式代码生成 */
    public SseEmitter generateStream(GenerateCodeRequest request, Long userId) {
        boolean isEng = "ENGINEERING".equalsIgnoreCase(request.getType());
        List<ChatMessage> messages = promptService.buildMessages(
                getSystemPrompt(request), request.getPrompt(), loadHistory(request.getConversationId()), isEng);
        log.info("开始 SSE 流式生成, prompt={}, modelId={}", truncate(request.getPrompt(), 50), request.getModelId());
        return doStream(messages, request.getModelId(), raw -> {
            Long cid = getOrCreateConversation(request, userId);
            String userMsg = request.getOriginalPrompt() != null && !request.getOriginalPrompt().isBlank()
                    ? request.getOriginalPrompt() : request.getPrompt();
            saveMessages(request, userId, userMsg, raw);
            Map<String, Object> m = new HashMap<>();
            m.put("conversationId", cid); m.put("code", extractCode(raw));
            m.put("text", extractText(raw));
            m.put("segments", extractSegments(raw));
            m.put("language", request.getLanguage() != null ? request.getLanguage() : LanguageUtil.detectByContent(raw));
            return m;
        });
    }

    public SseEmitter modifyCodeStream(CodeModifyRequest request, Long userId) {
        String code = request.getCurrentCode();
        boolean isMultiFile = code != null && code.contains("// =====");
        String sp = isMultiFile
                ? promptService.getProjectModifySystemPrompt(code)
                : promptService.getCodeModifySystemPrompt(code);
        String up = "修改元素: " + request.getElementInfo() + "\n修改要求: " + request.getModifyPrompt();
        return doStream(promptService.buildMessages(sp, up, null, false), null, raw -> {
            // 持久化修改对话到数据库
            Long cid = request.getConversationId();
            if (cid != null) {
                try {
                    messageRepo.save(Message.builder()
                        .conversationId(cid).role("USER")
                        .content(up).build());
                    messageRepo.save(Message.builder()
                        .conversationId(cid).role("AI")
                        .content(raw).build());
                    log.info("修改对话已持久化, conversationId={}, 长度={}", cid, raw.length());
                } catch (Exception ex) {
                    log.warn("保存修改消息失败: {}", ex.getMessage());
                }
            }
            return Map.of("conversationId", cid != null ? cid : 0);
        });
    }

    /** 工程项目流式生成（解析后在磁盘创建真实文件） */
    public SseEmitter generateProject(GenerateCodeRequest request, Long userId) {
        String sp = promptService.getProjectSystemPrompt();
        List<ChatMessage> messages = promptService.buildMessages(sp, request.getPrompt(),
                loadHistory(request.getConversationId()), true);
        log.info("开始工程项目流式生成, prompt={}", truncate(request.getPrompt(), 50));
        return doStream(messages, request.getModelId(), raw -> {
            Long cid = getOrCreateConversation(request, userId);
            String userMsg = request.getOriginalPrompt() != null && !request.getOriginalPrompt().isBlank()
                    ? request.getOriginalPrompt() : request.getPrompt();
            saveMessages(request, userId, userMsg, raw);
            Map<String, Object> parsed = parseProjectResponse(raw, cid, request.getPrompt());
            // 在磁盘上创建真实项目文件（放入以 [NAME] 命名的父文件夹）
            @SuppressWarnings("unchecked")
            List<Map<String, String>> files = (List<Map<String, String>>) parsed.get("files");
            if (files != null && !files.isEmpty()) {
                String folderName = (String) parsed.getOrDefault("folderName", "app");
                projectService.createProjectFiles(cid, folderName, files);
            }
            return parsed;
        });
    }

    /** 解析 [PROJECT] + [FILE] 极简标记格式，多层兜底 */
    private Map<String, Object> parseProjectResponse(String raw, Long conversationId, String userPrompt) {
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

        // 提取 [NAME] 项目文件夹名（英文小写连字符格式）
        java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile(
            "(?i)\\[NAME\\]\\s*([a-z][a-z0-9-]{1,40})").matcher(raw);
        String folderName = nameMatcher.find() ? nameMatcher.group(1).trim() : "";
        // 兜底：从用户提示词中提取英文关键词生成文件夹名
        if (folderName.isEmpty()) {
            folderName = generateFolderName(userPrompt, projectType);
        }
        result.put("folderName", folderName);

        // 核心：按 [FILE] path\n```lang\ncode\n``` 提取（稳定版，已验证可行）
        List<Map<String, String>> files = new ArrayList<>();
        java.util.regex.Matcher fileMatcher = java.util.regex.Pattern.compile(
            "(?i)\\[FILE\\]\\s*([^\\r\\n]+)\\s*[\\r\\n]+```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```"
        ).matcher(raw);
        StringBuilder allCode = new StringBuilder();
        while (fileMatcher.find()) {
            String path = fileMatcher.group(1).trim();
            String lang = fileMatcher.group(2).isEmpty() ? "text" : fileMatcher.group(2);
            String code = fileMatcher.group(3).trim();
            if (code.isEmpty()) {
                log.warn("主正则匹配到空代码块: path={}", path);
                continue;
            }
            Map<String, String> file = new HashMap<>();
            file.put("path", path); file.put("language", lang); file.put("content", code);
            files.add(file);
            if (!allCode.isEmpty()) allCode.append("\n\n");
            allCode.append("// ===== ").append(path).append(" =====\n").append(code);
        }
        log.info("主正则提取 {} 个文件, raw长度={}", files.size(), raw.length());

        // 兜底0：有 [FILE] 标记 — 按 [FILE] 切分补充 regex 遗漏的文件（始终运行）
        if (raw.contains("[FILE]")) {
            // 收集已捕获的文件路径（用于去重）
            java.util.Set<String> captured = new java.util.HashSet<>();
            for (Map<String, String> f : files) captured.add(f.get("path"));

            String[] sections = raw.split("(?i)\\[FILE\\]\\s*");
            for (String section : sections) {
                section = section.trim();
                if (section.isEmpty()) continue;
                int nl = section.indexOf('\n');
                String pathLine = nl > 0 ? section.substring(0, nl) : section;
                String path = pathLine.trim();

                // 跳过非文件路径的段（如 [PROJECT] / [DESC] 开头的前导段）
                if (path.contains("[") || path.isEmpty()) continue;

                // 检测并移除路径行末尾的 ``` 内联标记（LLM 可能把 [FILE] 和 ``` 写在同一行）
                boolean hadInlineFence = false;
                if (path.matches(".*```\\w*\\s*$")) {
                    hadInlineFence = true;
                    path = path.replaceFirst("\\s*```\\w*\\s*$", "").trim();
                }
                // 跳过已捕获的文件（去重）
                if (captured.contains(path)) continue;

                String rest = nl > 0 ? section.substring(nl + 1).trim() : "";
                String lang = "text";
                String code = "";

                // 尝试提取 ``` 代码块
                java.util.regex.Matcher codeMatcher = java.util.regex.Pattern.compile(
                    "```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```"
                ).matcher(rest);
                if (codeMatcher.find()) {
                    lang = codeMatcher.group(1).isEmpty() ? "text" : codeMatcher.group(1);
                    code = codeMatcher.group(2).trim();
                } else if (hadInlineFence) {
                    // 开 fence 在 [FILE] 行 → rest 的第一行是代码，末尾可能有 ```
                    code = rest.replaceAll("\\s*```\\s*$", "").trim();
                    if (!code.isEmpty()) lang = LanguageUtil.detectByContent(code);
                } else {
                    // 没有代码块 → 整个 rest 作为代码，去掉首尾可能的残留标记
                    code = rest.replaceAll("^```\\w*\\s*", "").replaceAll("\\s*```$", "").trim();
                    if (!code.isEmpty()) lang = LanguageUtil.detectByContent(code);
                }

                if (code.isEmpty()) continue; // 跳过空文件
                Map<String, String> file = new HashMap<>();
                file.put("path", path); file.put("language", lang); file.put("content", code);
                files.add(file);
                if (!allCode.isEmpty()) allCode.append("\n\n");
                allCode.append("// ===== ").append(path).append(" =====\n").append(code);
                log.warn("兜底补充遗漏文件: {}", path);
            }
            log.info("兜底0补充: raw中[FILE]段={}个, 主正则已捕获={}个",
                sections.length, captured.size());
        }

        // 路径前缀自动修正：LLM 可能漏掉文件夹前缀（如 vue3-shop/）→ 用多数文件的前缀补全
        if (files.size() >= 3) {
            // 统计每个文件的第一级目录前缀
            Map<String, Integer> prefixCount = new HashMap<>();
            for (Map<String, String> f : files) {
                String p = f.get("path");
                int slash = p.indexOf('/');
                if (slash > 0) {
                    String prefix = p.substring(0, slash + 1); // e.g. "vue3-shop/"
                    prefixCount.merge(prefix, 1, Integer::sum);
                }
            }
            // 找到超过 2/3 文件的优势前缀
            int threshold = files.size() * 2 / 3;
            String dominant = null;
            for (var e : prefixCount.entrySet()) {
                if (e.getValue() > threshold) {
                    dominant = e.getKey();
                    break;
                }
            }
            // 给不在该前缀下的文件补上（直接在前面加文件夹前缀）
            if (dominant != null) {
                for (Map<String, String> f : files) {
                    String p = f.get("path");
                    if (!p.startsWith(dominant)) {
                        String newPath = dominant + p;
                        f.put("path", newPath);
                        log.warn("路径前缀自动修正: {} → {}", p, newPath);
                    }
                }
                // 重建 allCode（路径变了）
                allCode = new StringBuilder();
                for (Map<String, String> f : files) {
                    if (!allCode.isEmpty()) allCode.append("\n\n");
                    allCode.append("// ===== ").append(f.get("path")).append(" =====\n").append(f.get("content"));
                }
            }
        }

        // 兜底1：按 ``` 块提取（兼容未关闭的代码块 — LLM 超 token 截断）
        if (files.isEmpty()) {
            java.util.regex.Matcher blockMatcher = java.util.regex.Pattern.compile(
                "```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```").matcher(raw);
            int idx = 0;
            while (blockMatcher.find()) {
                String code = blockMatcher.group(2).trim();
                if (code.isEmpty()) continue;
                String lang = blockMatcher.group(1).isEmpty() ? "text" : blockMatcher.group(1);
                // 优先用 // File: 标记作为文件名，否则智能生成
                String path = extractFileName(code);
                code = stripFileMarker(code);
                if (path.isEmpty()) path = buildFilePath(lang, ++idx, code);
                files.add(Map.of("path", path, "language", lang, "content", code));
                if (!allCode.isEmpty()) allCode.append("\n\n");
                allCode.append("// ===== ").append(path).append(" =====\n").append(code);
            }

            // 兜底1b：完整的 ``` 块都没匹配到 → 尝试提取未关闭的 ``` 块（LLM 截断）
            if (files.isEmpty()) {
                java.util.regex.Matcher openBlockMatcher = java.util.regex.Pattern.compile(
                    "```(\\w*)\\s*[\\r\\n]+([\\s\\S]*)$", java.util.regex.Pattern.DOTALL).matcher(raw);
                if (openBlockMatcher.find()) {
                    String lang = openBlockMatcher.group(1).isEmpty() ? "text" : openBlockMatcher.group(1);
                    String code = openBlockMatcher.group(2).trim();
                    if (!code.isEmpty()) {
                        String path = extractFileName(code);
                        code = stripFileMarker(code);
                        if (path.isEmpty()) path = buildFilePath(lang, 1, code);
                        files.add(Map.of("path", path, "language", lang, "content", code));
                        allCode.append("// ===== ").append(path).append(" =====\n").append(code);
                        log.warn("兜底1b: 提取未关闭的 {} 代码块, 路径={}, 长度={}", lang, path, code.length());
                    }
                }
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

    private SseEmitter doStream(List<ChatMessage> messages, Long modelId,
                                 java.util.function.Function<String, Map<String, Object>> extra) {
        SseEmitter emitter = new SseEmitter(300_000L);
        java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread.ofVirtual().start(() -> {
            StringBuilder full = new StringBuilder();
            long start = System.currentTimeMillis();
            java.util.concurrent.atomic.AtomicLong firstTokenAt = new java.util.concurrent.atomic.AtomicLong(0);
            try {
                StreamingChatLanguageModel model = modelProvider.getStreaming(modelId);
                model.chat(messages, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String token) {
                        if (completed.get()) return;
                        if (firstTokenAt.get() == 0) firstTokenAt.set(System.currentTimeMillis());
                        full.append(token);
                        try { emitter.send(SseEmitter.event().name("token").data(token)); }
                        catch (IOException e) {
                            // 客户端断开连接（正常行为），不再尝试发送
                            log.debug("SSE 客户端已断开, 已接收 {} 字符", full.length());
                            completed.set(true);
                            throw new RuntimeException("client-disconnected", e);
                        }
                    }
                    @Override public void onCompleteResponse(ChatResponse r) {
                        if (completed.get()) return;
                        completed.set(true);
                        try {
                            String raw = full.toString();
                            Map<String, Object> done = new HashMap<>();
                            done.put("code", extractCode(raw));
                            done.put("text", extractText(raw));
                            done.putAll(extra.apply(raw));
                            emitter.send(SseEmitter.event().name("done").data(MAPPER.writeValueAsString(done)));
                            emitter.complete();
                            log.info("SSE 流式完成, 长度={}", raw.length());
                            logCall(full.toString(), modelId, firstTokenAt.get() > 0 ? firstTokenAt.get() - start : System.currentTimeMillis() - start, true, null);
                        } catch (IOException e) {
                            log.debug("SSE done 发送失败, 客户端已断开");
                        } catch (Exception e) {
                            log.error("SSE done 处理异常: {}", e.getMessage());
                            logCall(full.toString(), modelId, firstTokenAt.get() > 0 ? firstTokenAt.get() - start : 0, false, e.getMessage());
                        }
                    }
                    @Override public void onError(Throwable e) {
                        if (completed.get() || "client-disconnected".equals(e.getMessage())) return;
                        completed.set(true);
                        log.error("SSE 流式异常: {}", e.getMessage());
                        safeSendError(emitter, "生成失败: " + e.getMessage());
                        logCall(full.toString(), modelId, 0, false, e.getMessage());
                        try { emitter.completeWithError(e); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                if (completed.get() || "client-disconnected".equals(e.getMessage())) return;
                completed.set(true);
                log.error("SSE 启动失败: {}", e.getMessage());
                safeSendError(emitter, "服务内部错误: " + e.getMessage());
                logCall("", modelId, 0, false, e.getMessage());
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });
        emitter.onTimeout(() -> {
            completed.set(true);
            log.warn("SSE 连接超时");
        });
        emitter.onCompletion(() -> log.debug("SSE 连接关闭"));
        return emitter;
    }

    private void safeSendError(SseEmitter emitter, String msg) {
        try { emitter.send(SseEmitter.event().name("error").data(msg)); } catch (IOException ignored) {}
    }

    private String getSystemPrompt(GenerateCodeRequest r) {
        return "ENGINEERING".equalsIgnoreCase(r.getType())
                ? promptService.getProjectSystemPrompt()
                : promptService.getSingleFileSystemPrompt();
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
        String type = "ENGINEERING".equalsIgnoreCase(r.getType()) ? "ENGINEERING" : "NATIVE";
        Conversation c = conversationRepo.save(
                Conversation.builder().userId(userId).title(truncate(r.getPrompt(), 50)).model("gpt-4o").status(1).type(type).build());
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

    /** 记录 API 调用到监控日志 */
    private void logCall(String raw, Long modelId, long ttfbMs, boolean success, String error) {
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
            monitorService.record("/api/ai/generate/stream", null, model, tokens,
                    ttfbMs > 0 ? ttfbMs : 0, success, error);
        } catch (Exception ignored) { /* monitoring shouldn't break main flow */ }
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
    private String truncate(String s, int max) {
        return s == null ? "新对话" : s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /** 根据语言和代码内容生成智能文件名 */
    private String buildFilePath(String lang, int idx, String code) {
        String ext = LanguageUtil.toExt(lang);
        // 单文件 HTML 项目 → index.html
        if ("html".equalsIgnoreCase(lang) && idx == 1) {
            return "index" + ext;
        }
        // 单文件 Vue 项目 → App.vue
        if ("vue".equalsIgnoreCase(lang) && idx == 1) {
            return "App" + ext;
        }
        return "file" + idx + ext;
    }

    /** 从代码首行剥离 // File: 标记 */
    private String stripFileMarker(String code) {
        String firstLine = code.lines().findFirst().orElse("");
        if (firstLine.matches("^\\s*//\\s*File:\\s*.+")) {
            return code.replaceFirst("^\\s*//\\s*File:[^\n]*\n?", "").trim();
        }
        return code;
    }

    /** 从用户提示词生成文件夹名（兜底，当 AI 未返回 [NAME] 时使用） */
    private String generateFolderName(String prompt, String projectType) {
        if (prompt == null || prompt.isBlank()) return "app";
        // 提取英文单词（连续字母），过滤过短的词
        String[] words = prompt.split("[^a-zA-Z]+");
        List<String> meaningful = new java.util.ArrayList<>();
        for (String w : words) {
            if (w.length() >= 2 && !isStopWord(w)) {
                meaningful.add(w.toLowerCase());
            }
        }
        if (!meaningful.isEmpty()) {
            // 去重，最多取 4 个词
            String name = String.join("-", meaningful.stream().distinct().limit(4).toList());
            if (name.length() > 50) name = name.substring(0, 50);
            // 移除尾部连字符
            name = name.replaceAll("-$", "");
            return name;
        }
        // 完全没有英文单词 → 按项目类型给默认名
        return "backend".equals(projectType) ? "backend-app" : "frontend-app";
    }

    private boolean isStopWord(String w) {
        return java.util.Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "shall", "to", "of", "in", "for",
            "on", "with", "at", "by", "from", "or", "and", "not", "no", "but",
            "if", "then", "else", "when", "up", "so", "as", "it", "its", "just",
            "that", "this", "these", "those", "all", "each", "every", "both"
        ).contains(w.toLowerCase());
    }
}
