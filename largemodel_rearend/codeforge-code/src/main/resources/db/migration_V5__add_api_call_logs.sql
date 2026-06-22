-- CodeForge V5: API 调用日志表（监控统计）
CREATE TABLE IF NOT EXISTS `api_call_logs` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `endpoint`   VARCHAR(200) DEFAULT NULL COMMENT '接口路径',
    `user_id`    BIGINT   DEFAULT NULL COMMENT '调用用户',
    `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
    `token_used` INT      DEFAULT NULL COMMENT '消耗 Token',
    `latency_ms` BIGINT   DEFAULT NULL COMMENT '响应延迟(ms)',
    `success`    TINYINT  DEFAULT 1 COMMENT '是否成功',
    `error_msg`  VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API调用日志';
