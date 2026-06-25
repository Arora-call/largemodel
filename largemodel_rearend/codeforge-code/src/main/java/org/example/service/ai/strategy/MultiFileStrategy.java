/**
 * 模块：AI代码生成 - 多文件策略
 * 功能：多文件 HTML 模式，生成分离的 index.html + style.css + script.js 三个文件
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.codegen.MultiFileCodeResult;
import org.example.dto.request.GenerateCodeRequest;
import org.example.enums.GenerateMode;
import org.example.service.ai.CodeGenJsonParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件生成策略 — 生成分离的 HTML/CSS/JS 三个文件。
 * <p>
 * AI 输出 3 个命名的代码块（```html, ```css, ```javascript），
 * 按文件名标记拆分为独立文件。
 */
@Slf4j
@Component
public class MultiFileStrategy extends AbstractCodeGenerationStrategy {

    /** 通用代码块提取 — 也匹配无语言标签的 ``` 块 */
    private static final Pattern CODE_BLOCK_RE = Pattern.compile(
            "```(\\w*)\\s*[\\r\\n]*([\\s\\S]*?)```");

    @Override
    public GenerateMode supportedMode() {
        return GenerateMode.MULTI_FILE;
    }

    @Override
    protected String selectSystemPrompt(GenerateCodeRequest request) {
        return promptService.getMultiFileSystemPrompt();
    }

    /** 重写：多文件模式追加格式强化标签 */
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> buildMessages(
            String systemPrompt, GenerateCodeRequest request) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from(systemPrompt));

        List<dev.langchain4j.data.message.ChatMessage> history = loadHistory(request.getConversationId());
        if (!history.isEmpty()) {
            messages.addAll(history);
        }

        // 格式强化：要求 JSON 输出
        String finalPrompt = """
                [FORMAT: 请严格输出系统提示中指定的 JSON 格式。htmlCode/cssCode/jsCode 缺一不可。]

                %s""".formatted(request.getPrompt());
        messages.add(dev.langchain4j.data.message.UserMessage.from(finalPrompt));
        return messages;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseResponse(String raw, Long conversationId,
                                                 GenerateCodeRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("language", "html");

        List<Map<String, String>> files = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();

        // 优先尝试 JSON 结构化解析
        MultiFileCodeResult parsed = CodeGenJsonParser.parseMultiFile(raw);
        if (parsed != null) {
            if (parsed.getHtmlCode() != null && !parsed.getHtmlCode().isBlank()) {
                files.add(Map.of("path", "index.html", "language", "html", "content", parsed.getHtmlCode()));
            }
            if (parsed.getCssCode() != null && !parsed.getCssCode().isBlank()) {
                files.add(Map.of("path", "style.css", "language", "css", "content", parsed.getCssCode()));
            }
            if (parsed.getJsCode() != null && !parsed.getJsCode().isBlank()) {
                files.add(Map.of("path", "script.js", "language", "javascript", "content", parsed.getJsCode()));
            }
            result.put("text", parsed.getDescription() != null ? parsed.getDescription() : "");
            log.info("多文件JSON解析成功, 文件数={}", files.size());
        }

        boolean jsonSuccess = !files.isEmpty();

        // JSON 解析失败 → 正则兜底
        if (files.isEmpty()) {
            Matcher m = CODE_BLOCK_RE.matcher(raw);
            int idx = 0;
            while (m.find()) {
                String lang = m.group(1).trim().toLowerCase();
                String code = m.group(2).trim();
                if (code.isEmpty()) continue;
                idx++;
                String filePath = extractFileAnnotation(code);
                if (!filePath.isEmpty()) {
                    code = stripFileAnnotation(code);
                } else {
                    filePath = inferFileName(lang, idx);
                }
                if (seenPaths.contains(filePath)) continue;
                seenPaths.add(filePath);
                files.add(Map.of("path", filePath, "language", lang, "content", code));
            }
            log.info("多文件正则解析, 匹配到{}个代码块, 生成{}个文件", idx, files.size());
        }

        // 兜底：没有代码块，整个 raw 作为 index.html
        if (files.isEmpty() && !raw.isBlank()) {
            log.warn("多文件兜底: 未匹配到代码块, 全部原始内容作为 index.html");
            files.add(Map.of("path", "index.html", "language", "html", "content", raw.trim()));
        }

        // 拼接所有代码（前端预览用）
        StringBuilder allCode = new StringBuilder();
        for (Map<String, String> f : files) {
            if (!allCode.isEmpty()) allCode.append("\n\n");
            allCode.append("// ===== ").append(f.get("path")).append(" =====\n")
                   .append(f.get("content"));
        }

        result.put("files", files);
        result.put("code", allCode.toString());
        if (!jsonSuccess) result.put("text", extractText(raw));
        result.put("segments", extractSegments(raw));

        log.info("多文件生成完成, conversationId={}, 文件数={}", conversationId, files.size());
        return result;
    }

    // persistFiles 使用基类默认实现（DB + 磁盘双写）

    /** 根据语言 + 序号推断文件名 */
    private String inferFileName(String lang, int idx) {
        return switch (lang) {
            case "html" -> "index.html";
            case "css" -> "style.css";
            case "javascript", "js" -> "script.js";
            default -> "file" + idx + ".txt";
        };
    }

    /** 从代码首行提取 // File: xxx 标注 */
    private String extractFileAnnotation(String code) {
        String firstLine = code.lines().findFirst().orElse("");
        if (firstLine.matches("^\\s*//\\s*File:\\s*.+")) {
            return firstLine.replaceFirst("^\\s*//\\s*File:\\s*", "").trim();
        }
        return "";
    }

    /** 去掉代码首行的 // File: xxx 标注 */
    private String stripFileAnnotation(String code) {
        String firstLine = code.lines().findFirst().orElse("");
        if (firstLine.matches("^\\s*//\\s*File:\\s*.+")) {
            return code.replaceFirst("^\\s*//\\s*File:[^\\n]*\\n?", "").trim();
        }
        return code;
    }
}
