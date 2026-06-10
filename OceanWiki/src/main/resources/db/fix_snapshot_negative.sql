-- 文件说明：这个脚本用来修复统计快照里的负数增量，避免首页出现“今日阅读/今日点赞”为负数。

USE oceanwiki;

-- 删除文档、重建数据或重新初始化后，累计阅读/点赞可能比前一天快照小。
-- 今日新增不能是负数，所以把已有负数修正为 0。
UPDATE `ebook_snapshot`
SET
    `view_increase` = GREATEST(`view_increase`, 0),
    `vote_increase` = GREATEST(`vote_increase`, 0)
WHERE `id` > 0
  AND (`view_increase` < 0 OR `vote_increase` < 0);
