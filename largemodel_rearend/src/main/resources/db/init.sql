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
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `username`      VARCHAR(50)  NOT NULL                  COMMENT '用户名',
    `password`      VARCHAR(200) NOT NULL                  COMMENT '密码（BCrypt加密）',
    `nickname`      VARCHAR(100) DEFAULT NULL              COMMENT '昵称',
    `email`         VARCHAR(150) DEFAULT NULL              COMMENT '邮箱',
    `phone`         VARCHAR(20)  DEFAULT NULL              COMMENT '手机号',
    `avatar`        VARCHAR(500) DEFAULT NULL              COMMENT '头像URL',
    `role`          VARCHAR(20)  NOT NULL DEFAULT 'USER'   COMMENT '角色: USER/ADMIN（单值，非RBAC）',
    `status`        INT          NOT NULL DEFAULT 1        COMMENT '状态: 0-禁用, 1-启用',
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '逻辑删除标志',
    `last_login_at` DATETIME     DEFAULT NULL              COMMENT '最后登录时间',
    `created_by`    VARCHAR(50)  DEFAULT NULL              COMMENT '创建人',
    `updated_by`    VARCHAR(50)  DEFAULT NULL              COMMENT '更新人',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 应用表
-- ============================================
CREATE TABLE IF NOT EXISTS `applications` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(200) NOT NULL               COMMENT '应用名称',
    `description` TEXT         DEFAULT NULL           COMMENT '应用描述',
    `type`        VARCHAR(50)  NOT NULL DEFAULT 'NATIVE' COMMENT '类型: NATIVE/ENGINEERING',
    `language`    VARCHAR(50)  DEFAULT NULL           COMMENT '编程语言: java/vue/python/html 等',
    `user_id`     BIGINT       NOT NULL               COMMENT '创建者用户ID',
    `status`      TINYINT      NOT NULL DEFAULT 1     COMMENT '状态: 0-删除, 1-草稿, 2-已生成',
    `source_code` MEDIUMTEXT   DEFAULT NULL           COMMENT '源代码',
    `config_json` JSON         DEFAULT NULL           COMMENT '配置信息(依赖/结构等)',
    `cover_image` VARCHAR(500) DEFAULT NULL           COMMENT '封面图URL',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

-- ============================================
-- 对话会话表
-- ============================================
CREATE TABLE IF NOT EXISTS `conversations` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id` BIGINT       DEFAULT NULL            COMMENT '关联应用ID',
    `user_id`        BIGINT       NOT NULL                COMMENT '对话用户ID',
    `title`          VARCHAR(200) DEFAULT NULL            COMMENT '对话标题（自动取首条消息前30字）',
    `model`          VARCHAR(100) DEFAULT NULL            COMMENT '使用的模型',
    `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 0-删除, 1-活跃',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_status` (`status`)
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

-- ============================================
-- 说明
--
-- 管理员账号由 DataInitializer 启动时自动创建
-- 默认账号: admin / admin123（BCrypt加密由Java生成）
--
-- 角色管理当前走 User.role 单值枚举字段（USER/ADMIN），
-- 无需额外的 roles/user_roles RBAC 关联表
-- ============================================
