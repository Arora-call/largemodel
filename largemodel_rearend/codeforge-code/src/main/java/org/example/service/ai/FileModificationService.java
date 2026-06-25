/**
 * 模块：AI 可视化编辑 — 文件级修改
 * 功能：基于选中元素 + 修改要求，AI 精准修改指定文件，而非重新生成
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.service.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.CodeModifyRequest;
import org.example.entity.Message;
import org.example.mapper.ProjectFileMapper;
import org.example.repository.MessageRepository;
import org.example.service.DeployService;
import org.example.service.MonitorService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件修改服务。
 *
 * <p>与"代码生成"（从零创建）不同，本服务专注于在现有文件基础上做精准修改。
 * AI 只需输出被修改的文件，未修改的文件保持不变。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>构建 Prompt：项目文件清单 + 选中元素信息 + 用户修改要求</li>
 *   <li>LLM 返回仅包含修改后文件的 [FILE] 标记格式</li>
 *   <li>解析响应：提取修改后的文件</li>
 *   <li>写入 DB（project_files）+ 磁盘，触发 Vue 重新构建</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileModificationService {

    private final DynamicModelProvider modelProvider;
    private final MessageRepository messageRepo;
    private final ProjectFileMapper projectFileMapper;
    private final MonitorService monitorService;
    private final DeployService deployService;
    private final VueProjectBuilder vueProjectBuilder;

    private static final Pattern FILE_BLOCK_RE = Pattern.compile(
            "(?i)\\[FILE\\]\\s*([^\\r\\n]+)\\s*[\\r\\n]+```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```");

    /**
     * SSE 流式文件修改。
     *
     * @param request 包含当前文件列表、元素信息、修改要求
     * @param userId  操作用户 ID
     * @return SSE 流
     */
    public SseEmitter modifyFiles(CodeModifyRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        AtomicBoolean completed = new AtomicBoolean(false);

        Thread.ofVirtual().start(() -> {
            StringBuilder full = new StringBuilder();
            long start = System.currentTimeMillis();
            AtomicLong firstTokenAt = new AtomicLong(0);

            try {
                // 1. 构建消息
                List<ChatMessage> messages = buildModifyMessages(request);

                // 2. 选模型（默认）
                StreamingChatLanguageModel model = modelProvider.getStreaming(null);

                model.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        if (completed.get()) return;
                        if (firstTokenAt.get() == 0) firstTokenAt.set(System.currentTimeMillis());
                        full.append(token);
                        try {
                            emitter.send(SseEmitter.event().name("token").data(wrap(token)));
                        } catch (IOException e) {
                            completed.set(true);
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse r) {
                        if (completed.get()) return;
                        completed.set(true);
                        String raw = full.toString();

                        try {
                            // 3. 解析 AI 返回的文件（可能是完整文件或代码片段）
                            List<Map<String, String>> parsedFiles = parseModifiedFiles(raw);
                            log.info("文件修改解析完成: convId={}, 解析文件数={}, raw长度={}",
                                    request.getConversationId(), parsedFiles.size(), raw.length());

                            // 4. 片段合并：如果 AI 只返回了片段，合并回原始文件
                            List<Map<String, String>> mergedFiles = mergeSnippetsWithOriginals(
                                    parsedFiles, request.getFiles());

                            // 5. 持久化：更新 DB project_files + 磁盘
                            if (!mergedFiles.isEmpty()) {
                                if (request.getConversationId() != null) {
                                    persistModifiedFiles(request.getConversationId(), mergedFiles, request.getType());
                                } else {
                                    log.warn("conversationId 为 null，仅写入磁盘");
                                    String dirName = (request.getType() != null ? request.getType().toLowerCase() : "single_file") + "_temp";
                                    deployService.writeFilesToDisk(null, null, dirName, mergedFiles);
                                }
                            } else {
                                log.warn("合并后无有效文件，跳过持久化");
                            }

                            // 5. 保存对话记录（存合并后的完整文件，而非原始片段）
                            if (request.getConversationId() != null) {
                                String userMsg = (request.getElementInfo() != null ? "选中元素: " + request.getElementInfo() + "\n" : "")
                                        + "修改要求: " + request.getModifyPrompt();
                                messageRepo.save(Message.builder()
                                        .conversationId(request.getConversationId()).role("USER")
                                        .content(userMsg).createdAt(LocalDateTime.now()).build());
                                // AI 消息存合并后的完整文件（用 [FILE] 标记，前端 parseAiMessage 可正确提取）
                                String aiMsgContent = extractDescription(raw) + "\n\n"
                                        + buildFileMarkersContent(mergedFiles);
                                messageRepo.save(Message.builder()
                                        .conversationId(request.getConversationId()).role("AI")
                                        .content(aiMsgContent).createdAt(LocalDateTime.now()).build());
                            }

                            // 6. 发送完成事件
                            Map<String, Object> done = new HashMap<>();
                            done.put("files", mergedFiles);
                            done.put("code", buildConcatenatedCode(mergedFiles));
                            done.put("text", extractDescription(raw));

                            emitter.send(SseEmitter.event().name("done")
                                    .data(wrap(toJson(done))));
                            emitter.complete();

                            log.info("文件修改完成: convId={}, 修改文件数={}, 总长={}",
                                    request.getConversationId(), mergedFiles.size(), raw.length());

                            // 7. 记录监控
                            long latency = firstTokenAt.get() > 0
                                    ? firstTokenAt.get() - start
                                    : System.currentTimeMillis() - start;
                            int tokens = raw.length() / 4;
                            monitorService.record("/api/codegen/modify/stream", userId,
                                    "default", tokens, latency, true, null);

                        } catch (Exception e) {
                            log.error("文件修改持久化失败", e);
                            safeSendError(emitter, "文件修改失败: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (completed.get()) return;
                        completed.set(true);
                        log.error("文件修改流异常: {}", e.getMessage());
                        safeSendError(emitter, "修改失败: " + e.getMessage());
                        try { emitter.completeWithError(e); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                if (completed.get()) return;
                completed.set(true);
                log.error("文件修改启动失败: {}", e.getMessage());
                safeSendError(emitter, "服务内部错误: " + e.getMessage());
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });

        emitter.onTimeout(() -> completed.set(true));
        return emitter;
    }

    // ─── Prompt 构建 ───

    /** 构建修改专用的 Prompt */
    private List<ChatMessage> buildModifyMessages(CodeModifyRequest request) {
        String systemPrompt = """
                你是一位资深前端开发专家。用户选中了页面上的特定元素，要求你修改它。

                ## ⚠️ 关键规则（必须严格遵守）
                1. 你只能修改用户指定的元素相关代码
                2. **必须输出被修改文件的完整内容，不能只输出片段！**
                   即使用户只让你改一个颜色，你也要输出整个文件的完整代码
                3. 如果只有一个文件被修改，用 [FILE] 标记输出一个完整文件
                4. 未修改的文件绝对不要输出
                5. 先简要说明你改了什么（1句话），再输出文件

                ## 输出格式（严格遵守）
                [FILE] index.html
                ```html
                <!DOCTYPE html>
                <html>
                ...完整的文件内容...
                </html>
                ```
                """;

        StringBuilder userPrompt = new StringBuilder();

        // 当前项目文件清单
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            userPrompt.append("## 当前项目文件\n\n");
            for (Map<String, String> f : request.getFiles()) {
                String path = f.get("path");
                String content = f.get("content");
                if (content == null) continue;
                // 每个文件展示前 2000 字符作为上下文
                String snippet = content.length() > 2000 ? content.substring(0, 2000) + "\n...(已截断)" : content;
                userPrompt.append("### ").append(path).append("\n```\n").append(snippet).append("\n```\n\n");
            }
        } else if (request.getCurrentCode() != null && !request.getCurrentCode().isBlank()) {
            userPrompt.append("## 当前代码\n```\n").append(request.getCurrentCode()).append("\n```\n\n");
        }

        // 选中元素信息
        if (request.getElementInfo() != null && !request.getElementInfo().isBlank()) {
            userPrompt.append("## 用户选中的页面元素\n").append(request.getElementInfo()).append("\n\n");
        }

        // 修改要求
        userPrompt.append("## 修改要求\n").append(request.getModifyPrompt());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(userPrompt.toString()));
        return messages;
    }

    // ─── 响应解析 ───

    /** 从 AI 响应中提取修改后的文件列表 */
    List<Map<String, String>> parseModifiedFiles(String raw) {
        List<Map<String, String>> files = new ArrayList<>();
        Matcher m = FILE_BLOCK_RE.matcher(raw);
        while (m.find()) {
            String path = m.group(1).trim();
            String lang = m.group(2).isEmpty() ? detectLang(path) : m.group(2);
            String code = m.group(3).trim();
            if (code.isEmpty()) continue;
            files.add(Map.of("path", path, "language", lang, "content", code));
        }
        if (files.isEmpty()) {
            // 兜底：通用代码块
            Matcher block = Pattern.compile("```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```").matcher(raw);
            int idx = 0;
            while (block.find()) {
                String code = block.group(2).trim();
                if (code.isEmpty()) continue;
                idx++;
                files.add(Map.of("path", "file" + idx + ".html",
                        "language", block.group(1).isEmpty() ? "html" : block.group(1),
                        "content", code));
            }
        }
        return files;
    }

    /** 提取 AI 的修改说明（第一个非代码块的文字段落） */
    private String extractDescription(String raw) {
        // 去掉所有 [FILE] 和代码块
        String text = raw.replaceAll("(?i)\\[FILE\\]\\s*[^\\r\\n]+", "")
                .replaceAll("```[\\s\\S]*?```", "").trim();
        if (text.length() > 200) text = text.substring(0, 200) + "...";
        return text;
    }

    // ─── 片段合并 ───

    /**
     * 检测 AI 返回的是完整文件还是代码片段，如果是片段则合并回原始文件。
     *
     * <p>很多模型（DeepSeek/GLM 等）倾向于只返回修改的元素片段，
     * 而不是完整文件。此方法将片段安全地合并回原文件。</p>
     */
    private List<Map<String, String>> mergeSnippetsWithOriginals(
            List<Map<String, String>> parsedFiles,
            List<Map<String, String>> originalFiles) {

        List<Map<String, String>> result = new ArrayList<>();

        for (Map<String, String> parsed : parsedFiles) {
            String path = parsed.get("path");
            String content = parsed.get("content");
            String lang = parsed.getOrDefault("language", "html");
            if (content == null || content.isBlank()) continue;

            // 判断是否为完整文件
            boolean isFullHtml = content.trim().startsWith("<!DOCTYPE")
                    || content.trim().startsWith("<html")
                    || content.contains("<head>")
                    || content.contains("<body>");
            boolean isFullVue = content.contains("<template>") && content.contains("<script");
            boolean isFullCss = path.endsWith(".css") && content.length() > 100;
            boolean isLongEnough = content.length() > 500;

            if (isFullHtml || isFullVue || isFullCss || isLongEnough) {
                // 看起来是完整文件，直接使用
                result.add(parsed);
                log.info("文件 [{}] 识别为完整文件 ({} 字符)，直接使用", path, content.length());
                continue;
            }

            // ── 是代码片段 → 需要合并回原始文件 ──
            log.info("文件 [{}] 识别为代码片段 ({} 字符)，查找原始文件合并", path, content.length());

            // 查找对应的原始文件
            Map<String, String> original = findOriginalFile(path, originalFiles);
            if (original != null && original.get("content") != null) {
                String merged = mergeSnippetIntoFile(original.get("content"), content, path);
                result.add(Map.of("path", path, "language", lang, "content", merged));
                log.info("片段合并完成: {} (原始{}→合并{} 字符)",
                        path, original.get("content").length(), merged.length());
            } else {
                // 无原始文件可合并 → 包装为最小可运行 HTML
                log.warn("片段 [{}] 无原始文件可合并，包装为最小 HTML", path);
                String wrapped = wrapSnippetAsHtml(content);
                result.add(Map.of("path", "index.html", "language", "html", "content", wrapped));
            }
        }
        return result;
    }

    /** 查找原始文件（按路径匹配） */
    private Map<String, String> findOriginalFile(String path, List<Map<String, String>> originals) {
        if (originals == null) return null;
        // 精确匹配
        for (Map<String, String> f : originals) {
            if (path.equals(f.get("path"))) return f;
        }
        // 模糊匹配（路径尾相同）
        for (Map<String, String> f : originals) {
            String op = f.get("path");
            if (op != null && (op.endsWith("/" + path) || path.endsWith("/" + op))) return f;
        }
        // 返回第一个 HTML 文件
        for (Map<String, String> f : originals) {
            String op = f.get("path");
            if (op != null && (op.endsWith(".html") || op.endsWith(".htm"))) return f;
        }
        // 返回任意第一个文件
        return originals.isEmpty() ? null : originals.get(0);
    }

    /**
     * 将 AI 返回的 HTML 片段合并回原始文件。
     * 策略：找到片段中的根元素标签，在原始文件中定位并用新内容替换。
     */
    private String mergeSnippetIntoFile(String original, String snippet, String path) {
        // 尝试提取片段中的 HTML 标签名
        String tagName = extractFirstTagName(snippet);
        if (tagName == null || tagName.isEmpty()) {
            // 无法识别标签 → 追加样式注入
            if (snippet.contains(":") && snippet.length() < 100) {
                // 看起来像 CSS 属性 → 注入到内联样式
                return injectInlineStyle(original, snippet);
            }
            // 直接返回原始文件（不修改）
            log.warn("无法解析片段标签，保持原始文件不变");
            return original;
        }

        // 在原始文件中找到对应标签并替换
        String result = replaceTagInHtml(original, tagName, snippet);
        if (!result.equals(original)) {
            return result; // 成功替换
        }

        // 替换失败 → 尝试样式注入
        if (snippet.contains("color") || snippet.contains("background") || snippet.contains("style")) {
            return injectInlineStyle(original, extractStyleProperties(snippet));
        }

        log.warn("无法将片段合并到原始文件 [{}]，保持原始文件不变", path);
        return original;
    }

    /** 提取第一个 HTML 标签名 */
    private String extractFirstTagName(String html) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<(\\w+)[\\s>]").matcher(html);
        return m.find() ? m.group(1).toLowerCase() : null;
    }

    /** 在 HTML 中用新片段替换匹配的标签 */
    private String replaceTagInHtml(String html, String tagName, String newContent) {
        // 匹配 <tagname ...>...</tagname> 的最外层
        String regex = "(?s)(<" + tagName + "\\b[^>]*>)(.*?)(</" + tagName + ">)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(html);
        if (m.find()) {
            return m.replaceFirst(java.util.regex.Matcher.quoteReplacement(newContent));
        }
        return html;
    }

    /** 提取 style 属性值 */
    private String extractStyleProperties(String snippet) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("style\\s*=\\s*\"([^\"]+)\"").matcher(snippet);
        if (m.find()) return m.group(1);
        m = java.util.regex.Pattern.compile("style\\s*=\\s*'([^']+)'").matcher(snippet);
        if (m.find()) return m.group(1);
        return snippet.trim();
    }

    /** 将 CSS 属性注入原有内联样式 */
    private String injectInlineStyle(String html, String styleProps) {
        if (styleProps == null || styleProps.isBlank()) return html;
        // 在第一个匹配的标签上注入 style 属性
        return html.replaceFirst("(<\\w+)([^>]*?)(/?>)",
                "$1 style=\"" + styleProps + "\"$2$3");
    }

    /** 将代码片段包装为最小可运行 HTML */
    private String wrapSnippetAsHtml(String snippet) {
        if (snippet.trim().startsWith("<!DOCTYPE") || snippet.trim().startsWith("<html")) {
            return snippet;
        }
        return "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n"
                + "<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n"
                + "<style>body{margin:0;font-family:system-ui,sans-serif;}</style>\n</head>\n<body>\n"
                + snippet + "\n</body>\n</html>";
    }

    /** 将合并后的文件列表构建为 [FILE] 标记格式，前端 parseAiMessage 可正确解析 */
    private String buildFileMarkersContent(List<Map<String, String>> files) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> f : files) {
            String path = f.get("path");
            String content = f.get("content");
            String lang = f.getOrDefault("language", detectLang(path));
            if (content == null) continue;
            sb.append("[FILE] ").append(path).append("\n");
            sb.append("```").append(lang).append("\n");
            sb.append(content).append("\n");
            sb.append("```\n\n");
        }
        return sb.toString();
    }

    // ─── 持久化 ───

    /**
     * 将修改后的文件写入 DB + 磁盘。
     * 对于 Vue 项目，还会触发 npm rebuild。
     * 注意：此方法由 modifyFiles 内部调用，不能使用 @Transactional（自调用不走代理）。
     */
    void persistModifiedFiles(Long conversationId, List<Map<String, String>> modifiedFiles, String type) {
        // 先删除旧的 project_files
        var oldFiles = projectFileMapper.findByConversationId(conversationId);
        for (var pf : oldFiles) {
            projectFileMapper.deleteById(pf.getId());
        }

        // 写入新的
        for (Map<String, String> f : modifiedFiles) {
            var pf = new org.example.entity.ProjectFile();
            pf.setConversationId(conversationId);
            pf.setFilePath(f.get("path"));
            pf.setContent(f.get("content"));
            pf.setCreatedAt(LocalDateTime.now());
            projectFileMapper.insert(pf);
        }

        // 写入磁盘
        log.info("开始写入磁盘: convId={}, type={}, 文件数={}", conversationId, type, modifiedFiles.size());
        deployService.writeFilesToDisk(conversationId, null,
                type != null ? type.toLowerCase() : "single_file", modifiedFiles);

        // Vue 项目：触发重新构建
        if ("VUE_PROJECT".equalsIgnoreCase(type)) {
            java.nio.file.Path projectDir = java.nio.file.Path.of(
                    System.getProperty("user.dir"),
                    "tmp/code_output/vue_project_conv" + conversationId);
            vueProjectBuilder.buildAsync(projectDir);
        }

        log.info("文件修改持久化完成: convId={}, 文件数={}", conversationId, modifiedFiles.size());
    }

    // ─── 工具 ───

    private String detectLang(String path) {
        if (path == null) return "text";
        String lower = path.toLowerCase();
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".json")) return "json";
        return "text";
    }

    private String buildConcatenatedCode(List<Map<String, String>> files) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> f : files) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append("// ===== ").append(f.get("path")).append(" =====\n").append(f.get("content"));
        }
        return sb.toString();
    }

    private String wrap(String content) {
        try {
            return "{\"d\":" + toJson(content) + "}";
        } catch (Exception e) {
            return "{\"d\":\"\"}";
        }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "\"\"";
        }
    }

    private void safeSendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(wrap(msg)));
        } catch (IOException ignored) {}
    }
}
