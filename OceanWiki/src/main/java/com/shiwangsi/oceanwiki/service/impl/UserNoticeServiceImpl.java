// 文件说明：这个业务实现类负责通知的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.UserNotice;
import com.shiwangsi.oceanwiki.mapper.UserNoticeMapper;
import com.shiwangsi.oceanwiki.service.IUserNoticeService;
import org.springframework.stereotype.Service;

// 用户通知业务实现类
// 审核结果、反馈处理结果会写入通知，普通用户可以在个人中心查看
@Service
public class UserNoticeServiceImpl extends ServiceImpl<UserNoticeMapper, UserNotice> implements IUserNoticeService {
}
