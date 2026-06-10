// 文件说明：这个异常文件负责ObwikiExceptionAdvice相关错误处理，让接口错误返回更统一。
package com.shiwangsi.oceanwiki.exception;

import com.shiwangsi.oceanwiki.resp.CommonResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// 统一异常处理类
// Controller 里抛出的异常会在这里转成 CommonResp，前端就不会收到一大段报错页面
@Slf4j
@RestControllerAdvice(basePackages = "com.shiwangsi.oceanwiki.controller")
public class ObwikiExceptionAdvice {

    // 处理参数校验异常，比如 @NotBlank 校验失败
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public CommonResp<Map<String, String>> handleValidException(MethodArgumentNotValidException e) {
        log.error("数据校验异常：{}", e.getMessage());
        BindingResult result = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();

        // 把每个字段的错误信息收集起来，方便前端精确提示
        result.getFieldErrors().forEach(item -> errorMap.put(item.getField(), item.getDefaultMessage()));
        return new CommonResp<>(false, "数据校验异常", errorMap);
    }

    // 处理业务异常，比如登录失败、分类不能删除
    @ExceptionHandler(BusinessException.class)
    public CommonResp<Object> handlerBusinessException(BusinessException e) {
        log.warn(e.getCode().getDesc());
        return CommonResp.fail(e.getCode().getDesc());
    }

    // 处理系统异常，防止完整异常堆栈直接暴露给前端
    @ExceptionHandler(Exception.class)
    public CommonResp<Object> handlerException(Exception e) {
        log.error(e.getMessage(), e);
        return CommonResp.fail("系统异常，请查看后端控制台日志");
    }
}
