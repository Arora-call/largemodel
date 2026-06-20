-- ============================================
-- CodeForge 数据库迁移 V3
-- 新增 Agent 工作流表
-- ============================================

CREATE TABLE IF NOT EXISTS `agent_workflows` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(200) NOT NULL                COMMENT '工作流名称',
    `description` VARCHAR(1000) DEFAULT NULL           COMMENT '工作流描述',
    `agent_chain` VARCHAR(500) NOT NULL                COMMENT 'Agent链: analyzer,architect,coder,tester,reviewer',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/COMPLETED/FAILED',
    `result`      MEDIUMTEXT   DEFAULT NULL            COMMENT '执行结果汇总',
    `requirement` TEXT         DEFAULT NULL            COMMENT '用户需求（原始输入）',
    `user_id`     BIGINT       NOT NULL                COMMENT '所属用户',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent工作流表';
