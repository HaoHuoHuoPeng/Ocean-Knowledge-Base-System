-- 文件说明：这个 SQL 脚本负责补齐电子书审核流程需要的字段、角色和权限。
-- 使用方式：在 MySQL Workbench 中连接 oceanwiki 数据库后，直接执行本文件。
-- 执行后需要重新登录前端，因为登录信息里的权限是在登录时读取并缓存到浏览器里的。

USE `oceanwiki`;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- 电子书发布状态。
SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ebook'
      AND COLUMN_NAME = 'status'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `ebook` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''draft'' COMMENT ''发布状态：draft草稿，pending待审核，published已发布，rejected已驳回，offline已下架'' AFTER `cover`',
    'SELECT ''ebook.status 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 电子书下架原因。
SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ebook'
      AND COLUMN_NAME = 'offline_reason'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `ebook` ADD COLUMN `offline_reason` VARCHAR(300) DEFAULT NULL COMMENT ''下架原因'' AFTER `status`',
    'SELECT ''ebook.offline_reason 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 电子书审核备注。
SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ebook'
      AND COLUMN_NAME = 'review_remark'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `ebook` ADD COLUMN `review_remark` VARCHAR(300) DEFAULT NULL COMMENT ''审核备注'' AFTER `offline_reason`',
    'SELECT ''ebook.review_remark 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老数据如果没有状态，默认先作为草稿处理，由内容编辑员重新提交审核。
UPDATE `ebook`
SET `status` = 'draft'
WHERE `id` > 0
  AND (`status` IS NULL OR `status` = '');

-- 把原来的内容管理员展示名改成内容编辑员，只负责编辑和提交审核。
UPDATE `sys_role`
SET `name` = '内容编辑员',
    `description` = '负责电子书、分类和文档编辑，可以提交电子书审核，不负责审核发布',
    `sort` = 2
WHERE `id` > 0
  AND `code` = 'CONTENT_ADMIN';

-- 新增内容审核员角色。ID 使用 UUID_SHORT，避免依赖数据库自增。
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `built_in`, `sort`)
SELECT UUID_SHORT(), 'CONTENT_REVIEWER', '内容审核员', '负责审核电子书是否可以发布，不负责日常编辑维护', 1, 3
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `code` = 'CONTENT_REVIEWER'
);

UPDATE `sys_role`
SET `name` = '内容审核员',
    `description` = '负责审核电子书是否可以发布，不负责日常编辑维护',
    `built_in` = 1,
    `sort` = 3
WHERE `id` > 0
  AND `code` = 'CONTENT_REVIEWER';

UPDATE `sys_role`
SET `sort` = 4
WHERE `id` > 0
  AND `code` = 'NORMAL_USER';

-- 新增电子书审核权限。
INSERT INTO `sys_permission` (`id`, `code`, `name`, `module`, `path`, `sort`)
SELECT UUID_SHORT(), 'ebook:review', '审核电子书', '电子书', '/admin/ebook', 3
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `code` = 'ebook:review'
);

UPDATE `sys_permission`
SET `name` = '审核电子书',
    `module` = '电子书',
    `path` = '/admin/ebook',
    `sort` = 3
WHERE `id` > 0
  AND `code` = 'ebook:review';

-- 科研人员不具备电子书审核权限，避免自己提交自己审核。
DELETE FROM `sys_role_permission`
WHERE `id` > 0
  AND `role_id` = (SELECT `id` FROM `sys_role` WHERE `code` = 'CONTENT_ADMIN' LIMIT 1)
  AND `permission_id` = (SELECT `id` FROM `sys_permission` WHERE `code` = 'ebook:review' LIMIT 1);

-- 超级管理员拥有电子书审核权限。
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID_SHORT(), r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` = 'ebook:review'
WHERE r.`code` = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_permission` rp
      WHERE rp.`role_id` = r.`id`
        AND rp.`permission_id` = p.`id`
  );

-- 内容审核员拥有查看电子书、审核电子书、审核评论、审核反馈和使用小游戏的权限。
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID_SHORT(), r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN ('ebook:view', 'ebook:review', 'comment:manage', 'feedback:manage', 'game:play')
WHERE r.`code` = 'CONTENT_REVIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_permission` rp
      WHERE rp.`role_id` = r.`id`
        AND rp.`permission_id` = p.`id`
  );

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
