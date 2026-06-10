-- 文件说明：这个脚本用于把电子书文档数量改成只统计真正的内容文档。
-- 父文档如果只是目录，例如“虎鲸”“海豚”，并且下面还有子文档，就不会计入电子书 doc_count。
-- 真正计入数量的是没有子文档的叶子文档，例如“虎鲸简介”“虎鲸习性”。

UPDATE ebook e
LEFT JOIN (
    SELECT
        d.ebook_id,
        COUNT(1) AS doc_count,
        IFNULL(SUM(d.view_count), 0) AS view_count,
        IFNULL(SUM(d.vote_count), 0) AS vote_count
    FROM doc d
    WHERE d.status = 'published'
      AND NOT EXISTS (
          SELECT 1
          FROM doc child
          WHERE child.parent = d.id
      )
    GROUP BY d.ebook_id
) stat ON e.id = stat.ebook_id
SET
    e.doc_count = IFNULL(stat.doc_count, 0),
    e.view_count = IFNULL(stat.view_count, 0),
    e.vote_count = IFNULL(stat.vote_count, 0)
WHERE e.id > 0;
