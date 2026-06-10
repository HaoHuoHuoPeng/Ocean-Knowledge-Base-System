// 文件说明：这个实体类对应角色相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

// 角色实体类，对应 sys_role 表
// 一个用户可以有多个角色，一个角色也可以分配给多个用户
@Data
@TableName("sys_role")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    // 角色主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 角色编码，程序里用它判断角色身份
    private String code;

    // 角色名称，页面上展示给用户看
    private String name;

    // 角色说明，方便知道这个角色是干什么的
    private String description;

    // 是否内置角色，1 表示系统自带角色，不建议删除
    private Integer builtIn;

    // 排序值，数字越小越靠前
    private Integer sort;

    // 角色拥有的权限 id 列表，只给前端新增、编辑、回显权限时使用，不是 sys_role 表字段
    @TableField(exist = false)
    private List<Long> permissionIds;
}

