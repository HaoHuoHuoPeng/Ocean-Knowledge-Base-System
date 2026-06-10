-- 文件说明：这个 SQL 脚本负责content_upgrade相关的数据库表和初始数据。
-- 内容功能增量脚本
-- 使用方式：已有数据库执行本文件，给文档补发布状态字段

USE `oceanwiki`;

SET @doc_status_column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'doc'
      AND COLUMN_NAME = 'status'
);

SET @add_doc_status_sql = IF(
    @doc_status_column_count = 0,
    'ALTER TABLE `doc` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''published'' COMMENT ''发布状态：draft草稿，published已发布''',
    'SELECT ''doc.status 字段已存在，跳过添加'' AS message'
);

PREPARE add_doc_status_stmt FROM @add_doc_status_sql;
EXECUTE add_doc_status_stmt;
DEALLOCATE PREPARE add_doc_status_stmt;

UPDATE `doc`
SET `status` = 'published'
WHERE `id` > 0 AND (`status` IS NULL OR `status` = '');

UPDATE ebook e
LEFT JOIN (
    SELECT
        ebook_id,
        COUNT(1) AS doc_count,
        IFNULL(SUM(view_count), 0) AS view_count,
        IFNULL(SUM(vote_count), 0) AS vote_count
    FROM doc
    WHERE status = 'published'
    GROUP BY ebook_id
) d ON e.id = d.ebook_id
SET
    e.doc_count = IFNULL(d.doc_count, 0),
    e.view_count = IFNULL(d.view_count, 0),
    e.vote_count = IFNULL(d.vote_count, 0)
WHERE e.id > 0;

