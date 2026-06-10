// 文件说明：这个 Controller 负责后台看板统计接口，给管理员首页展示系统关键数据。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.entity.Category;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.entity.UserFeedback;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.ICategoryService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IEbookSnapshotService;
import com.shiwangsi.oceanwiki.service.IReadingHistoryService;
import com.shiwangsi.oceanwiki.service.IUserCommentService;
import com.shiwangsi.oceanwiki.service.IUserFeedbackService;
import com.shiwangsi.oceanwiki.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

// 后台看板接口
// 这里只放超级管理员需要看的系统管理数据，不放普通用户阅读推荐内容
@Tag(name = "后台看板接口")
@RestController
@RequestMapping("/dashboard")
public class AdminDashboardController {

    private final IEbookService ebookService;
    private final ICategoryService categoryService;
    private final IDocService docService;
    private final IUserService userService;
    private final IUserCommentService commentService;
    private final IUserFeedbackService feedbackService;
    private final IReadingHistoryService historyService;
    private final IEbookSnapshotService snapshotService;

    public AdminDashboardController(IEbookService ebookService,
                                    ICategoryService categoryService,
                                    IDocService docService,
                                    IUserService userService,
                                    IUserCommentService commentService,
                                    IUserFeedbackService feedbackService,
                                    IReadingHistoryService historyService,
                                    IEbookSnapshotService snapshotService) {
        this.ebookService = ebookService;
        this.categoryService = categoryService;
        this.docService = docService;
        this.userService = userService;
        this.commentService = commentService;
        this.feedbackService = feedbackService;
        this.historyService = historyService;
        this.snapshotService = snapshotService;
    }

    // 查询后台看板总览
    @Operation(summary = "查询后台看板")
    @GetMapping("/overview")
    public CommonResp<Map<String, Object>> overview() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        result.put("ebookCount", ebookService.count());
        result.put("publishedEbookCount", ebookService.count(new QueryWrapper<Ebook>().eq("status", "published")));
        result.put("pendingEbookCount", ebookService.count(new QueryWrapper<Ebook>().eq("status", "pending")));
        result.put("docCount", docService.count(leafDocWrapper()));
        result.put("userCount", userService.count());
        result.put("pendingCommentCount", commentService.count(new QueryWrapper<UserComment>().eq("status", "pending")));
        result.put("openFeedbackCount", feedbackService.count(new QueryWrapper<UserFeedback>().eq("status", "open")));
        result.put("todayReadCount", historyService.count(new QueryWrapper<ReadingHistory>().ge("read_time", todayStart)));
        result.put("pendingDocCount", docService.count(leafDocWrapper().eq("status", "pending")));
        result.put("trend", snapshotService.get30Statistic());
        result.put("todoStats", buildTodoStats(result));
        result.put("ebookStatusStats", buildEbookStatusStats());
        result.put("categoryRank", buildCategoryRank());

        return CommonResp.ok(result);
    }

    private QueryWrapper<Doc> leafDocWrapper() {
        // 只统计真正的内容文档，不统计作为目录使用的父文档
        return new QueryWrapper<Doc>().notInSql("id", "select parent from doc where parent <> 0");
    }

    private List<Map<String, Object>> buildTodoStats(Map<String, Object> overview) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(chartItem("待审核电子书", overview.get("pendingEbookCount")));
        list.add(chartItem("待审核文档", overview.get("pendingDocCount")));
        list.add(chartItem("待审核评论", overview.get("pendingCommentCount")));
        list.add(chartItem("待处理反馈", overview.get("openFeedbackCount")));
        return list;
    }

    private List<Map<String, Object>> buildEbookStatusStats() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(chartItem("草稿", ebookService.count(new QueryWrapper<Ebook>().eq("status", "draft"))));
        list.add(chartItem("待审核", ebookService.count(new QueryWrapper<Ebook>().eq("status", "pending"))));
        list.add(chartItem("已发布", ebookService.count(new QueryWrapper<Ebook>().eq("status", "published"))));
        list.add(chartItem("已下架", ebookService.count(new QueryWrapper<Ebook>().eq("status", "offline"))));
        return list;
    }

    private List<Map<String, Object>> buildCategoryRank() {
        List<Category> categories = categoryService.list();
        Map<Long, Category> categoryMap = categories.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Category::getId, item -> item, (oldValue, newValue) -> oldValue));
        Map<Long, Long> countMap = new LinkedHashMap<>();

        for (Ebook ebook : ebookService.list()) {
            Long categoryId = ebook.getCategoryId() != null
                    ? ebook.getCategoryId()
                    : (ebook.getCategory2Id() != null ? ebook.getCategory2Id() : ebook.getCategory1Id());
            if (categoryId == null) {
                continue;
            }
            Long rootCategoryId = findRootCategoryId(categoryId, categoryMap);
            countMap.merge(rootCategoryId, 1L, Long::sum);
        }

        return countMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .map(item -> chartItem(categoryMap.containsKey(item.getKey()) ? categoryMap.get(item.getKey()).getName() : "未分类", item.getValue()))
                .collect(Collectors.toList());
    }

    private Long findRootCategoryId(Long categoryId, Map<Long, Category> categoryMap) {
        Category current = categoryMap.get(categoryId);
        while (current != null && current.getParent() != null && current.getParent() != 0 && categoryMap.containsKey(current.getParent())) {
            current = categoryMap.get(current.getParent());
        }
        return current == null ? categoryId : current.getId();
    }

    private Map<String, Object> chartItem(String name, Object value) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value == null ? 0 : value);
        return item;
    }
}
