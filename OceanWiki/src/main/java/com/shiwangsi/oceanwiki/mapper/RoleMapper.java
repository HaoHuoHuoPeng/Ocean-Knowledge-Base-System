// 文件说明：这个 Mapper 负责角色的数据库访问，继承 BaseMapper 后可以直接做基础增删改查。
package com.shiwangsi.oceanwiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwangsi.oceanwiki.entity.Role;

// 角色 Mapper，负责 sys_role 表的基础增删改查
public interface RoleMapper extends BaseMapper<Role> {
}
