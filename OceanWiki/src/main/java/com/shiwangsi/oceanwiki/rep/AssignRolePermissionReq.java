// 文件说明：这个请求参数类用来接收前端提交的权限表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

import java.util.List;

// 给角色分配权限时，前端提交这个对象
@Data
public class AssignRolePermissionReq {

    // 要分配权限的角色 id
    private Long roleId;

    // 这个角色最终拥有的权限 id 列表
    private List<Long> permissionIds;
}
