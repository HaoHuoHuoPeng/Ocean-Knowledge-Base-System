// 文件说明：这个业务实现类负责用户的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.mapper.UserMapper;
import com.shiwangsi.oceanwiki.service.IUserService;
import org.springframework.stereotype.Service;

// 用户业务实现类，登录和重置密码逻辑目前写在 UserController 里
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
}
