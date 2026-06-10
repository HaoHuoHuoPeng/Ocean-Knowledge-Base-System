// 文件说明：这个 Controller 负责统计快照相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IEbookSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 统计接口
// 首页统计数字和统计页面趋势都走这里
@Tag(name = "统计接口")
@RestController
@RequestMapping("/ebookSnapshot")
public class EbookSnapshotController {

    private final IEbookSnapshotService ebookSnapshotService;

    public EbookSnapshotController(IEbookSnapshotService ebookSnapshotService) {
        this.ebookSnapshotService = ebookSnapshotService;
    }

    // 手动生成今天的快照
    @Operation(summary = "生成今天统计快照")
    @GetMapping("/genSnapshot")
    public CommonResp<Object> genSnapshot() {
        ebookSnapshotService.genSnapshot();
        return CommonResp.ok("生成成功", null);
    }

    // 首页统计总览
    @Operation(summary = "查询首页统计")
    @GetMapping("/getStatistic")
    public CommonResp<Map<String, Object>> getStatistic() {
        return CommonResp.ok(ebookSnapshotService.getStatistic());
    }

    // 最近 30 天统计
    @Operation(summary = "查询最近 30 天统计")
    @GetMapping("/get30Statistic")
    public CommonResp<List<Map<String, Object>>> get30Statistic() {
        return CommonResp.ok(ebookSnapshotService.get30Statistic());
    }
}
