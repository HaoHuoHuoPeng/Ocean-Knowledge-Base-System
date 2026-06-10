// 文件说明：这个业务实现类负责用户的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.UserRole;
import com.shiwangsi.oceanwiki.mapper.UserRoleMapper;
import com.shiwangsi.oceanwiki.service.IUserRoleService;
import org.springframework.stereotype.Service;

// 用户角色业务实现类
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements IUserRoleService {
}
