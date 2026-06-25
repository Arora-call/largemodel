/**
 * 模块：AI代码生成 - 单文件策略
 * 功能：单文件 HTML 模式，生成一个包含内联 CSS/JS 的完整 .html 文件
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.codegen.SingleFileCodeResult;
import org.example.dto.request.GenerateCodeRequest;
import org.example.enums.GenerateMode;
import org.example.service.ai.CodeGenJsonParser;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 单文件生成策略 — 生成单个 HTML 文件（CSS/JS 内联）。
 * <p>
 * AI 输出一个 ```html 代码块，解析后作为单个文件返回。
 */
@Slf4j
@Component
public class SingleFileStrategy extends AbstractCodeGenerationStrategy {

    @Override
    public GenerateMode supportedMode() {
        return GenerateMode.SINGLE_FILE;
    }

    @Override
    protected String selectSystemPrompt(GenerateCodeRequest request) {
        return promptService.getSingleFileSystemPrompt();
    }

    /** 追加 JSON 格式强化标签 */
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> buildMessages(
            String systemPrompt, GenerateCodeRequest request) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from(systemPrompt));
        List<dev.langchain4j.data.message.ChatMessage> history = loadHistory(request.getConversationId());
        if (!history.isEmpty()) messages.addAll(history);
        messages.add(dev.langchain4j.data.message.UserMessage.from(
                "[FORMAT: 请严格输出系统提示中指定的 JSON 格式。]\n\n" + request.getPrompt()));
        return messages;
    }

    @Override
    protected Map<String, Object> parseResponse(String raw, Long conversationId,
                                                 GenerateCodeRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("language", "html");

        // 优先尝试 JSON 结构化解析
        SingleFileCodeResult parsed = CodeGenJsonParser.parseSingleFile(raw);
        List<Map<String, String>> files = new ArrayList<>();

        if (parsed != null && parsed.getHtmlCode() != null && !parsed.getHtmlCode().isBlank()) {
            files.add(Map.of("path", "index.html", "language", "html", "content", parsed.getHtmlCode()));
            result.put("text", parsed.getDescription() != null ? parsed.getDescription() : "");
            log.info("单文件JSON解析成功, 代码长度={}", parsed.getHtmlCode().length());
        } else {
            // 兜底：正则提取 HTML 代码块
            String code = extractCode(raw);
            if (code.isEmpty()) code = raw.trim();
            files.add(Map.of("path", "index.html", "language", "html", "content", code));
            result.put("text", extractText(raw));
            log.info("单文件正则解析, 代码长度={}", code.length());
        }

        result.put("files", files);
        result.put("code", files.get(0).get("content"));
        result.put("segments", extractSegments(raw));
        return result;
    }

    // persistFiles 使用基类默认实现（DB + 磁盘双写）
}
