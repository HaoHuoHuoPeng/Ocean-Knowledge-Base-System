-- 文件说明：这个 SQL 脚本负责rbac相关的数据库表和初始数据。
-- 角色权限增量脚本
-- 使用方式：如果你已经有数据库，只想补角色权限功能，在 MySQL Workbench 中执行本文件

USE `oceanwiki`;

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL COMMENT '角色ID',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
  `built_in` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置角色，1表示内置',
  `sort` INT NOT NULL DEFAULT 99 COMMENT '排序值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` BIGINT NOT NULL COMMENT '权限ID',
  `code` VARCHAR(80) NOT NULL COMMENT '权限编码',
  `name` VARCHAR(80) NOT NULL COMMENT '权限名称',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '所属模块',
  `path` VARCHAR(120) DEFAULT NULL COMMENT '前端页面路径',
  `sort` INT NOT NULL DEFAULT 99 COMMENT '排序值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL COMMENT '关联ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` BIGINT NOT NULL COMMENT '关联ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `built_in`, `sort`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '系统最高权限角色，只能分配给一个用户', 1, 1),
(2, 'CONTENT_ADMIN', '科研人员', '负责电子书、分类和文档编辑，可以提交电子书审核，不负责审核发布', 1, 2),
(4, 'CONTENT_REVIEWER', '内容审核员', '负责审核电子书是否可以发布，不负责日常编辑维护', 1, 3),
(3, 'NORMAL_USER', '普通用户', '注册后默认角色，可以阅读电子书、投稿文档和使用小游戏', 1, 4)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `built_in` = VALUES(`built_in`),
  `sort` = VALUES(`sort`);

INSERT INTO `sys_permission` (`id`, `code`, `name`, `module`, `path`, `sort`) VALUES
(1, 'ebook:view', '查看电子书', '电子书', '/', 1),
(2, 'ebook:manage', '管理电子书', '电子书', '/admin/ebook', 2),
(9, 'ebook:review', '审核电子书', '电子书', '/admin/ebook', 3),
(3, 'category:manage', '管理分类', '分类', '/admin/category', 4),
(4, 'doc:manage', '管理文档', '文档', '/admin/doc', 5),
(5, 'statistics:view', '查看统计', '统计', '/statistics', 6),
(6, 'user:manage', '管理用户', '系统管理', '/admin/user', 7),
(7, 'role:manage', '管理角色权限', '系统管理', '/admin/role', 8),
(8, 'game:play', '使用小游戏', '小游戏', '/piano-tiles', 9),
(10, 'comment:manage', '管理评论', '系统管理', '/admin/comment', 10),
(11, 'feedback:manage', '管理反馈', '系统管理', '/admin/feedback', 11)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `module` = VALUES(`module`),
  `path` = VALUES(`path`),
  `sort` = VALUES(`sort`);

DELETE FROM `sys_role_permission`
WHERE `id` > 0;

INSERT IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID_SHORT(), 1, `id` FROM `sys_permission`;

INSERT IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
(UUID_SHORT(), 2, 1),
(UUID_SHORT(), 2, 2),
(UUID_SHORT(), 2, 3),
(UUID_SHORT(), 2, 4),
(UUID_SHORT(), 2, 5),
(UUID_SHORT(), 2, 8),
(UUID_SHORT(), 4, 1),
(UUID_SHORT(), 4, 9),
(UUID_SHORT(), 4, 8),
(UUID_SHORT(), 4, 10),
(UUID_SHORT(), 4, 11),
(UUID_SHORT(), 3, 1),
(UUID_SHORT(), 3, 8);

-- 保证超级管理员只有 admin 一个
DELETE FROM `sys_user_role`
WHERE `id` > 0 AND `role_id` = 1 AND `user_id` <> 1;

INSERT IGNORE INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(UUID_SHORT(), 1, 1),
(UUID_SHORT(), 2, 3);

