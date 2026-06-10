// 文件说明：这个异常文件负责BusinessExceptionCode相关错误处理，让接口错误返回更统一。
package com.shiwangsi.oceanwiki.exception;

// 业务异常枚举
// 新增错误提示时统一写在这里，Controller 和 Service 里不要到处散落中文错误字符串
public enum BusinessExceptionCode {

    USER_LOGIN_NAME_EXIST("账号已存在"),
    LOGIN_USER_ERROR("用户名不存在或密码错误"),
    ROLE_NOT_FOUND("角色不存在"),
    ROLE_CODE_EXIST("角色编码已存在"),
    ROLE_BUILT_IN_CAN_NOT_DELETE("系统内置角色不能删除"),
    SUPER_ADMIN_ROLE_CAN_NOT_DELETE("超级管理员角色不能删除"),
    NORMAL_USER_ROLE_CAN_NOT_DELETE("普通用户角色不能删除"),
    NORMAL_USER_ROLE_NOT_FOUND("普通用户角色不存在"),
    USER_ROLE_ONLY_ONE("用户只能分配一个角色"),
    SUPER_ADMIN_CAN_NOT_ASSIGN("不能通过用户管理新增或分配超级管理员"),
    SUPER_ADMIN_ONLY_ONE("超级管理员只能有一个"),
    SUPER_ADMIN_CAN_NOT_DELETE("超级管理员不能删除"),
    CURRENT_USER_CAN_NOT_DELETE("不能删除当前登录用户"),
    CURRENT_PASSWORD_ERROR("当前密码错误"),
    SUPER_ADMIN_PERMISSION_CAN_NOT_EMPTY("超级管理员权限不能为空"),
    REGISTER_INFO_ERROR("请填写登录名、昵称和密码"),
    CATEGORY_HAS_CHILD("该分类下面还有子分类，不能直接删除"),
    CATEGORY_HAS_EBOOK("该分类下面还有电子书，不能直接删除"),
    EBOOK_HAS_DOC("该电子书下面还有文档，不能直接删除，请先处理文档"),
    EBOOK_HAS_USER_DATA("该电子书已经产生阅读、收藏、评论等用户数据，不能直接删除，建议改为下架"),
    DOC_NOT_FOUND("文档不存在"),
    EBOOK_NOT_FOUND("电子书不存在");

    // desc 是返回给前端看的中文提示
    private String desc;

    BusinessExceptionCode(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
