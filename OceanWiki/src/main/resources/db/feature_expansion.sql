-- 文件说明：这个 SQL 脚本负责本次系统完善扩展需要的数据库变更。
-- 使用方式：在 MySQL Workbench 中打开本文件，确认数据库名是 oceanwiki 后执行。

USE `oceanwiki`;

-- 电子书发布状态：draft 草稿，pending 待审核，published 已发布，offline 已下架
-- 这里先判断字段是否存在，避免你重复执行脚本时报“字段已存在”
SET @ebook_status_column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ebook'
      AND COLUMN_NAME = 'status'
);

SET @add_ebook_status_sql = IF(
    @ebook_status_column_count = 0,
    'ALTER TABLE `ebook` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''published'' COMMENT ''发布状态'' AFTER `cover`',
    'SELECT ''ebook.status 字段已存在，跳过添加'' AS message'
);

PREPARE add_ebook_status_stmt FROM @add_ebook_status_sql;
EXECUTE add_ebook_status_stmt;
DEALLOCATE PREPARE add_ebook_status_stmt;

-- 用户收藏分组名称，方便个人中心做“我的书架”
-- 这里同样先判断字段是否存在，避免重复执行脚本失败
SET @favorite_folder_column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_favorite'
      AND COLUMN_NAME = 'folder_name'
);

SET @add_favorite_folder_sql = IF(
    @favorite_folder_column_count = 0,
    'ALTER TABLE `user_favorite` ADD COLUMN `folder_name` VARCHAR(50) NOT NULL DEFAULT ''默认分组'' COMMENT ''收藏分组'' AFTER `create_time`',
    'SELECT ''user_favorite.folder_name 字段已存在，跳过添加'' AS message'
);

PREPARE add_favorite_folder_stmt FROM @add_favorite_folder_sql;
EXECUTE add_favorite_folder_stmt;
DEALLOCATE PREPARE add_favorite_folder_stmt;

-- 文档版本记录表，用来保存每次修改前的旧内容
CREATE TABLE IF NOT EXISTS `doc_version` (
  `id` BIGINT NOT NULL COMMENT '版本ID',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `version_no` INT NOT NULL COMMENT '版本号',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '文档标题',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '文档状态',
  `content` MEDIUMTEXT COMMENT '文档正文',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建用户ID',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_version_doc` (`doc_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档历史版本表';

