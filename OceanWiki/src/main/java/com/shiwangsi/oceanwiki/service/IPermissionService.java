// 文件说明：这个业务接口定义权限模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.Permission;

// 权限业务接口，继承 MyBatis-Plus 提供的通用 IService
public interface IPermissionService extends IService<Permission> {
}
