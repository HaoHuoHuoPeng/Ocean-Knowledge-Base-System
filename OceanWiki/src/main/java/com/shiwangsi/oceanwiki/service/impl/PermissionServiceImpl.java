// 文件说明：这个业务实现类负责权限的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.Permission;
import com.shiwangsi.oceanwiki.mapper.PermissionMapper;
import com.shiwangsi.oceanwiki.service.IPermissionService;
import org.springframework.stereotype.Service;

// 权限业务实现类
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {
}
