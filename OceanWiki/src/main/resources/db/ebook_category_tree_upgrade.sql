-- 文件说明：这个 SQL 脚本负责把电子书分类从固定二级分类升级为任意层级分类。
-- 使用方式：在 MySQL Workbench 中打开本文件，确认数据库名是 oceanwiki 后执行。

USE `oceanwiki`;

SET @ebook_category_column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ebook'
      AND COLUMN_NAME = 'category_id'
);

SET @add_ebook_category_sql = IF(
    @ebook_category_column_count = 0,
    'ALTER TABLE `ebook` ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT ''所属分类ID，支持任意层级分类'' AFTER `name`',
    'SELECT ''ebook.category_id 字段已存在，跳过添加'' AS message'
);

PREPARE add_ebook_category_stmt FROM @add_ebook_category_sql;
EXECUTE add_ebook_category_stmt;
DEALLOCATE PREPARE add_ebook_category_stmt;

-- 你的 MySQL Workbench 之前开了安全更新模式，不临时关闭的话，这条批量回填可能会报 1175。
SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- 旧数据没有 category_id 时，优先使用二级分类；没有二级分类再使用一级分类。
UPDATE `ebook`
SET `category_id` = IFNULL(`category2_id`, `category1_id`)
WHERE `category_id` IS NULL;

-- 回填结束后恢复你原来的安全更新设置。
SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
