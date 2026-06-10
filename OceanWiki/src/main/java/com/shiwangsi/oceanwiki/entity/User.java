// 文件说明：这个实体类对应用户相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

// 用户实体类，对应数据库中的 user 表
// 当前项目先做基础登录和用户管理，密码先明文保存，后面正式项目要改成加密保存
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // 用户主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 登录名，用来登录系统
    @TableField("login_name")
    private String loginName;

    // 用户昵称或真实姓名
    private String name;

    // 登录密码
    private String password;

    // 用户拥有的角色 id 列表，只给前端展示和提交使用，不是 user 表字段
    @TableField(exist = false)
    private List<Long> roleIds;

    // 用户拥有的角色名称列表，只给前端展示使用，不是 user 表字段
    @TableField(exist = false)
    private List<String> roleNames;

    // 用户拥有的角色编码列表，只给前端判断使用，不是 user 表字段
    @TableField(exist = false)
    private List<String> roleCodes;

    // 用户最终拥有的权限编码列表，只给前端判断使用，不是 user 表字段
    @TableField(exist = false)
    private List<String> permissions;
}

