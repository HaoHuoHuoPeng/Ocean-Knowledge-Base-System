-- 文件说明：这个脚本用于给已有文档补齐创建人，避免历史文档在页面上看不到投稿人。
-- 执行前请确认 user 表里存在 admin 和 scientific_researcher 这两个账号。

-- 除“海藻食物营养成分”外，其余历史文档都标记为管理员创建。
UPDATE doc
SET create_user_id = (SELECT id FROM user WHERE login_name = 'admin' LIMIT 1)
WHERE name <> '海藻食物营养成分';

-- “海藻食物营养成分”标记为科研人员投稿。
UPDATE doc
SET create_user_id = (SELECT id FROM user WHERE login_name = 'scientific_researcher' LIMIT 1)
WHERE name = '海藻食物营养成分';
