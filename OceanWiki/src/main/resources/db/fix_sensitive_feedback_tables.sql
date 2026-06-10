-- 文件说明：这个 SQL 脚本负责fix_sensitive_feedback_tables相关的数据库表和初始数据。
-- 修复敏感词和反馈回复缺表问题
-- 使用方式：在 MySQL Workbench 里连接 MySQL 后，直接整段执行本文件
-- 这个脚本只会补充缺少的表、默认敏感词和权限，不会删除你的现有数据

CREATE DATABASE IF NOT EXISTS `oceanwiki`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE `oceanwiki`;

CREATE TABLE IF NOT EXISTS `sensitive_word` (
  `id` BIGINT NOT NULL COMMENT '敏感词ID',
  `word` VARCHAR(50) NOT NULL COMMENT '敏感词',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

CREATE TABLE IF NOT EXISTS `feedback_reply` (
  `id` BIGINT NOT NULL COMMENT '回复ID',
  `feedback_id` BIGINT NOT NULL COMMENT '反馈ID',
  `user_id` BIGINT NOT NULL COMMENT '回复人ID',
  `reply_type` VARCHAR(20) NOT NULL COMMENT '回复人类型：admin管理员，user用户',
  `content` VARCHAR(1000) NOT NULL COMMENT '回复内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_reply_feedback` (`feedback_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈回复表';

INSERT INTO `sensitive_word` (`word`, `enabled`, `remark`) VALUES
('违法', 1, '默认敏感词'),
('赌博', 1, '默认敏感词'),
('诈骗', 1, '默认敏感词'),
('色情', 1, '默认敏感词'),
('暴力', 1, '默认敏感词'),
('辱骂', 1, '默认敏感词'),
('广告', 1, '默认敏感词')
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_permission` (`code`, `name`, `module`, `path`, `sort`) VALUES
('comment:manage', '管理评论', '系统管理', '/admin/comment', 9),
('feedback:manage', '管理反馈', '系统管理', '/admin/feedback', 10),
('operation:log', '查看操作日志', '系统管理', '/admin/log', 11),
('sensitive:manage', '管理敏感词', '系统管理', '/admin/sensitive-word', 12)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `module` = VALUES(`module`),
  `path` = VALUES(`path`),
  `sort` = VALUES(`sort`);

-- 超级管理员角色默认拥有全部后台权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`;

-- 内容管理员可以管理评论、反馈和敏感词
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission`
WHERE `code` IN ('comment:manage', 'feedback:manage', 'sensitive:manage');

