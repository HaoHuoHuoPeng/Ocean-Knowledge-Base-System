// 文件说明：这个实体类对应权限相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 角色权限关联实体类，对应 sys_role_permission 表
// 这张表负责记录“某个角色拥有哪些权限”
@Data
@TableName("sys_role_permission")
public class RolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    // 关联记录主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 角色 id
    @TableField("role_id")
    private Long roleId;

    // 权限 id
    @TableField("permission_id")
    private Long permissionId;
}

