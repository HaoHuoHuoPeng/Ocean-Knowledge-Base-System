-- 清理电子书历史残留的下架原因
-- 正常情况下，只有“已下架”的电子书才应该保留下架原因
USE `oceanwiki`;

UPDATE `ebook`
SET `offline_reason` = NULL
WHERE `id` > 0
  AND `status` <> 'offline'
  AND `offline_reason` IS NOT NULL;
