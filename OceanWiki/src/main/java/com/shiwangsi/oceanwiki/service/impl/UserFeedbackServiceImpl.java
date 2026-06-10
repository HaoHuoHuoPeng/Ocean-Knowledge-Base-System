// 文件说明：这个业务实现类负责反馈的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.UserFeedback;
import com.shiwangsi.oceanwiki.mapper.UserFeedbackMapper;
import com.shiwangsi.oceanwiki.service.IUserFeedbackService;
import org.springframework.stereotype.Service;

// 用户反馈业务实现类
// 普通用户提交建议或纠错，管理员处理反馈，底层都通过这个类操作 user_feedback 表
@Service
public class UserFeedbackServiceImpl extends ServiceImpl<UserFeedbackMapper, UserFeedback> implements IUserFeedbackService {
}
