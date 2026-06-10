// 文件说明：这个业务接口定义用户模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.User;

// 用户业务接口，Controller 调用 Service，不直接调用 Mapper
// 继承 IService<User> 后，MyBatis-Plus 会自动提供常用业务方法
public interface IUserService extends IService<User> {

}
