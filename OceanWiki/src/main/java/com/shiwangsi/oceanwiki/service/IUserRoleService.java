// 文件说明：这个业务接口定义用户模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.UserRole;

// 用户角色业务接口，继承 MyBatis-Plus 提供的通用 IService
public interface IUserRoleService extends IService<UserRole> {
}
