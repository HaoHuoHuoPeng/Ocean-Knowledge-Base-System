// 文件说明：这个 Controller 负责操作日志相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 操作日志接口
// 只有有权限的管理员可以查看，用来追踪后台关键操作
@Tag(name = "操作日志接口")
@RestController
@RequestMapping("/operationLog")
public class OperationLogController {

    private final IOperationLogService operationLogService;

    public OperationLogController(IOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    // 分页查询操作日志
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    public CommonResp<PageResp<OperationLog>> page(String keyword, Long current, Long pageSize) {
        Page<OperationLog> page = operationLogService.page(new Page<>(safeCurrent(current), safePageSize(pageSize)), buildQuery(keyword));
        return CommonResp.ok(PageResp.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    // 兼容旧前端接口
    @Operation(summary = "查询操作日志")
    @GetMapping("/list")
    public CommonResp<List<OperationLog>> list(String keyword) {
        return CommonResp.ok(operationLogService.list(buildQuery(keyword)));
    }

    // 导出操作日志 CSV
    @Operation(summary = "导出操作日志 CSV")
    @GetMapping("/export")
    public void export(String keyword, HttpServletResponse response) throws IOException {
        List<OperationLog> list = operationLogService.list(buildQuery(keyword));
        writeCsvResponse(response, "操作日志.csv");
        // 写入 UTF-8 BOM，避免 Windows 上用 Excel 打开 CSV 时中文变成乱码
        response.getWriter().write('\ufeff');
        response.getWriter().println("ID,用户ID,模块,动作,内容,修改前,修改后,操作时间");
        for (OperationLog log : list) {
            response.getWriter().println(csv(log.getId())
                    + "," + csv(log.getUserId())
                    + "," + csv(log.getModule())
                    + "," + csv(log.getAction())
                    + "," + csv(log.getContent())
                    + "," + csv(log.getBeforeData())
                    + "," + csv(log.getAfterData())
                    + "," + csv(log.getCreateTime()));
        }
    }

    private QueryWrapper<OperationLog> buildQuery(String keyword) {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("module", keyword)
                    .or()
                    .like("action", keyword)
                    .or()
                    .like("content", keyword));
        }
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    private long safeCurrent(Long current) {
        return current == null || current < 1 ? 1 : current;
    }

    private long safePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private void writeCsvResponse(HttpServletResponse response, String fileName) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
