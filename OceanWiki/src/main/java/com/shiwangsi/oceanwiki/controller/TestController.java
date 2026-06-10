// 文件说明：这个 Controller 负责Test相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.shiwangsi.oceanwiki.rep.DemoReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// @RestController 表示这个类专门接收浏览器或前端发来的 HTTP 请求
@RestController
// 当前类下面的所有接口地址都会以 /test 开头
@RequestMapping("/test")
// Knife4j 文档里显示的接口分组名称
@Tag(name = "测试接口")
public class TestController {

    private final IUserService userService;
    private final IEbookService ebookService;
    private final IDocService docService;

    public TestController(IUserService userService, IEbookService ebookService, IDocService docService) {
        this.userService = userService;
        this.ebookService = ebookService;
        this.docService = docService;
    }

    // GET 请求地址：http://127.0.0.1:8881/test/health
    @GetMapping("/health")
    @Operation(summary = "后端健康检查")
    public CommonResp<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        // status 表示后端服务当前可以正常响应请求
        result.put("status", "UP");
        // time 表示本次健康检查的后端服务器时间
        result.put("time", LocalDateTime.now());
        return CommonResp.ok("后端服务正常", result);
    }

    // GET 请求地址：http://127.0.0.1:8881/test/database-count
    @GetMapping("/database-count")
    @Operation(summary = "数据库基础数量检查")
    public CommonResp<Map<String, Long>> databaseCount() {
        Map<String, Long> result = new LinkedHashMap<>();
        // userCount 表示当前用户总数
        result.put("userCount", userService.count());
        // ebookCount 表示当前电子书总数
        result.put("ebookCount", ebookService.count());
        // docCount 表示当前文档总数
        result.put("docCount", docService.count());
        return CommonResp.ok("数据库连接正常", result);
    }

    // POST 请求地址：http://127.0.0.1:8881/test/sayHello
    @PostMapping("/sayHello")
    // Knife4j 文档里显示的方法说明
    @Operation(summary = "最简单的测试方法")
    public CommonResp<DemoReq> sayHello(@Valid @RequestBody DemoReq demoReq) {
        // @RequestBody 表示从请求体 JSON 中接收参数
        // @Valid 表示启用 DemoReq 里的参数校验注解
        return new CommonResp<>(true, "查询成功", demoReq);
    }
}
