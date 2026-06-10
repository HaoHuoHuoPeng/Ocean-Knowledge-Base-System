// 文件说明：这个 Mapper 负责评论的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.UserComment;

// 用户评论 Mapper，负责 user_comment 表的基础增删改查
public interface UserCommentMapper extends BaseMapper<UserComment> {
}
