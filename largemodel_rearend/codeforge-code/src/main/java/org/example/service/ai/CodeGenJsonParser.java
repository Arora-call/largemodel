/**
 * 模块：AI代码生成
 * 功能：从 AI 原始响应中提取 JSON 并解析为结构化 DTO
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.codegen.MultiFileCodeResult;
import org.example.dto.codegen.SingleFileCodeResult;
import org.example.dto.codegen.VueProjectCodeResult;

/**
 * AI 响应 JSON 解析器。
 * <p>
 * 尝试从 AI 原始文本中提取 JSON（支持 ```json 包裹或裸 JSON），
 * 反序列化为结构化 DTO。失败返回 null，调用方回退到正则解析。
 */
@Slf4j
public final class CodeGenJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CodeGenJsonParser() {}

    public static SingleFileCodeResult parseSingleFile(String raw) {
        String json = extractJson(raw, "htmlCode");
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, SingleFileCodeResult.class);
        } catch (Exception e) {
            log.debug("单文件JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static MultiFileCodeResult parseMultiFile(String raw) {
        String json = extractJson(raw, "cssCode");
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, MultiFileCodeResult.class);
        } catch (Exception e) {
            log.debug("多文件JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static VueProjectCodeResult parseVueProject(String raw) {
        String json = extractJson(raw, "files");
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, VueProjectCodeResult.class);
        } catch (Exception e) {
            log.debug("Vue项目JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从原始文本中提取 JSON 对象字符串。
     * 用括号深度追踪而非正则，避免 JSON 内容中的 ``` 截断问题。
     *
     * @param keyHint 期望的 JSON key，用于跳过非 JSON 文本
     */
    private static String extractJson(String raw, String keyHint) {
        if (raw == null || raw.isBlank()) return null;

        // 一、尝试跳过 ```json 包裹
        String content = raw;
        int fenceStart = raw.indexOf("```");
        if (fenceStart >= 0) {
            int fenceEnd = raw.indexOf("\n", fenceStart);
            if (fenceEnd > 0) {
                // 找到 ```json 后的内容
                int jsonStart = raw.indexOf("{", fenceEnd);
                if (jsonStart < 0) jsonStart = raw.indexOf("{");
                if (jsonStart >= 0) {
                    String json = extractBraced(raw, jsonStart);
                    if (json != null && json.contains("\"" + keyHint + "\"")) return json;
                }
            }
        }

        // 二、搜索裸 JSON（包含期望 key 的 { ... }）
        int searchFrom = 0;
        while (searchFrom < raw.length()) {
            int brace = raw.indexOf("{", searchFrom);
            if (brace < 0) break;
            String json = extractBraced(raw, brace);
            if (json != null && json.contains("\"" + keyHint + "\"")) return json;
            searchFrom = brace + 1;
        }

        return null;
    }

    /** 从指定位置提取配对的 { ... } JSON，通过深度计数处理内嵌括号 */
    private static String extractBraced(String s, int start) {
        if (s.charAt(start) != '{') return null;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escaped) { escaped = false; continue; }
                if (c == '\\') { escaped = true; continue; }
                if (c == '"') { inString = false; }
            } else {
                if (c == '"') { inString = true; }
                else if (c == '{') { depth++; }
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return s.substring(start, i + 1);
                }
            }
        }
        return null; // 未闭合
    }
}
