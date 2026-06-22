/**
 * AI 模型配置实体 — 管理多个 API Key 和模型参数
 * 安全说明：apiKeyEncrypted 使用 AES-256-GCM 加密存储，前端永不返回原始值
 */
package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("model_configs")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 显示名称 */
    private String name;

    /** 提供商: deepseek / openai / zhipu / custom */
    private String provider;

    /** API 端点 */
    private String baseUrl;

    /** AES-256-GCM 加密的 API Key（前端不可见） */
    private String apiKeyEncrypted;

    /** 模型标识 */
    private String modelName;

    /** 温度参数 */
    @Builder.Default
    private Double temperature = 0.7;

    /** 最大输出 Token */
    @Builder.Default
    private Integer maxTokens = 16384;

    /** 是否启用 */
    @Builder.Default
    private Integer isEnabled = 1;

    /** 是否默认模型 */
    @Builder.Default
    private Integer isDefault = 0;

    /** 排序 */
    @Builder.Default
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
