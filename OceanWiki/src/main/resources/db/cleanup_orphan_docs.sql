-- 文件说明：这个脚本负责清理父文档已经不存在的残留子文档。
-- 使用场景：之前删除一级文档时没有连带删除子文档，导致阅读目录里可能残留二级、三级文档。
-- 使用方式：复制本文件内容到 MySQL Workbench 执行。

USE `oceanwiki`;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_orphan_doc;

-- 使用递归查询一次性找出所有残留文档。
-- 不能一边读取临时表一边插入同一个临时表，否则 MySQL 会报 Can't reopen table。
CREATE TEMPORARY TABLE tmp_orphan_doc AS
WITH RECURSIVE orphan_tree AS (
    -- 第一层：父文档已经不存在的文档。
    SELECT d.id
    FROM doc d
    LEFT JOIN doc p ON d.parent = p.id
    WHERE d.parent IS NOT NULL
      AND d.parent <> 0
      AND p.id IS NULL

    UNION ALL

    -- 后续层：孤儿文档下面的子文档也一起清理。
    SELECT child.id
    FROM doc child
    JOIN orphan_tree parent_orphan ON child.parent = parent_orphan.id
)
SELECT DISTINCT id
FROM orphan_tree;

-- 删除这些残留文档的关联数据。
DELETE c
FROM content c
JOIN tmp_orphan_doc t ON c.id = t.id;

DELETE v
FROM doc_version v
JOIN tmp_orphan_doc t ON v.doc_id = t.id;

DELETE vote
FROM doc_vote vote
JOIN tmp_orphan_doc t ON vote.doc_id = t.id;

DELETE h
FROM reading_history h
JOIN tmp_orphan_doc t ON h.doc_id = t.id;

DELETE f
FROM user_favorite f
JOIN tmp_orphan_doc t ON f.target_type = 'doc' AND f.target_id = t.id;

DELETE cmt
FROM user_comment cmt
JOIN tmp_orphan_doc t ON cmt.target_type = 'doc' AND cmt.target_id = t.id;

DELETE d
FROM doc d
JOIN tmp_orphan_doc t ON d.id = t.id;

DROP TEMPORARY TABLE IF EXISTS tmp_orphan_doc;

-- 重新统计电子书的文档数、阅读数和点赞数。
UPDATE ebook e
LEFT JOIN (
    SELECT
        ebook_id,
        COUNT(1) AS doc_count,
        IFNULL(SUM(view_count), 0) AS view_count,
        IFNULL(SUM(vote_count), 0) AS vote_count
    FROM doc
    WHERE status = 'published'
      AND NOT EXISTS (
          SELECT 1 FROM doc child WHERE child.parent = doc.id
      )
    GROUP BY ebook_id
) d ON e.id = d.ebook_id
SET
    e.doc_count = IFNULL(d.doc_count, 0),
    e.view_count = IFNULL(d.view_count, 0),
    e.vote_count = IFNULL(d.vote_count, 0);

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
