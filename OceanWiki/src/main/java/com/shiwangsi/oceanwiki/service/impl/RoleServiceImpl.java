// 文件说明：这个业务实现类负责角色的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.Role;
import com.shiwangsi.oceanwiki.mapper.RoleMapper;
import com.shiwangsi.oceanwiki.service.IRoleService;
import org.springframework.stereotype.Service;

// 角色业务实现类
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {
}
