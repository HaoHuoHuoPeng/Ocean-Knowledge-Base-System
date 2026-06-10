// 文件说明：这个 Mapper 负责反馈回复的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.FeedbackReply;

// 反馈回复 Mapper，负责 feedback_reply 表的基础增删改查
public interface FeedbackReplyMapper extends BaseMapper<FeedbackReply> {
}
