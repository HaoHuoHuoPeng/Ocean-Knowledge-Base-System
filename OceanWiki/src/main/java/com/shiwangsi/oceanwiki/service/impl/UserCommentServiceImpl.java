// 文件说明：这个业务实现类负责评论的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.mapper.UserCommentMapper;
import com.shiwangsi.oceanwiki.service.IUserCommentService;
import org.springframework.stereotype.Service;

// 用户评论业务实现类
// 评论发布、审核、删除这些规则放在 Controller 里，方便看完整业务过程
@Service
public class UserCommentServiceImpl extends ServiceImpl<UserCommentMapper, UserComment> implements IUserCommentService {
}
