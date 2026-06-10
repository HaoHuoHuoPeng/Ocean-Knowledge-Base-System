// 文件说明：这个请求参数类用来接收前端提交的用户表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

import java.util.List;

// 给用户分配角色时，前端提交这个对象
@Data
public class AssignUserRoleReq {

    // 要分配角色的用户 id
    private Long userId;

    // 这个用户最终拥有的角色 id 列表
    private List<Long> roleIds;
}
