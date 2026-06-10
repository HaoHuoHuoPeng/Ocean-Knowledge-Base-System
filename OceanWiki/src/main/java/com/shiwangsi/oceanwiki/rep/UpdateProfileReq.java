// 文件说明：这个请求参数类用来接收前端提交的UpdateProfileReq表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 当前登录用户修改自己资料时使用的请求对象
// 这里不接收用户 id，后端直接从 token 判断当前是谁，避免用户伪造 id 修改别人资料
@Data
public class UpdateProfileReq {

    // 新账号，对应 user 表里的 login_name
    private String loginName;

    // 新姓名，对应页面上显示的昵称或真实姓名
    private String name;

    // 当前密码，修改密码时必须填写，用来确认是本人操作
    private String currentPassword;

    // 新密码，不填表示只修改账号和姓名，不修改密码
    private String newPassword;
}
