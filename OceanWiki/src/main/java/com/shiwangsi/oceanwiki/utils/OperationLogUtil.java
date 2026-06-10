// 文件说明：这个工具类负责统一写入后台操作日志，避免每个 Controller 重复写代码。
package com.shiwangsi.oceanwiki.utils;

import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

// 操作日志工具类
// 后台关键操作统一调用这里保存日志，避免每个 Controller 重复写保存逻辑
public class OperationLogUtil {

    private OperationLogUtil() {
    }

    // 记录当前登录用户的操作
    // 如果请求里没有登录态，也允许写入空 userId，方便排查异常调用
    public static void save(IOperationLogService operationLogService,
                            HttpServletRequest request,
                            String module,
                            String action,
                            String content) {
        Long userId = AuthUtil.getCurrentUserId(request);
        save(operationLogService, userId, module, action, content);
    }

    // 记录当前登录用户的操作，并保存修改前后的关键数据
    public static void save(IOperationLogService operationLogService,
                            HttpServletRequest request,
                            String module,
                            String action,
                            String content,
                            String beforeData,
                            String afterData) {
        Long userId = AuthUtil.getCurrentUserId(request);
        save(operationLogService, userId, module, action, content, beforeData, afterData);
    }

    // 记录指定用户的操作
    public static void save(IOperationLogService operationLogService,
                            Long userId,
                            String module,
                            String action,
                            String content) {
        save(operationLogService, userId, module, action, content, null, null);
    }

    // 记录指定用户的操作，并保存修改前后的关键数据
    public static void save(IOperationLogService operationLogService,
                            Long userId,
                            String module,
                            String action,
                            String content,
                            String beforeData,
                            String afterData) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setModule(module);
        log.setAction(action);
        log.setContent(content);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setCreateTime(LocalDateTime.now());
        operationLogService.save(log);
    }
}
