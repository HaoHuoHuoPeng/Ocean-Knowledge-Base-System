-- 文件说明：这个 SQL 脚本负责初始化海洋知识库的基础表和示例数据。
-- 海洋知识库数据库初始化脚本
-- 使用方式：在 MySQL Workbench 中打开本文件，整段执行即可

CREATE DATABASE IF NOT EXISTS `oceanwiki`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE `oceanwiki`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ebook_snapshot`;
DROP TABLE IF EXISTS `content`;
DROP TABLE IF EXISTS `doc`;
DROP TABLE IF EXISTS `ebook`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `user`;

-- 清理之前可能创建过的非电子书扩展表
DROP TABLE IF EXISTS `ocean_observation`;
DROP TABLE IF EXISTS `ocean_ecosystem`;
DROP TABLE IF EXISTS `ocean_species`;

-- 分类表：用于保存海洋生物的分类树
CREATE TABLE `category` (
  `id` BIGINT NOT NULL COMMENT '分类ID',
  `parent` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示一级分类',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 电子书表：用于保存首页展示的海洋生物资料卡片
CREATE TABLE `ebook` (
  `id` BIGINT NOT NULL COMMENT '电子书ID',
  `name` VARCHAR(100) NOT NULL COMMENT '电子书名称',
  `category_id` BIGINT DEFAULT NULL COMMENT '所属分类ID，支持任意层级分类',
  `category1_id` BIGINT DEFAULT NULL COMMENT '一级分类ID',
  `category2_id` BIGINT DEFAULT NULL COMMENT '二级分类ID',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '简介',
  `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面图片地址',
  `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '发布状态：draft草稿，pending待审核，published已发布，rejected已驳回，offline已下架',
  `offline_reason` VARCHAR(300) DEFAULT NULL COMMENT '下架原因',
  `review_remark` VARCHAR(300) DEFAULT NULL COMMENT '审核备注',
  `doc_count` INT NOT NULL DEFAULT 0 COMMENT '文档数量',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '阅读数',
  `vote_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子书表';

