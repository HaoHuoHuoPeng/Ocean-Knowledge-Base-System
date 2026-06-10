// 文件说明：这个 Mapper 负责反馈的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.UserFeedback;

// 用户反馈 Mapper，负责 user_feedback 表的基础增删改查
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
}
