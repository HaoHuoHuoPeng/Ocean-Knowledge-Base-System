-- 文件说明：这个 SQL 脚本负责新增用户收藏分组表，并给已有用户补默认分组。
-- 使用方式：在 MySQL Workbench 中选择 oceanwiki 数据库后执行本文件。

USE `oceanwiki`;

CREATE TABLE IF NOT EXISTS `user_favorite_folder` (
  `id` BIGINT NOT NULL COMMENT '收藏分组ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分组名称',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite_folder` (`user_id`, `name`),
  KEY `idx_user_favorite_folder_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏分组表';

INSERT INTO `user_favorite_folder` (`id`, `user_id`, `name`, `create_time`)
SELECT
  CAST((UNIX_TIMESTAMP(NOW(3)) * 1000 + u.`id`) AS UNSIGNED),
  u.`id`,
  '默认分组',
  NOW()
FROM `user` u
WHERE NOT EXISTS (
  SELECT 1
  FROM `user_favorite_folder` f
  WHERE f.`user_id` = u.`id`
    AND f.`name` = '默认分组'
);
