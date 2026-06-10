// 文件说明：这个异常文件负责BusinessException相关错误处理，让接口错误返回更统一。
package com.shiwangsi.oceanwiki.exception;

// 自定义业务异常
// 例如登录名已存在、密码错误、分类下面还有子分类等，都可以抛这个异常
public class BusinessException extends RuntimeException {

    // code 保存具体错误类型，统一异常处理器会读取它的中文提示
    private BusinessExceptionCode code;

    public BusinessException(BusinessExceptionCode code) {
        super(code.getDesc());
        this.code = code;
    }

    public BusinessExceptionCode getCode() {
        return code;
    }

    public void setCode(BusinessExceptionCode code) {
        this.code = code;
    }

    // 业务异常通常不需要完整堆栈，减少控制台输出噪音
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
