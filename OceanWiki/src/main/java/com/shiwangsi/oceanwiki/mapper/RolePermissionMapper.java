// 文件说明：这个 Mapper 负责权限的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.RolePermission;

// 角色权限 Mapper，负责 sys_role_permission 表的基础增删改查
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
