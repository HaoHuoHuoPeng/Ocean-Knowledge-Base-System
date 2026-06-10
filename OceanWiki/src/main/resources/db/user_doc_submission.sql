-- 文件说明：这个脚本用于支持普通用户投稿文档。
-- 普通用户投稿后，系统会记录投稿人 id，便于查询“我的投稿”和发送审核通知。
-- 如果你之前已经执行过其中一部分，这个脚本会自动跳过已存在的字段和索引。

SET @has_review_remark = (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'doc'
      AND COLUMN_NAME = 'review_remark'
);

SET @sql = IF(
    @has_review_remark = 0,
    'ALTER TABLE `doc` ADD COLUMN `review_remark` VARCHAR(300) DEFAULT NULL COMMENT ''审核或下架原因'' AFTER `status`',
    'SELECT ''doc.review_remark 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_create_user_id = (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'doc'
      AND COLUMN_NAME = 'create_user_id'
);

SET @sql = IF(
    @has_create_user_id = 0,
    'ALTER TABLE `doc` ADD COLUMN `create_user_id` BIGINT NULL COMMENT ''创建人ID，普通用户投稿时记录投稿人'' AFTER `review_remark`',
    'SELECT ''doc.create_user_id 字段已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_create_user_index = (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'doc'
      AND INDEX_NAME = 'idx_doc_create_user_id'
);

SET @sql = IF(
    @has_create_user_index = 0,
    'CREATE INDEX `idx_doc_create_user_id` ON `doc` (`create_user_id`)',
    'SELECT ''idx_doc_create_user_id 索引已存在，跳过添加'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
