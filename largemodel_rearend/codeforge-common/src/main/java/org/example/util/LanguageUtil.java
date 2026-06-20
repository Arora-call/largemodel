/**
 * 模块：工具类
 * 功能：统一语言检测和文件扩展名映射，消除多处重复的 switch/if-else
 * 作者：yx
 * 创建时间：2026-06-17
 */
package org.example.util;

public final class LanguageUtil {

    private LanguageUtil() {}

    /** 根据文件扩展名检测语言 */
    public static String detectByExt(String path) {
        if (path == null) return "text";
        String name = path.toLowerCase();
        if (name.endsWith(".vue")) return "vue";
        if (name.endsWith(".java")) return "java";
        if (name.endsWith(".py")) return "python";
        if (name.endsWith(".js") || name.endsWith(".mjs")) return "javascript";
        if (name.endsWith(".ts")) return "typescript";
        if (name.endsWith(".html")) return "html";
        if (name.endsWith(".css")) return "css";
        if (name.endsWith(".json")) return "json";
        if (name.endsWith(".xml")) return "xml";
        if (name.endsWith(".md")) return "markdown";
        if (name.endsWith(".yml") || name.endsWith(".yaml")) return "yaml";
        if (name.endsWith(".sql")) return "sql";
        if (name.endsWith(".sh") || name.endsWith(".bash")) return "shell";
        return "text";
    }

    /** 根据代码内容检测语言 */
    public static String detectByContent(String code) {
        if (code == null || code.isBlank()) return "text";
        if (code.matches("(?s).*<template.*|<script.*")) return "vue";
        if (code.matches("(?s).*public\\s+class.*|@RestController.*")) return "java";
        if (code.matches("(?s).*<!DOCTYPE\\s+html.*|<html.*")) return "html";
        if (code.matches("(?s).*def\\s+\\w+\\s*\\(.*")) return "python";
        return "text";
    }

    /** 语言 → 扩展名 */
    public static String toExt(String lang) {
        if (lang == null) return ".txt";
        return switch (lang.toLowerCase()) {
            case "java" -> ".java";
            case "python", "py" -> ".py";
            case "vue", "html" -> ".html";
            case "javascript", "js" -> ".js";
            case "typescript", "ts" -> ".ts";
            case "css" -> ".css";
            case "json" -> ".json";
            case "xml" -> ".xml";
            case "markdown", "md" -> ".md";
            default -> ".txt";
        };
    }

    /** 语言 → CSS 渐变封面 */
    public static String toCoverGradient(String lang) {
        if (lang == null) return "linear-gradient(135deg,#6366f1,#8b5cf6)";
        return switch (lang.toLowerCase()) {
            case "vue" -> "linear-gradient(135deg,#42b883,#35495e)";
            case "html" -> "linear-gradient(135deg,#e34c26,#f06529)";
            case "java" -> "linear-gradient(135deg,#f89820,#5382a1)";
            case "python" -> "linear-gradient(135deg,#3776ab,#ffd43b)";
            default -> "linear-gradient(135deg,#6366f1,#8b5cf6)";
        };
    }
}