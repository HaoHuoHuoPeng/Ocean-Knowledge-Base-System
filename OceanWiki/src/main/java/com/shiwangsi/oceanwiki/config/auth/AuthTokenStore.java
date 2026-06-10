// 文件说明：这个工具类负责保存登录 token 对应的用户 id、角色和权限。
package com.shiwangsi.oceanwiki.config.auth;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 简单的登录会话存储
// 当前项目先把 token 放在内存里，后续需要多服务部署时再换成 Redis
public class AuthTokenStore {

    private static final Map<String, LoginUserInfo> TOKEN_MAP = new ConcurrentHashMap<>();

    private AuthTokenStore() {
    }

    public static void put(String token, Long userId, List<String> roleCodes, List<String> permissions) {
        TOKEN_MAP.put(token, new LoginUserInfo(userId, roleCodes, permissions));
    }

    public static LoginUserInfo get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return TOKEN_MAP.get(token);
    }

    public static void remove(String token) {
        TOKEN_MAP.remove(token);
    }

    public record LoginUserInfo(Long userId, List<String> roleCodes, List<String> permissions) {
    }
}
