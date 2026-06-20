-- ============================================
-- CodeForge 数据库迁移 V2
-- 新增知识库文档表
-- ============================================

CREATE TABLE IF NOT EXISTS `knowledge_documents` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`         VARCHAR(300) NOT NULL                COMMENT '文档标题',
    `doc_type`      VARCHAR(50)  NOT NULL                COMMENT '文档类型: pdf/markdown/txt/code',
    `file_name`     VARCHAR(500) DEFAULT NULL            COMMENT '原始文件名',
    `file_size`     BIGINT       DEFAULT NULL            COMMENT '文件大小(bytes)',
    `content`       MEDIUMTEXT   NOT NULL                COMMENT '文档原始内容',
    `summary`       VARCHAR(1000) DEFAULT NULL           COMMENT '摘要(前500字)',
    `collection`    VARCHAR(100) DEFAULT 'default'       COMMENT '所属知识库集合',
    `vector_status` VARCHAR(20)  DEFAULT 'pending'       COMMENT '向量化状态: pending/completed/failed',
    `user_id`       BIGINT       NOT NULL                COMMENT '上传用户ID',
    `status`        INT          NOT NULL DEFAULT 1      COMMENT '状态: 0-已删除, 1-正常',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_collection` (`collection`),
    KEY `idx_vector_status` (`vector_status`),
    FULLTEXT KEY `ft_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';
