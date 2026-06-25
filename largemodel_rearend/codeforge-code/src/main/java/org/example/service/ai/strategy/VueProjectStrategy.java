/**
 * 模块：AI代码生成 - Vue3 项目策略
 * 功能：Vue3 工程项目模式，生成完整的 Vite + Vue Router 工程结构
 * 作者：yx
 * 创建时间：2026-06-24
 * 修改记录：
 *  2026-06-24 初始化 — [FILE] 标记格式，纯 prompt 解析，兼容所有模型
 */
package org.example.service.ai.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.codegen.VueProjectCodeResult;
import org.example.dto.request.GenerateCodeRequest;
import org.example.enums.GenerateMode;
import org.example.service.ai.CodeGenJsonParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vue3 项目生成策略 — 使用 [FILE] + ``` 标记格式。
 * <p>
 * AI 输出结构化文本 + 代码块，由正则解析为多文件 Vue3 项目。
 * 不依赖 function calling，兼容 DeepSeek/GLM/GPT 等所有模型。
 */
@Slf4j
@Component
public class VueProjectStrategy extends AbstractCodeGenerationStrategy {

    private final org.example.service.ai.VueProjectBuilder vueProjectBuilder;

    public VueProjectStrategy(org.example.service.ai.VueProjectBuilder vueProjectBuilder) {
        this.vueProjectBuilder = vueProjectBuilder;
    }

    // [FILE] path\n```lang\ncode\n``` 主解析正则
    private static final Pattern FILE_BLOCK_RE = Pattern.compile(
            "(?i)\\[FILE\\]\\s*([^\\r\\n]+)\\s*[\\r\\n]+```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```");
    private static final Pattern DESC_RE = Pattern.compile(
            "(?i)\\[DESC\\]\\s*([^\\r\\n]+)");
    private static final Pattern NAME_RE = Pattern.compile(
            "(?i)\\[NAME\\]\\s*([a-z][a-z0-9-]{1,40})");

    @Override
    public GenerateMode supportedMode() {
        return GenerateMode.VUE_PROJECT;
    }

    @Override
    protected String selectSystemPrompt(GenerateCodeRequest request) {
        return promptService.getVueProjectPromptSystemPrompt();
    }

    /** 追加格式强化标签 */
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> buildMessages(
            String systemPrompt, GenerateCodeRequest request) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from(systemPrompt));

        List<dev.langchain4j.data.message.ChatMessage> history = loadHistory(request.getConversationId());
        if (!history.isEmpty()) {
            messages.addAll(history);
        }

        String finalPrompt = """
                [FORMAT: 请严格输出系统提示中指定的 JSON 格式。files 数组必须包含所有强制文件。]

                %s""".formatted(request.getPrompt());
        messages.add(dev.langchain4j.data.message.UserMessage.from(finalPrompt));
        return messages;
    }

    @Override
    protected Map<String, Object> parseResponse(String raw, Long conversationId,
                                                 GenerateCodeRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("projectType", "frontend");
        result.put("language", "vue");

        // 优先尝试 JSON 结构化解析
        VueProjectCodeResult parsed = CodeGenJsonParser.parseVueProject(raw);
        if (parsed != null && parsed.getFiles() != null && !parsed.getFiles().isEmpty()) {
            List<Map<String, String>> files = new ArrayList<>();
            for (VueProjectCodeResult.FileEntry fe : parsed.getFiles()) {
                if (fe.getPath() != null && fe.getContent() != null && !fe.getContent().isBlank()) {
                    files.add(Map.of("path", fe.getPath(), "language", detectLangFromPath(fe.getPath()),
                            "content", fe.getContent()));
                }
            }
            StringBuilder allCode = new StringBuilder();
            for (Map<String, String> f : files) {
                if (!allCode.isEmpty()) allCode.append("\n\n");
                allCode.append("// ===== ").append(f.get("path")).append(" =====\n").append(f.get("content"));
            }
            result.put("files", files);
            result.put("code", allCode.toString());
            result.put("text", parsed.getDescription() != null ? parsed.getDescription() : "");
            result.put("folderName", parsed.getProjectName() != null ? parsed.getProjectName() : "vue-app");
            result.put("segments", extractSegments(raw));
            log.info("Vue项目JSON解析成功, 文件数={}", files.size());
            return result;
        }

        // JSON 解析失败 → 正则兜底（[FILE] 标记）
        log.info("Vue项目回退到[FILE]正则解析");
        Matcher descMatcher = DESC_RE.matcher(raw);
        String overview = descMatcher.find() ? descMatcher.group(1).trim() : "";
        if (overview.isEmpty()) {
            overview = raw.lines()
                    .filter(l -> !l.isBlank()
                            && !l.trim().matches("(?i)\\[PROJECT.*|\\[FILE.*|\\[NAME.*|\\[DESC.*")
                            && !l.trim().startsWith("```"))
                    .findFirst().orElse("").trim();
            if (overview.length() > 100) overview = overview.substring(0, 100) + "...";
        }
        result.put("text", overview);

        // 提取 [NAME]
        Matcher nameMatcher = NAME_RE.matcher(raw);
        String folderName = nameMatcher.find() ? nameMatcher.group(1).trim() : "";
        if (folderName.isEmpty()) folderName = generateFolderName(request.getPrompt());
        result.put("folderName", folderName);

        // 核心：[FILE] 解析
        List<Map<String, String>> files = new ArrayList<>();
        Set<String> captured = new HashSet<>();
        Matcher fileMatcher = FILE_BLOCK_RE.matcher(raw);
        while (fileMatcher.find()) {
            String path = fileMatcher.group(1).trim();
            String lang = fileMatcher.group(2).isEmpty() ? detectLangFromPath(path) : fileMatcher.group(2);
            String code = fileMatcher.group(3).trim();
            if (code.isEmpty()) continue;
            captured.add(path);
            files.add(Map.of("path", path, "language", lang, "content", code));
        }
        log.info("Vue主正则提取 {} 个文件", files.size());

        // 兜底：[FILE] 段补充遗漏
        if (raw.contains("[FILE]")) {
            String[] sections = raw.split("(?i)\\[FILE\\]\\s*");
            for (String section : sections) {
                section = section.trim();
                if (section.isEmpty()) continue;
                int nl = section.indexOf('\n');
                String pathLine = nl > 0 ? section.substring(0, nl).trim() : section.trim();
                if (pathLine.contains("[") || pathLine.isEmpty() || captured.contains(pathLine)) continue;
                String rest = nl > 0 ? section.substring(nl + 1).trim() : "";
                Matcher codeMatcher = Pattern.compile(
                        "```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```").matcher(rest);
                if (codeMatcher.find()) {
                    String code = codeMatcher.group(2).trim();
                    if (!code.isEmpty()) {
                        captured.add(pathLine);
                        files.add(Map.of("path", pathLine, "language",
                                codeMatcher.group(1).isEmpty() ? detectLangFromPath(pathLine) : codeMatcher.group(1),
                                "content", code));
                    }
                }
            }
            log.info("Vue兜底补充后共 {} 个文件", files.size());
        }

        // 兜底2：通用代码块提取
        if (files.isEmpty()) {
            Matcher blockMatcher = Pattern.compile(
                    "```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```").matcher(raw);
            int idx = 0;
            while (blockMatcher.find()) {
                String code = blockMatcher.group(2).trim();
                if (code.isEmpty()) continue;
                idx++;
                String lang = blockMatcher.group(1).isEmpty() ? "text" : blockMatcher.group(1);
                files.add(Map.of("path", "file" + idx + extForLang(lang),
                        "language", lang, "content", code));
            }
            log.warn("Vue兜底2: 通用代码块提取 {} 个文件", files.size());
        }

        // 最终兜底
        if (files.isEmpty() && !raw.isBlank()) {
            log.warn("Vue最终兜底: 整个原始内容作为 output.txt");
            files.add(Map.of("path", "output.txt", "language", "text", "content", raw.trim()));
        }

        // 拼接 allCode
        StringBuilder allCode = new StringBuilder();
        for (Map<String, String> f : files) {
            if (!allCode.isEmpty()) allCode.append("\n\n");
            allCode.append("// ===== ").append(f.get("path")).append(" =====\n").append(f.get("content"));
        }

        result.put("files", files);
        result.put("code", allCode.toString());
        result.put("segments", extractSegments(raw));

        log.info("Vue项目生成完成, conversationId={}, 文件数={}, 代码总长={}",
                conversationId, files.size(), allCode.length());
        return result;
    }

    // persistFiles 使用基类默认实现（DB + 磁盘双写），完成后触发 npm build

    @Override
    @SuppressWarnings("unchecked")
    protected void persistFiles(Long conversationId, Map<String, Object> parsed,
                               org.example.dto.request.GenerateCodeRequest request) {
        super.persistFiles(conversationId, parsed, request);
        // 异步构建 Vue 项目（npm install + npm run build → dist/）
        List<Map<String, String>> files = (List<Map<String, String>>) parsed.get("files");
        if (files != null && !files.isEmpty()) {
            java.nio.file.Path projectDir = java.nio.file.Path.of(
                    System.getProperty("user.dir"),
                    "tmp/code_output/vue_project_conv" + conversationId);
            vueProjectBuilder.buildAsync(projectDir);
        }
    }

    // ─── 工具方法 ───

    private String generateFolderName(String prompt) {
        if (prompt == null || prompt.isBlank()) return "vue-app";
        String[] words = prompt.split("[^a-zA-Z]+");
        List<String> meaningful = new ArrayList<>();
        for (String w : words) {
            if (w.length() >= 2 && !STOP_WORDS.contains(w.toLowerCase())) {
                meaningful.add(w.toLowerCase());
            }
        }
        if (!meaningful.isEmpty()) {
            String name = String.join("-", meaningful.stream().distinct().limit(4).toList());
            if (name.length() > 50) name = name.substring(0, 50);
            return name.replaceAll("-$", "");
        }
        return "vue-app";
    }

    private String detectLangFromPath(String path) {
        if (path == null) return "text";
        String lower = path.toLowerCase();
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".json")) return "json";
        return "text";
    }

    private String extForLang(String lang) {
        return switch (lang.toLowerCase()) {
            case "vue" -> ".vue"; case "html" -> ".html"; case "css" -> ".css";
            case "javascript", "js" -> ".js"; case "json" -> ".json";
            default -> ".txt";
        };
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "shall", "to", "of", "in", "for",
            "on", "with", "at", "by", "from", "or", "and", "not", "no", "but",
            "if", "then", "else", "when", "up", "so", "as", "it", "its", "just",
            "that", "this", "these", "those", "all", "each", "every", "both"
    );
}
