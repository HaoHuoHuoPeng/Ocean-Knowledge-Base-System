// 文件说明：这个工具类负责密码加密、密码校验和判断密码是否已经加密。
package com.shiwangsi.oceanwiki.utils;

import cn.hutool.crypto.digest.BCrypt;

// 密码工具类
// 新密码统一用 BCrypt 保存，老的明文密码登录成功后会自动升级
public class PasswordUtil {

    private PasswordUtil() {
    }

    // 加密原始密码
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    // 判断输入密码和数据库密码是否匹配
    public static boolean matches(String rawPassword, String dbPassword) {
        if (rawPassword == null || dbPassword == null) {
            return false;
        }

        if (isBCrypt(dbPassword)) {
            return BCrypt.checkpw(rawPassword, dbPassword);
        }

        // 兼容旧数据：旧密码如果是明文，仍然允许本次登录
        return rawPassword.equals(dbPassword);
    }

    // 判断数据库里的密码是否已经是 BCrypt 格式
    public static boolean isBCrypt(String dbPassword) {
        return dbPassword != null
                && (dbPassword.startsWith("$2a$")
                || dbPassword.startsWith("$2b$")
                || dbPassword.startsWith("$2y$"));
    }
}