-- 文档表：用于保存电子书下面的目录和文章标题
CREATE TABLE `doc` (
  `id` BIGINT NOT NULL COMMENT '文档ID',
  `ebook_id` BIGINT NOT NULL COMMENT '所属电子书ID',
  `parent` BIGINT NOT NULL DEFAULT 0 COMMENT '父文档ID，0表示顶级文档',
  `name` VARCHAR(100) NOT NULL COMMENT '文档名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '阅读数',
  `vote_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '发布状态：draft草稿，pending待审核，published已发布，rejected已驳回，offline已下架',
  `review_remark` VARCHAR(300) DEFAULT NULL COMMENT '审核或下架原因',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人ID，普通用户投稿时记录投稿人',
  PRIMARY KEY (`id`),
  KEY `idx_doc_ebook_id` (`ebook_id`),
  KEY `idx_doc_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 正文表：用于保存文档正文，id 和 doc.id 一一对应
CREATE TABLE `content` (
  `id` BIGINT NOT NULL COMMENT '文档ID，和doc表ID一致',
  `content` MEDIUMTEXT COMMENT '正文内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正文表';

-- 用户表：用于保存后台登录用户
CREATE TABLE `user` (
  `id` BIGINT NOT NULL COMMENT '用户ID',
  `login_name` VARCHAR(50) NOT NULL COMMENT '登录名',
  `name` VARCHAR(50) NOT NULL COMMENT '昵称',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_login_name` (`login_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表：用于保存超级管理员、内容管理员、普通用户等角色
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL COMMENT '角色ID',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
  `built_in` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置角色，1表示内置',
  `sort` INT NOT NULL DEFAULT 99 COMMENT '排序值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表：用于保存系统里可以分配给角色的能力
CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL COMMENT '权限ID',
  `code` VARCHAR(80) NOT NULL COMMENT '权限编码',
  `name` VARCHAR(80) NOT NULL COMMENT '权限名称',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '所属模块',
  `path` VARCHAR(120) DEFAULT NULL COMMENT '前端页面路径',
  `sort` INT NOT NULL DEFAULT 99 COMMENT '排序值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户角色关联表：用于保存某个用户拥有哪些角色
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL COMMENT '关联ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表：用于保存某个角色拥有哪些权限
CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL COMMENT '关联ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 电子书统计快照表：用于保存每天的阅读和点赞统计
CREATE TABLE `ebook_snapshot` (
  `id` BIGINT NOT NULL COMMENT '快照ID',
  `ebook_id` BIGINT NOT NULL COMMENT '电子书ID',
  `date` DATE NOT NULL COMMENT '快照日期',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '当天总阅读数',
  `vote_count` INT NOT NULL DEFAULT 0 COMMENT '当天总点赞数',
  `view_increase` INT NOT NULL DEFAULT 0 COMMENT '当天新增阅读数',
  `vote_increase` INT NOT NULL DEFAULT 0 COMMENT '当天新增点赞数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ebook_snapshot` (`ebook_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子书统计快照表';

-- 一级分类
INSERT INTO `category` (`id`, `parent`, `name`, `sort`) VALUES
(100, 0, '海洋动物', 1),
(200, 0, '海洋植物', 2),
(300, 0, '海洋微生物', 3);

-- 二级分类
INSERT INTO `category` (`id`, `parent`, `name`, `sort`) VALUES
(101, 100, '鲸豚类', 1),
(102, 100, '水母类', 2),
(103, 100, '鱼类', 3),
(201, 200, '藻类植物', 1),
(301, 300, '海洋细菌', 1);

-- 电子书数据：cover 字段使用你提供的 OSS 图片地址，初始化数据默认已发布，方便首页直接展示
INSERT INTO `ebook` (`id`, `name`, `category_id`, `category1_id`, `category2_id`, `description`, `cover`, `status`) VALUES
(1, '虎鲸', 101, 100, 101, '虎鲸是高度社会化的海洋哺乳动物，常以家族群体活动，具有很强的捕食能力。', 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/hujing.jpg', 'published'),
(2, '海藻', 201, 200, 201, '海藻是生活在海洋中的大型藻类，既是海洋生态系统的重要生产者，也能为多种生物提供栖息环境。', 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/haizao.jpg', 'published'),
(3, '水母', 102, 100, 102, '水母身体柔软透明，依靠伞状身体收缩运动，是海洋中常见的浮游动物。', 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/shuimu.jpg', 'published'),
(4, '海豚', 101, 100, 101, '海豚具有较高智力和复杂的声音交流能力，常成群活动，行动敏捷。', 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/haitun.jpg', 'published'),
(5, '抹香鲸', 101, 100, 101, '抹香鲸是深潜能力很强的大型齿鲸，主要捕食深海乌贼等动物。', 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/moxiangjing.jpg', 'published'),
(6, '虎鲸保护', 101, 100, 101, '本篇用于补充虎鲸相关保护知识，封面复用虎鲸图片地址。', 'http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/hujing.jpg', 'published');

-- 文档目录
INSERT INTO `doc` (`id`, `ebook_id`, `parent`, `name`, `sort`, `view_count`, `vote_count`, `status`) VALUES
(1, 1, 0, '虎鲸简介', 1, 0, 0, 'published'),
(2, 1, 0, '虎鲸习性', 2, 0, 0, 'published'),
(3, 2, 0, '海藻简介', 1, 0, 0, 'published'),
(4, 3, 0, '水母简介', 1, 0, 0, 'published'),
(5, 4, 0, '海豚简介', 1, 0, 0, 'published'),
(6, 5, 0, '抹香鲸简介', 1, 0, 0, 'published'),
(7, 6, 0, '虎鲸保护知识', 1, 0, 0, 'published');

-- 文档正文
INSERT INTO `content` (`id`, `content`) VALUES
(1, '<h2>虎鲸简介</h2><p>虎鲸又叫逆戟鲸，是海洋中极具代表性的顶级捕食者。它们通常以家族为单位活动，拥有清晰的分工和复杂的声音交流方式。</p>'),
(2, '<h2>虎鲸习性</h2><p>虎鲸会根据区域和族群形成不同的捕食策略，有的族群主要捕食鱼类，有的族群会捕食海豹、海狮等海洋哺乳动物。</p>'),
(3, '<h2>海藻简介</h2><p>海藻通过光合作用制造有机物，是许多海洋生物的食物来源，也能吸收二氧化碳并释放氧气。</p>'),
(4, '<h2>水母简介</h2><p>水母身体大部分由水组成，结构看似简单，但在海洋生态系统中有重要作用。部分水母具有刺细胞，需要注意防护。</p>'),
(5, '<h2>海豚简介</h2><p>海豚行动敏捷，常通过回声定位寻找猎物，也会通过声音与同伴保持联系。</p>'),
(6, '<h2>抹香鲸简介</h2><p>抹香鲸头部巨大，潜水能力很强，能够进入深海捕食大型乌贼。</p>'),
(7, '<h2>虎鲸保护知识</h2><p>海洋污染、噪声干扰和食物链变化都会影响虎鲸生存。保护海洋环境，就是保护虎鲸的栖息地。</p>');

-- 后台默认用户，登录名 admin，密码 123456
INSERT INTO `user` (`id`, `login_name`, `name`, `password`) VALUES
(1, 'admin', '管理员', '123456'),
(2, 'student', '学习账号', '123456');

-- 角色数据
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `built_in`, `sort`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '系统最高权限角色，只能分配给一个用户', 1, 1),
(2, 'CONTENT_ADMIN', '科研人员', '负责电子书、分类和文档编辑，可以提交电子书审核，不负责审核发布', 1, 2),
(4, 'CONTENT_REVIEWER', '内容审核员', '负责审核电子书是否可以发布，不负责日常编辑维护', 1, 3),
(3, 'NORMAL_USER', '普通用户', '注册后默认角色，可以阅读电子书、投稿文档和使用小游戏', 1, 4);

-- 权限数据
INSERT INTO `sys_permission` (`id`, `code`, `name`, `module`, `path`, `sort`) VALUES
(1, 'ebook:view', '查看电子书', '电子书', '/', 1),
(2, 'ebook:manage', '管理电子书', '电子书', '/admin/ebook', 2),
(9, 'ebook:review', '审核电子书', '电子书', '/admin/ebook', 3),
(3, 'category:manage', '管理分类', '分类', '/admin/category', 4),
(4, 'doc:manage', '管理文档', '文档', '/admin/doc', 5),
(5, 'statistics:view', '查看统计', '统计', '/statistics', 6),
(6, 'user:manage', '管理用户', '系统管理', '/admin/user', 7),
(7, 'role:manage', '管理角色权限', '系统管理', '/admin/role', 8),
(8, 'game:play', '使用小游戏', '小游戏', '/piano-tiles', 9),
(10, 'comment:manage', '管理评论', '系统管理', '/admin/comment', 10),
(11, 'feedback:manage', '管理反馈', '系统管理', '/admin/feedback', 11);

-- 超级管理员拥有全部权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID_SHORT(), 1, `id` FROM `sys_permission`;

-- 科研人员拥有内容编辑相关权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
(UUID_SHORT(), 2, 1),
(UUID_SHORT(), 2, 2),
(UUID_SHORT(), 2, 3),
(UUID_SHORT(), 2, 4),
(UUID_SHORT(), 2, 5),
(UUID_SHORT(), 2, 8);

-- 内容审核员负责电子书审核、评论审核和反馈审核，也可以阅读电子书和使用小游戏
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
(UUID_SHORT(), 4, 1),
(UUID_SHORT(), 4, 9),
(UUID_SHORT(), 4, 8),
(UUID_SHORT(), 4, 10),
(UUID_SHORT(), 4, 11);

-- 普通用户可以阅读、投稿文档和使用小游戏；投稿文档不单独配置权限，只要登录即可提交审核
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
(UUID_SHORT(), 3, 1),
(UUID_SHORT(), 3, 8);

-- 默认账号角色：超级管理员只能有 admin 一个，student 是普通用户
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(UUID_SHORT(), 1, 1),
(UUID_SHORT(), 2, 3);

-- 初始化电子书统计字段
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

-- 初始化今天的统计快照
INSERT INTO `ebook_snapshot` (`ebook_id`, `date`, `view_count`, `vote_count`, `view_increase`, `vote_increase`)
SELECT `id`, CURDATE(), `view_count`, `vote_count`, 0, 0
FROM `ebook`;

SET FOREIGN_KEY_CHECKS = 1;

