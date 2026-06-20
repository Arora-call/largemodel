-- ============================================
-- CodeForge 数据库迁移 V1
-- 为现有 conversations 表添加 type 列
-- 执行条件：仅当 type 列不存在时执行
-- ============================================

-- 添加 type 列（如果不存在）
ALTER TABLE `conversations`
    ADD COLUMN IF NOT EXISTS `type` VARCHAR(20) NOT NULL DEFAULT 'NATIVE'
    COMMENT '对话类型: NATIVE(代码生成) / ENGINEERING(工程项目)'
    AFTER `application_id`;

-- 添加 type 列索引
ALTER TABLE `conversations`
    ADD INDEX IF NOT EXISTS `idx_type` (`type`);

-- 将已有的工程对话标记为 ENGINEERING（根据关联的 application 类型推断）
-- 如果 application_id 对应的 application.type = 'ENGINEERING'，则设置 conversation.type = 'ENGINEERING'
UPDATE `conversations` c
    INNER JOIN `applications` a ON c.application_id = a.id
SET c.type = 'ENGINEERING'
WHERE a.type = 'ENGINEERING' AND c.type = 'NATIVE';
