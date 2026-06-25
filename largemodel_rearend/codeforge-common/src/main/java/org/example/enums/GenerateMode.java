/**
 * 模块：AI代码生成
 * 功能：代码生成模式枚举，定义三种生成模式
 * 作者：yx
 * 创建时间：2026-06-24
 * 修改记录：
 *  2026-06-24 初始化 — 重构AI代码生成模块，统一三种生成模式
 */
package org.example.enums;

/**
 * 代码生成模式
 * <p>
 * SINGLE_FILE — 单文件 HTML 模式（CSS/JS 内联在一个 .html 文件中）
 * MULTI_FILE  — 多文件 HTML 模式（分离 index.html + style.css + script.js）
 * VUE_PROJECT — Vue3 工程项目模式（完整 Vite + Vue Router 工程结构）
 */
public enum GenerateMode {

    SINGLE_FILE("单文件", "生成单个 HTML 文件，CSS/JS 内联"),
    MULTI_FILE("多文件", "生成分离的 HTML/CSS/JS 三个文件"),
    VUE_PROJECT("Vue3项目", "生成完整的 Vue3 + Vite 工程项目");

    private final String displayName;
    private final String description;

    GenerateMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从旧类型字符串转换（兼容 NATIVE / ENGINEERING）
     */
    public static GenerateMode fromLegacyType(String legacyType) {
        if ("ENGINEERING".equalsIgnoreCase(legacyType)) {
            return VUE_PROJECT;
        }
        return SINGLE_FILE;
    }
}
