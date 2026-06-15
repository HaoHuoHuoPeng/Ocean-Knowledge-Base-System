// 文件说明：这个工具类负责把登录 token 对应的用户 id、角色和权限保存到 Redis。
package com.shiwangsi.oceanwiki.config.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// 登录会话存储
// 正常运行时使用 Redis，Redis 临时不可用时用内存兜底，避免开发时整个登录流程直接不可用
@Component
public class AuthTokenStore {

    private static final String TOKEN_KEY_PREFIX = "oceanwiki:login:token:";
    private static final long TOKEN_EXPIRE_HOURS = 24;
    private static final Map<String, LoginUserInfo> TOKEN_MAP = new ConcurrentHashMap<>();
    private static StringRedisTemplate redisTemplate;
    private static ObjectMapper objectMapper;

    public AuthTokenStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        AuthTokenStore.redisTemplate = redisTemplate;
        AuthTokenStore.objectMapper = objectMapper;
    }

    public static void put(String token, Long userId, List<String> roleCodes, List<String> permissions) {
        LoginUserInfo loginUserInfo = new LoginUserInfo(userId, roleCodes, permissions);
        TOKEN_MAP.put(token, loginUserInfo);
        if (canUseRedis()) {
            try {
                redisTemplate.opsForValue().set(redisKey(token), objectMapper.writeValueAsString(loginUserInfo), TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
            } catch (Exception ignored) {
                // Redis 不可用时保留内存兜底
            }
        }
    }

    public static LoginUserInfo get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (canUseRedis()) {
            try {
                String value = redisTemplate.opsForValue().get(redisKey(token));
                if (value != null && !value.isBlank()) {
                    LoginUserInfo loginUserInfo = objectMapper.readValue(value, LoginUserInfo.class);
                    TOKEN_MAP.put(token, loginUserInfo);
                    redisTemplate.expire(redisKey(token), TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
                    return loginUserInfo;
                }
            } catch (JsonProcessingException ignored) {
                remove(token);
                return null;
            } catch (Exception ignored) {
                // Redis 不可用时读取内存兜底
            }
        }
        return TOKEN_MAP.get(token);
    }

    public static void remove(String token) {
        TOKEN_MAP.remove(token);
        if (canUseRedis()) {
            try {
                redisTemplate.delete(redisKey(token));
            } catch (Exception ignored) {
                // Redis 不可用时删除内存即可
            }
        }
    }

    private static boolean canUseRedis() {
        return redisTemplate != null && objectMapper != null;
    }

    private static String redisKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    public record LoginUserInfo(Long userId, List<String> roleCodes, List<String> permissions) {
    }
}
