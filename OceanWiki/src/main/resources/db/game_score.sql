-- 文件说明：这个 SQL 脚本负责game_score相关的数据库表和初始数据。
-- 小游戏成绩表脚本
-- 使用方式：在 MySQL Workbench 中打开本文件，确认数据库名是 oceanwiki 后执行

USE `oceanwiki`;

CREATE TABLE IF NOT EXISTS `game_score` (
  `id` BIGINT NOT NULL COMMENT '成绩ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `game_code` VARCHAR(50) NOT NULL COMMENT '游戏编码',
  `score` INT NOT NULL DEFAULT 0 COMMENT '正确次数或游戏得分',
  `total_press_count` INT NOT NULL DEFAULT 0 COMMENT '总按键次数',
  `wrong_count` INT NOT NULL DEFAULT 0 COMMENT '错误次数',
  `accuracy` INT NOT NULL DEFAULT 100 COMMENT '正确率',
  `max_combo` INT NOT NULL DEFAULT 0 COMMENT '最高连击数',
  `reason` VARCHAR(50) DEFAULT NULL COMMENT '结束原因',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_game_score_game_rank` (`game_code`, `score`, `accuracy`, `max_combo`),
  KEY `idx_game_score_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小游戏成绩表';

INSERT INTO `sys_permission` (`code`, `name`, `module`, `path`, `sort`) VALUES
('game:play', '使用小游戏', '小游戏', '/piano-tiles', 8)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `module` = VALUES(`module`),
  `path` = VALUES(`path`),
  `sort` = VALUES(`sort`);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` = 'game:play'
WHERE r.`code` IN ('SUPER_ADMIN', 'CONTENT_ADMIN', 'NORMAL_USER');

