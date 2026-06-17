-- ============================================
-- 大模型代码应用生成平台 - 数据库初始化脚本
-- 数据库: largemodel
-- ============================================

CREATE DATABASE IF NOT EXISTS `largemodel` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `largemodel`;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `username`     VARCHAR(50)  NOT NULL                  COMMENT '用户名',
    `password`     VARCHAR(200) NOT NULL                  COMMENT '密码（BCrypt加密）',
    `nickname`     VARCHAR(100) DEFAULT NULL              COMMENT '昵称',
    `email`        VARCHAR(150) DEFAULT NULL              COMMENT '邮箱',
    `phone`        VARCHAR(20)  DEFAULT NULL              COMMENT '手机号',
    `avatar`       VARCHAR(500) DEFAULT NULL              COMMENT '头像URL',
    `role`         VARCHAR(20)  NOT NULL DEFAULT 'USER'   COMMENT '角色: USER-普通用户, ADMIN-管理员',
    `status`       INT          NOT NULL DEFAULT 1        COMMENT '状态: 0-禁用, 1-启用',
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `last_login_at` DATETIME    DEFAULT NULL              COMMENT '最后登录时间',
    `created_by`   VARCHAR(50)  DEFAULT NULL              COMMENT '创建人',
    `updated_by`   VARCHAR(50)  DEFAULT NULL              COMMENT '更新人',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 角色表（预留扩展）
-- ============================================
CREATE TABLE IF NOT EXISTS `roles` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `name`        VARCHAR(50)  NOT NULL                  COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL              COMMENT '角色描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================
-- 用户角色关联表（预留扩展，支持多角色）
-- ============================================
CREATE TABLE IF NOT EXISTS `user_roles` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`    BIGINT   NOT NULL                 COMMENT '用户ID',
    `role_id`    BIGINT   NOT NULL                 COMMENT '角色ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================
-- 插入默认角色
-- ============================================
INSERT INTO `roles` (`name`, `description`) VALUES ('USER', '普通用户')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

INSERT INTO `roles` (`name`, `description`) VALUES ('ADMIN', '管理员')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- ============================================
-- 管理员账号：由 DataInitializer 启动时自动创建
-- 默认账号: admin / admin123（BCrypt加密由Java生成）
-- 手动执行此SQL后需 DELETE FROM users WHERE username='admin'
-- 然后重启后端，让Java重新生成加密密码
-- ============================================

-- ============================================
-- 应用表
-- ============================================
CREATE TABLE IF NOT EXISTS `applications` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(200) NOT NULL               COMMENT '应用名称',
    `description` TEXT         DEFAULT NULL           COMMENT '应用描述',
    `type`        VARCHAR(50)  NOT NULL DEFAULT 'NATIVE' COMMENT '类型: NATIVE/ENGINEERING',
    `user_id`     BIGINT       NOT NULL               COMMENT '创建者用户ID',
    `status`      TINYINT      NOT NULL DEFAULT 1     COMMENT '状态: 0-删除, 1-草稿, 2-已生成',
    `source_code` MEDIUMTEXT   DEFAULT NULL           COMMENT '源代码',
    `config_json` JSON         DEFAULT NULL           COMMENT '配置信息',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

-- ============================================
-- 对话会话表
-- ============================================
CREATE TABLE IF NOT EXISTS `conversations` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id` BIGINT       DEFAULT NULL            COMMENT '关联应用ID',
    `user_id`        BIGINT       NOT NULL                COMMENT '对话用户ID',
    `title`          VARCHAR(200) DEFAULT NULL            COMMENT '对话标题',
    `model`          VARCHAR(100) DEFAULT NULL            COMMENT '使用的模型',
    `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0-删除, 1-活跃',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

-- ============================================
-- 消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `messages` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT      NOT NULL                COMMENT '所属对话ID',
    `role`            VARCHAR(20) NOT NULL                COMMENT '角色: USER/AI/SYSTEM',
    `content`         MEDIUMTEXT  NOT NULL                COMMENT '消息内容',
    `token_count`     INT         DEFAULT NULL            COMMENT 'Token消耗数',
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
