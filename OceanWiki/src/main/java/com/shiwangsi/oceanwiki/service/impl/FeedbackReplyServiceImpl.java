// 文件说明：这个业务实现类负责反馈回复的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.FeedbackReply;
import com.shiwangsi.oceanwiki.mapper.FeedbackReplyMapper;
import com.shiwangsi.oceanwiki.service.IFeedbackReplyService;
import org.springframework.stereotype.Service;

// 反馈回复业务实现类
@Service
public class FeedbackReplyServiceImpl extends ServiceImpl<FeedbackReplyMapper, FeedbackReply> implements IFeedbackReplyService {
}
