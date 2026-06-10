-- 文件说明：这个 SQL 脚本负责创建收藏、历史、评论、反馈、通知和日志等用户功能表。
-- 普通用户功能增量脚本
-- 使用方式：在 MySQL Workbench 中连接 oceanwiki 数据库后，整段执行本文件
-- 本脚本会新增收藏、阅读历史、评论、反馈、通知、操作日志等表

CREATE DATABASE IF NOT EXISTS `oceanwiki`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE `oceanwiki`;

CREATE TABLE IF NOT EXISTS `user_favorite` (
  `id` BIGINT NOT NULL COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '收藏类型：ebook电子书，doc文档',
  `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`, `target_type`, `target_id`),
  KEY `idx_user_favorite_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

CREATE TABLE IF NOT EXISTS `reading_history` (
  `id` BIGINT NOT NULL COMMENT '阅读历史ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `ebook_id` BIGINT NOT NULL COMMENT '电子书ID',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reading_history` (`user_id`, `doc_id`),
  KEY `idx_reading_history_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读历史表';

CREATE TABLE IF NOT EXISTS `user_comment` (
  `id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '评论目标类型：ebook电子书，doc文档',
  `target_id` BIGINT NOT NULL COMMENT '评论目标ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID，0表示一级评论',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '状态：published已发布，pending待审核，rejected已驳回，deleted已删除',
  `sensitive_hit` VARCHAR(255) DEFAULT NULL COMMENT '命中的敏感词',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_user_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_comment_target` (`target_type`, `target_id`),
  KEY `idx_user_comment_user` (`user_id`),
  KEY `idx_user_comment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户评论表';

CREATE TABLE IF NOT EXISTS `user_feedback` (
  `id` BIGINT NOT NULL COMMENT '反馈ID',
  `user_id` BIGINT NOT NULL COMMENT '反馈用户ID',
  `target_type` VARCHAR(20) DEFAULT NULL COMMENT '目标类型：ebook电子书，doc文档',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `type` VARCHAR(20) NOT NULL DEFAULT 'suggestion' COMMENT '类型：suggestion建议，correction纠错',
  `title` VARCHAR(100) NOT NULL COMMENT '标题',
  `content` VARCHAR(1000) NOT NULL COMMENT '内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'open' COMMENT '状态：open待处理，handled已处理，rejected已驳回',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_user_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handle_remark` VARCHAR(255) DEFAULT NULL COMMENT '处理备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_feedback_user` (`user_id`),
  KEY `idx_user_feedback_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

CREATE TABLE IF NOT EXISTS `user_notice` (
  `id` BIGINT NOT NULL COMMENT '通知ID',
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
  `read_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_notice_user` (`user_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `module` VARCHAR(50) NOT NULL COMMENT '模块',
  `action` VARCHAR(50) NOT NULL COMMENT '动作',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '操作内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user` (`user_id`),
  KEY `idx_operation_log_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

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

-- 补充后台权限
-- 超级管理员会拥有全部权限，内容管理员可以处理评论和反馈
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

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission`
WHERE `code` IN ('comment:manage', 'feedback:manage', 'sensitive:manage');

