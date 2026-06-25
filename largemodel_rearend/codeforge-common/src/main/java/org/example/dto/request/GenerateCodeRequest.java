/**
 * 模块：AI代码生成
 * 功能：代码生成请求体，包含对话ID、Prompt、生成类型
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class GenerateCodeRequest {

    private Long conversationId;

    @NotBlank(message = "需求描述不能为空")
    private String prompt;

    /** NATIVE / ENGINEERING */
    private String type = "NATIVE";

    /** 语言: java / python / vue 等 */
    private String language = "java";

    /** 用户原始提问（不含代码上下文），用于保存对话记录 */
    private String originalPrompt;

    /** AI 模型配置 ID（null 则使用默认模型） */
    private Long modelId;

    /** 知识库文档 ID 列表 — 作为 RAG 上下文注入 Prompt */
    private List<Long> knowledgeDocIds;

    /** 是否启用知识库自动检索（根据 Prompt 关键词自动匹配文档） */
    private boolean autoSearchKnowledge = false;

    /** 知识库上下文（由服务端构建，非客户端传入，序列化忽略） */
    @JsonIgnore
    private String knowledgeContext;
}
