-- 文件说明：给内容审核员补齐评论审核和反馈审核权限。
-- 使用方式：复制本文件内容到 MySQL Workbench 执行。
-- 执行后，CONTENT_REVIEWER 可以进入电子书审核、评论审核、反馈审核页面。

USE `oceanwiki`;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- 确保内容审核员角色说明和当前系统职责一致。
UPDATE `sys_role`
SET `name` = '内容审核员',
    `description` = '负责审核电子书、评论和反馈，不负责日常编辑维护'
WHERE `code` = 'CONTENT_REVIEWER';

-- 确保评论审核和反馈审核对应的权限存在。
INSERT INTO `sys_permission` (`id`, `code`, `name`, `module`, `path`, `sort`)
SELECT UUID_SHORT(), 'comment:manage', '管理评论', '系统管理', '/admin/comment', 10
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `code` = 'comment:manage'
);

INSERT INTO `sys_permission` (`id`, `code`, `name`, `module`, `path`, `sort`)
SELECT UUID_SHORT(), 'feedback:manage', '管理反馈', '系统管理', '/admin/feedback', 11
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `code` = 'feedback:manage'
);

-- 给内容审核员补齐审核入口权限。
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
