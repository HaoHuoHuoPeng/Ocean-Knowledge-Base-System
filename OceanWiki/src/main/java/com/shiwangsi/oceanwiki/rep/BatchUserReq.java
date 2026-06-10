// 文件说明：这个请求参数类用来接收用户管理里的批量操作参数。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

import java.util.List;

// 用户批量操作请求
// 批量重置密码、批量分配角色都会用到用户 id 列表
@Data
public class BatchUserReq {

    // 要批量操作的用户 id 列表
    private List<Long> userIds;

    // 批量重置密码时使用；为空时后端默认重置为 123456
    private String password;

    // 批量分配角色时使用；一个用户只能拥有一个角色，所以这里只接收一个角色 id
    private Long roleId;
}
