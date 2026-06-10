// 文件说明：这个过滤器负责包装请求，减少表单和请求体里的 XSS 脚本注入风险。
package com.shiwangsi.oceanwiki.config.xss;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/*
 * XSS 请求过滤器。
 * 所有请求都会先经过这里，再交给 Controller 处理。
 */
@WebFilter(urlPatterns = "/*")
public class XssFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 这里暂时不需要初始化内容
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // ServletRequest 是通用请求对象，先转换成 HTTP 请求对象
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // 用我们自己的包装类包一层，读取参数时就可以做 XSS 过滤
        XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

        // 放行请求，把包装后的请求继续传给后面的 Controller
        filterChain.doFilter(wrapper, servletResponse);
    }

    @Override
    public void destroy() {
        // 这里暂时不需要释放资源
    }
}
