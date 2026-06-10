// 文件说明：这个实体类对应用户相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 用户角色关联实体类，对应 sys_user_role 表
// 这张表负责记录“哪个用户拥有哪个角色”
@Data
@TableName("sys_user_role")
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    // 关联记录主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 用户 id
    @TableField("user_id")
    private Long userId;

    // 角色 id
    @TableField("role_id")
    private Long roleId;
}

