// 文件说明：这个实体类对应权限相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 权限实体类，对应 sys_permission 表
// 权限是更细的能力，例如电子书管理、用户管理、角色管理
@Data
@TableName("sys_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    // 权限主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 权限编码，前后端判断权限时主要看这个值
    private String code;

    // 权限名称，页面上展示给用户看
    private String name;

    // 所属模块，方便页面分组展示
    private String module;

    // 对应前端页面地址，不是每个按钮权限都一定有页面地址
    private String path;

    // 排序值，数字越小越靠前
    private Integer sort;
}

