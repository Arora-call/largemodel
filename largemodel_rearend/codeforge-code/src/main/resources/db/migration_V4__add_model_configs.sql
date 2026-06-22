-- ============================================
-- CodeForge 数据库迁移 V4
-- 新增 AI 模型配置表（多 API Key 管理）
-- ============================================
use largemodel;

CREATE TABLE IF NOT EXISTS `model_configs` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`             VARCHAR(100) NOT NULL                COMMENT '显示名称',
    `provider`         VARCHAR(50)  NOT NULL                COMMENT '提供商: deepseek/openai/zhipu/custom',
    `base_url`         VARCHAR(500) NOT NULL                COMMENT 'API 端点',
    `api_key_encrypted` VARCHAR(500) NOT NULL               COMMENT 'AES-256-GCM 加密的 API Key',
    `model_name`       VARCHAR(100) NOT NULL                COMMENT '模型标识',
    `temperature`      DOUBLE       DEFAULT 0.7             COMMENT '默认温度',
    `max_tokens`       INT          DEFAULT 16384           COMMENT '最大输出 Token',
    `is_enabled`       TINYINT      DEFAULT 1               COMMENT '是否启用 0/1',
    `is_default`       TINYINT      DEFAULT 0               COMMENT '是否默认模型 0/1',
    `sort_order`       INT          DEFAULT 0               COMMENT '排序权重',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`is_enabled`),
    KEY `idx_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型配置表';
