-- 文件说明：这个脚本负责给内容审核员补上“文档审核”权限。
-- 使用方式：复制本文件内容到 MySQL Workbench 执行，执行后需要重新登录前端账号，让最新权限重新写入浏览器缓存。

USE `oceanwiki`;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- 确保内容审核员角色说明包含文档投稿审核职责。
UPDATE `sys_role`
SET `name` = '内容审核员',
    `description` = '负责审核电子书、文档投稿、评论和反馈，不负责日常编辑维护'
WHERE `code` = 'CONTENT_REVIEWER';

-- 权限 ID 使用固定的 19 位大整数，避免依赖数据库自增。
SET @doc_review_permission_id = 1900000000000000201;

-- 如果系统里还没有 doc:review 权限，就新增一个。
INSERT INTO `sys_permission` (`id`, `code`, `name`, `module`, `path`, `sort`)
SELECT @doc_review_permission_id, 'doc:review', '审核文档投稿', '文档', '/admin/doc', 6
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `code` = 'doc:review'
);

-- 如果权限已经存在，就直接取已有权限 ID，避免重复插入。
SET @doc_review_permission_id = (
    SELECT `id` FROM `sys_permission` WHERE `code` = 'doc:review' LIMIT 1
);

-- 给内容审核员绑定文档审核权限。
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT 1900000000000000202, r.`id`, @doc_review_permission_id
FROM `sys_role` r
WHERE r.`code` = 'CONTENT_REVIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM `sys_role_permission` rp
      WHERE rp.`role_id` = r.`id`
        AND rp.`permission_id` = @doc_review_permission_id
  );

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
