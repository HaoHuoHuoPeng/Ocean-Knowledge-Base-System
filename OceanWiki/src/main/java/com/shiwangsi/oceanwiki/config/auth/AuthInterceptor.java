// 文件说明：这个拦截器负责后端接口权限判断，根据 token 和权限决定请求能不能放行。
package com.shiwangsi.oceanwiki.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

// 接口权限拦截器
// 前端隐藏菜单只是为了页面体验，真正能不能访问后台接口，还要在这里再判断一次
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String LOGIN_REQUIRED = "__LOGIN_REQUIRED__";

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String permission = resolvePermission(request.getMethod(), request.getRequestURI());
        if (permission == null) {
            return true;
        }

        String token = request.getHeader("Authorization");
        AuthTokenStore.LoginUserInfo loginUserInfo = AuthTokenStore.get(token);
        if (loginUserInfo == null) {
            writeFail(response, "请先登录");
            return false;
        }

        if (LOGIN_REQUIRED.equals(permission)) {
            return true;
        }

        // 后台看板是系统级总览，只允许唯一的超级管理员查看
        if (isSuperAdminOnly(request.getRequestURI())) {
            if (loginUserInfo.roleCodes() != null && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE)) {
                return true;
            }
            writeFail(response, "后台看板仅超级管理员可访问");
            return false;
        }

        // 超级管理员默认拥有全部权限，不需要逐个勾选权限
        if (loginUserInfo.roleCodes() != null && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE)) {
            return true;
        }

        if (!hasPermission(loginUserInfo, permission)) {
            writeFail(response, "没有操作权限");
            return false;
        }

        return true;
    }

    private String resolvePermission(String method, String uri) {
        if (uri.startsWith("/role") || uri.startsWith("/permission")) {
            return "role:manage";
        }

        // 当前登录用户修改自己的资料不属于用户管理权限，否则普通用户会进不了账号资料页
        if (uri.startsWith("/user")
                && !uri.startsWith("/user/login")
                && !uri.startsWith("/user/register")
                && !uri.startsWith("/user/logout")
                && !uri.startsWith("/user/profile")) {
            return "user:manage";
        }

        if (uri.startsWith("/ebook/save")
                || uri.startsWith("/ebook/delete")
                || uri.startsWith("/ebook/submitReview")
                || uri.startsWith("/ebook/offline")
                || uri.startsWith("/ebook/refresh")
                || uri.startsWith("/ebook/export")) {
            return "ebook:manage";
        }

        if (uri.startsWith("/ebook/review")) {
            return "ebook:review";
        }

        if (uri.startsWith("/upload/ebook-cover")) {
            return "ebook:manage";
        }

        if (uri.startsWith("/upload/doc-image")) {
            return LOGIN_REQUIRED;
        }

        if (uri.startsWith("/category/save") || uri.startsWith("/category/delete")) {
            return "category:manage";
        }

        if (uri.startsWith("/doc/submit") || uri.startsWith("/doc/my-submissions")) {
            return null;
        }

        if (uri.startsWith("/doc/review")) {
            return "doc:review";
        }

        if (uri.equals("/doc/all") || uri.equals("/doc/pageTree")) {
            return "doc:manage|doc:review";
        }

        if (uri.startsWith("/doc/save")
                || uri.startsWith("/doc/delete")
                || uri.startsWith("/doc/remove")
                || uri.startsWith("/doc/versions")
                || uri.startsWith("/doc/version")
                || uri.startsWith("/doc/rollback")) {
            return "doc:manage";
        }

        if (uri.startsWith("/content")) {
            return "doc:manage|doc:review";
        }

        if (uri.startsWith("/comment/admin")) {
            return "comment:manage";
        }

        if (uri.startsWith("/feedback/admin")) {
            return "feedback:manage";
        }

        if (uri.startsWith("/operationLog")) {
            return "operation:log";
        }

        if (uri.startsWith("/sensitiveWord")) {
            return "sensitive:manage";
        }

        if (uri.startsWith("/ebookSnapshot") || uri.startsWith("/dashboard")) {
            return "statistics:view";
        }

        if (uri.startsWith("/game")) {
            return "game:play";
        }

        return null;
    }

    private boolean hasPermission(AuthTokenStore.LoginUserInfo loginUserInfo, String permission) {
        if (loginUserInfo.permissions() == null) {
            return false;
        }
        for (String item : permission.split("\\|")) {
            if (loginUserInfo.permissions().contains(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuperAdminOnly(String uri) {
        return uri.startsWith("/dashboard");
    }

    private void writeFail(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(CommonResp.fail(message)));
    }
}
