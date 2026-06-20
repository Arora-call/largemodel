/**
 * 代码审查请求
 */
package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodeReviewRequest {
    /** 待审查的代码 */
    @NotBlank
    private String code;

    /** 编程语言（可选，用于针对性审查） */
    private String language;

    /** 审查维度（可选，逗号分隔：security,performance,style,best_practice） */
    private String dimensions;

    /** 额外上下文（如项目描述） */
    private String context;
}
