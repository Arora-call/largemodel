/**
 * 模块：AI代码生成
 * 功能：代码生成请求体，包含对话ID、Prompt、生成类型
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateCodeRequest {

    private Long conversationId;

    @NotBlank(message = "需求描述不能为空")
    private String prompt;

    /** NATIVE / ENGINEERING */
    private String type = "NATIVE";

    /** 语言: java / python / vue 等 */
    private String language = "java";
}
