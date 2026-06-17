-- ============================================
-- 大模型代码应用生成平台 - 建库建表脚本
-- 使用 JPA ddl-auto=update 可自动建表，
-- 此脚本供手动初始化使用。
-- ============================================

CREATE DATABASE IF NOT EXISTS `largemodel`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `largemodel`;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `username`      VARCHAR(50)  NOT NULL                  COMMENT '用户名',
    `password`      VARCHAR(200) NOT NULL                  COMMENT '密码(BCrypt加密)',
    `nickname`      VARCHAR(100) DEFAULT NULL              COMMENT '昵称',
    `email`         VARCHAR(150) DEFAULT NULL              COMMENT '邮箱',
    `phone`         VARCHAR(20)  DEFAULT NULL              COMMENT '手机号',
    `avatar`        VARCHAR(500) DEFAULT NULL              COMMENT '头像URL',
    `role`          VARCHAR(20)  NOT NULL DEFAULT 'USER'   COMMENT '角色: USER/ADMIN',
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

-- 角色表
CREATE TABLE IF NOT EXISTS `roles` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(50)  NOT NULL                 COMMENT '角色名称(USER/ADMIN)',
    `description` VARCHAR(200) DEFAULT NULL             COMMENT '角色描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
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
