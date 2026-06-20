/**
 * 代码审查结果
 */
package org.example.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReviewResult {
    /** 总体评分 (0-100) */
    private Integer score;
    /** 审查摘要 */
    private String summary;
    /** 各维度结果 */
    private List<DimensionResult> dimensions;
    /** 原始 Markdown 文本 */
    private String rawText;

    @Data
    @Builder
    public static class DimensionResult {
        private String name;      // 维度名称 (security/performance/style/best_practice)
        private String label;     // 中文标签
        private Integer score;    // 维度评分
        private String content;   // 审查内容
    }
}
