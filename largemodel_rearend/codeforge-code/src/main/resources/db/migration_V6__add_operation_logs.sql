-- CodeForge V6: 操作日志表
CREATE TABLE IF NOT EXISTS `operation_logs` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `module`        VARCHAR(50)  DEFAULT NULL COMMENT '模块',
    `action`        VARCHAR(50)  DEFAULT NULL COMMENT '操作类型',
    `target`        VARCHAR(200) DEFAULT NULL COMMENT '操作对象',
    `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人用户名',
    `detail`        TEXT         DEFAULT NULL COMMENT '详情',
    `ip`            VARCHAR(50)  DEFAULT NULL COMMENT '请求IP',
    `success`       TINYINT      DEFAULT 1 COMMENT '是否成功',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_module` (`module`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
