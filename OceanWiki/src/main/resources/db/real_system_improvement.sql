-- 文件说明：这个 SQL 脚本负责补充真实系统使用时需要的字段。
-- 使用方式：在 MySQL Workbench 中打开本文件，确认数据库名是 oceanwiki 后执行。

USE `oceanwiki`;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

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

-- 文档审核或下架原因。
SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'doc'
      AND COLUMN_NAME = 'review_remark'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `doc` ADD COLUMN `review_remark` VARCHAR(300) DEFAULT NULL COMMENT ''审核或下架原因'' AFTER `status`',
    'SELECT ''doc.review_remark 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 阅读进度，0 到 100。
SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reading_history'
      AND COLUMN_NAME = 'progress'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `reading_history` ADD COLUMN `progress` INT DEFAULT 0 COMMENT ''阅读进度百分比'' AFTER `read_time`',
    'SELECT ''reading_history.progress 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 操作日志修改前后的关键数据。
SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'operation_log'
      AND COLUMN_NAME = 'before_data'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `operation_log` ADD COLUMN `before_data` TEXT DEFAULT NULL COMMENT ''修改前数据'' AFTER `content`',
    'SELECT ''operation_log.before_data 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'operation_log'
      AND COLUMN_NAME = 'after_data'
);
SET @sql = IF(
    @column_count = 0,
    'ALTER TABLE `operation_log` ADD COLUMN `after_data` TEXT DEFAULT NULL COMMENT ''修改后数据'' AFTER `before_data`',
    'SELECT ''operation_log.after_data 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 给真实使用中经常查询的字段补索引。
SET @index_count = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reading_history'
      AND INDEX_NAME = 'idx_reading_history_user_time'
);
SET @sql = IF(
    @index_count = 0,
    'ALTER TABLE `reading_history` ADD INDEX `idx_reading_history_user_time` (`user_id`, `read_time`)',
    'SELECT ''reading_history.idx_reading_history_user_time 索引已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_count = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_comment'
      AND INDEX_NAME = 'idx_user_comment_target_status'
);
SET @sql = IF(
    @index_count = 0,
    'ALTER TABLE `user_comment` ADD INDEX `idx_user_comment_target_status` (`target_type`, `target_id`, `status`)',
    'SELECT ''user_comment.idx_user_comment_target_status 索引已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
