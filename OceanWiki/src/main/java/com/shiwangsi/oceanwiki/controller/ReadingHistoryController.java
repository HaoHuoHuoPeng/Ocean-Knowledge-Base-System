// 文件说明：这个 Controller 负责阅读历史相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IReadingHistoryService;
import com.shiwangsi.oceanwiki.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 阅读历史接口
// 用户每次打开正文都会更新最近阅读时间，个人中心可以查看最近读过哪些内容
@Tag(name = "阅读历史接口")
@RestController
@RequestMapping("/history")
public class ReadingHistoryController {

    private final IReadingHistoryService historyService;
    private final IEbookService ebookService;
    private final IDocService docService;

    public ReadingHistoryController(IReadingHistoryService historyService, IEbookService ebookService, IDocService docService) {
        this.historyService = historyService;
        this.ebookService = ebookService;
        this.docService = docService;
    }

    // 查询我的阅读历史
    @Operation(summary = "查询我的阅读历史")
    @GetMapping("/my")
    public CommonResp<List<ReadingHistory>> my(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        List<ReadingHistory> list = historyService.list(new QueryWrapper<ReadingHistory>()
                .eq("user_id", userId)
                .orderByDesc("read_time"));
        Map<Long, ReadingHistory> latestHistoryMap = new LinkedHashMap<>();
        for (ReadingHistory history : list) {
            if (history.getEbookId() != null) {
                latestHistoryMap.putIfAbsent(history.getEbookId(), history);
            }
        }
        List<ReadingHistory> ebookHistories = latestHistoryMap.values().stream().toList();
        ebookHistories.forEach(history -> fillDisplayInfo(history, userId));
        return CommonResp.ok(ebookHistories);
    }

    // 查询最近阅读的一条记录
    // 首页用它展示“继续阅读”，用户可以直接回到上次读到的电子书和文档
    @Operation(summary = "查询最近阅读")
    @GetMapping("/latest")
    public CommonResp<ReadingHistory> latest(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ReadingHistory history = historyService.getOne(new QueryWrapper<ReadingHistory>()
                .eq("user_id", userId)
                .orderByDesc("read_time")
                .last("limit 1"));
        if (history != null) {
            fillDisplayInfo(history, userId);
        }
        return CommonResp.ok(history);
    }

    // 删除一条自己的阅读历史
    @Operation(summary = "删除阅读历史")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        ReadingHistory history = historyService.getOne(new QueryWrapper<ReadingHistory>()
                .eq("id", id)
                .eq("user_id", userId));
        if (history == null) {
            return CommonResp.ok("删除成功", null);
        }
        historyService.remove(new QueryWrapper<ReadingHistory>()
                .eq("user_id", userId)
                .eq("ebook_id", history.getEbookId()));
        return CommonResp.ok("删除成功", null);
    }

    // 清空自己的阅读历史
    @Operation(summary = "清空阅读历史")
    @DeleteMapping("/clear")
    public CommonResp<Object> clear(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        historyService.remove(new QueryWrapper<ReadingHistory>().eq("user_id", userId));
        return CommonResp.ok("清空成功", null);
    }

    // 更新阅读进度
    // 前端阅读页滚动时会把当前进度保存过来，方便下次继续阅读
    @Operation(summary = "更新阅读进度")
    @PostMapping("/progress")
    public CommonResp<Object> updateProgress(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        Long docId = Long.valueOf(String.valueOf(req.get("docId")));
        Integer progress = Integer.valueOf(String.valueOf(req.getOrDefault("progress", 0)));
        progress = Math.max(0, Math.min(progress, 100));

        Doc doc = docService.getById(docId);
        if (doc == null || doc.getEbookId() == null) {
            return CommonResp.fail("文档不存在");
        }

        ReadingHistory history = historyService.getOne(new QueryWrapper<ReadingHistory>()
                .eq("user_id", userId)
                .eq("doc_id", docId));
        if (history == null) {
            history = new ReadingHistory();
            history.setUserId(userId);
            history.setDocId(docId);
            history.setEbookId(doc.getEbookId());
        }
        history.setProgress(progress);
        history.setReadTime(LocalDateTime.now());
        historyService.saveOrUpdate(history);
        return CommonResp.ok("阅读进度已保存", null);
    }

    private void fillDisplayInfo(ReadingHistory history, Long userId) {
        Ebook ebook = ebookService.getById(history.getEbookId());
        Doc doc = docService.getById(history.getDocId());
        history.setEbookName(ebook == null ? "电子书已删除" : ebook.getName());
        history.setDocName(doc == null ? "文档已删除" : doc.getName());
        history.setProgress(calculateEbookProgress(userId, history.getEbookId()));
    }

    private Integer calculateEbookProgress(Long userId, Long ebookId) {
        if (userId == null || ebookId == null) {
            return 0;
        }

        List<Doc> docs = docService.listByEbookId(ebookId);
        List<Long> readableDocIds = findReadableLeafDocIds(docs);
        if (readableDocIds.isEmpty()) {
            return 0;
        }

        List<ReadingHistory> histories = historyService.list(new QueryWrapper<ReadingHistory>()
                .eq("user_id", userId)
                .eq("ebook_id", ebookId)
                .in("doc_id", readableDocIds));
        Set<Long> completedDocIds = histories.stream()
                .filter(history -> history.getProgress() != null && history.getProgress() >= 100)
                .map(ReadingHistory::getDocId)
                .collect(Collectors.toSet());

        return Math.min(100, Math.round(completedDocIds.size() * 100F / readableDocIds.size()));
    }

    private List<Long> findReadableLeafDocIds(List<Doc> docs) {
        Set<Long> parentIds = docs.stream()
                .map(Doc::getParent)
                .filter(parent -> parent != null && parent != 0)
                .collect(Collectors.toCollection(HashSet::new));

        return docs.stream()
                .filter(doc -> doc.getId() != null)
                .filter(doc -> !parentIds.contains(doc.getId()))
                .map(Doc::getId)
                .toList();
    }
}
