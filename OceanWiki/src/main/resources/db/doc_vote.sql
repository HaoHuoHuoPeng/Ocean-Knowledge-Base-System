-- 文件说明：这个 SQL 脚本负责doc_vote相关的数据库表和初始数据。
-- 文档点赞记录表脚本
-- 使用方式：在 MySQL Workbench 中打开本文件，确认数据库名是 oceanwiki 后执行

USE `oceanwiki`;

CREATE TABLE IF NOT EXISTS `doc_vote` (
  `id` BIGINT NOT NULL COMMENT '点赞记录ID',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `create_time` DATETIME NOT NULL COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_vote_doc_user` (`doc_id`, `user_id`),
  KEY `idx_doc_vote_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档点赞记录表';

-- 如果以前已经有点赞数，但没有用户点赞记录，这里不强行清空旧点赞数
-- 后续新的点赞和取消点赞都会按用户记录来维护 doc.vote_count

