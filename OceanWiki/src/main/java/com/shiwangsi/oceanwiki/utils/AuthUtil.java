// 文件说明：这个工具类负责从请求头 token 中获取当前登录用户 id。
package com.shiwangsi.oceanwiki.utils;

import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.exception.BusinessExceptionCode;
import jakarta.servlet.http.HttpServletRequest;

// 登录工具类
// Controller 里经常需要知道“当前是谁在操作”，所以把读取 token 的代码集中放在这里
public class AuthUtil {

    private AuthUtil() {
    }

    // 尝试读取当前登录用户 id
    // 没有登录时返回 null，适合阅读正文这种“未登录也能继续处理”的场景
    public static Long getCurrentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        AuthTokenStore.LoginUserInfo loginUserInfo = AuthTokenStore.get(token);
        return loginUserInfo == null ? null : loginUserInfo.userId();
    }

    // 读取当前登录用户的完整会话信息
    // 需要判断角色和权限时使用，例如只允许审核员查看待审核数据
    public static AuthTokenStore.LoginUserInfo getLoginUserInfo(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        return AuthTokenStore.get(token);
    }

    // 强制要求当前用户已登录
    // 收藏、评论、反馈、个人中心都必须登录，所以这些接口使用这个方法
    public static Long requireLogin(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            throw new BusinessException(BusinessExceptionCode.LOGIN_USER_ERROR);
        }
        return userId;
    }
}
