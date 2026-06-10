// 文件说明：这个 Controller 负责电子书相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiwangsi.oceanwiki.entity.Category;
import com.shiwangsi.oceanwiki.entity.DocVote;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.entity.UserFavorite;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.exception.BusinessExceptionCode;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.ListByPageResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import com.shiwangsi.oceanwiki.service.ICategoryService;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IDocVoteService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.IReadingHistoryService;
import com.shiwangsi.oceanwiki.service.IUserCommentService;
import com.shiwangsi.oceanwiki.service.IUserFavoriteService;
import com.shiwangsi.oceanwiki.utils.AuthUtil;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 电子书管理接口
// 首页展示和后台电子书管理都调用这里
@Tag(name = "电子书管理接口")
@RestController
@RequestMapping("/ebook")
public class EbookController {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_OFFLINE = "offline";
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String EBOOK_MANAGE_PERMISSION = "ebook:manage";
    private static final String EBOOK_REVIEW_PERMISSION = "ebook:review";

    private final IEbookService ebookService;
    private final ICategoryService categoryService;
    private final IDocService docService;
    private final IOperationLogService operationLogService;
    private final IReadingHistoryService historyService;
    private final IUserFavoriteService favoriteService;
    private final IDocVoteService docVoteService;
    private final IUserCommentService commentService;

    public EbookController(IEbookService ebookService,
                           ICategoryService categoryService,
                           IDocService docService,
                           IOperationLogService operationLogService,
                           IReadingHistoryService historyService,
                           IUserFavoriteService favoriteService,
                           IDocVoteService docVoteService,
                           IUserCommentService commentService) {
        this.ebookService = ebookService;
        this.categoryService = categoryService;
        this.docService = docService;
        this.operationLogService = operationLogService;
        this.historyService = historyService;
        this.favoriteService = favoriteService;
        this.docVoteService = docVoteService;
        this.commentService = commentService;
    }

    // 查询电子书列表
    // name 为空时查询全部；categoryId 不为空时按当前分类及其所有子分类过滤
    @Operation(summary = "查询电子书列表")
    @GetMapping("/list")
    public CommonResp<List<Ebook>> list(String name, Long categoryId) {
        QueryWrapper<Ebook> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like("name", name);
        }
        applyCategoryFilter(wrapper, categoryId);
        wrapper.eq("status", STATUS_PUBLISHED);
        wrapper.orderByAsc("category_id", "category1_id", "category2_id", "id");
        return CommonResp.ok(ebookService.list(wrapper));
    }

    // 推荐电子书
    // 这里使用协同过滤：阅读、点赞、收藏会被转换成“用户-电子书评分矩阵”
    // 基于用户的协同过滤：先找和当前用户行为相似的用户，再推荐这些相似用户喜欢的电子书
    // 基于物品的协同过滤：先找当前用户看过、赞过、收藏过的电子书，再推荐和这些电子书相似的电子书
    @Operation(summary = "推荐电子书")
    @GetMapping("/recommend")
    public CommonResp<List<Ebook>> recommend(Integer limit, HttpServletRequest request) {
        List<Ebook> publishedEbooks = ebookService.list(new QueryWrapper<Ebook>()
                .eq("status", STATUS_PUBLISHED));
        if (publishedEbooks.isEmpty()) {
            return CommonResp.ok(List.of());
        }

        int safeLimit = limit == null || limit < 1 ? 3 : Math.min(limit, 12);
        Map<Long, Ebook> publishedEbookMap = publishedEbooks.stream()
                .collect(Collectors.toMap(Ebook::getId, item -> item));
        Long currentUserId = AuthUtil.getCurrentUserId(request);
        Map<Long, Map<Long, Double>> userScoreMatrix = buildUserScoreMatrix(publishedEbookMap);

        if (currentUserId == null || !userScoreMatrix.containsKey(currentUserId)) {
            return CommonResp.ok(buildColdStartRecommend(publishedEbooks, safeLimit));
        }

        Map<Long, Double> currentUserScores = userScoreMatrix.get(currentUserId);
        Set<Long> operatedEbookIds = currentUserScores.keySet();
        Map<Long, Double> userBasedScores = calculateUserBasedScores(currentUserId, userScoreMatrix, operatedEbookIds, publishedEbookMap.keySet());
        Map<Long, Double> itemBasedScores = calculateItemBasedScores(currentUserScores, userScoreMatrix, operatedEbookIds, publishedEbookMap.keySet());
        Map<Long, Double> mixedScores = new HashMap<>();

        userBasedScores.forEach((ebookId, score) -> addScore(mixedScores, ebookId, score * 0.6));
        itemBasedScores.forEach((ebookId, score) -> addScore(mixedScores, ebookId, score * 0.4));

        if (mixedScores.isEmpty()) {
            return CommonResp.ok(buildColdStartRecommend(publishedEbooks, safeLimit));
        }

        Set<Ebook> result = mixedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(safeLimit)
                .map(item -> {
                    Ebook ebook = publishedEbookMap.get(item.getKey());
                    ebook.setRecommendScore(roundScore(item.getValue()));
                    return ebook;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 协同过滤结果不足时，用冷启动结果补齐，避免用户看到的推荐数量太少
        for (Ebook ebook : buildColdStartRecommend(publishedEbooks, safeLimit)) {
            if (result.size() >= safeLimit) {
                break;
            }
            if (!operatedEbookIds.contains(ebook.getId())) {
                result.add(ebook);
            }
        }

        return CommonResp.ok(result.stream().limit(safeLimit).collect(Collectors.toList()));
    }

    // 按条件分页查询电子书信息
    // 后台电子书管理页使用这个接口，支持名称、任意层级分类、状态组合查询
    @Operation(summary = "按条件分页查询电子书")
    @GetMapping("/page")
    public CommonResp<PageResp<Ebook>> page(String name,
                                            Long categoryId,
                                            String status,
                                            Long current,
                                            Long pageSize,
                                            HttpServletRequest request) {
        QueryWrapper<Ebook> wrapper = buildEbookQuery(name, categoryId, resolveAdminQueryStatus(status, request));
        Page<Ebook> page = ebookService.page(new Page<>(safeCurrent(current), safePageSize(pageSize)), wrapper);
        return CommonResp.ok(PageResp.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    // 通过地址参数分页查询电子书
    // 访问示例：http://localhost:8080/ebook/listByPage?page=1&size=3
    // 这里没有用 json 请求体，page、size、name、categoryId 都是地址栏后面的查询参数
    @Operation(summary = "通过地址参数分页查询电子书")
    @GetMapping("/listByPage")
    public CommonResp<ListByPageResp<Ebook>> listByPage(Integer page,
                                                        Integer size,
                                                        String name,
                                                        Long categoryId,
                                                        String status,
                                                        HttpServletRequest request) {
        QueryWrapper<Ebook> wrapper = buildEbookQuery(name, categoryId, resolveAdminQueryStatus(status, request));
        Page<Ebook> pageResult = ebookService.page(new Page<>(safeCurrent(page), safePageSize(size)), wrapper);
        return CommonResp.ok("查询成功", ListByPageResp.of(pageResult.getTotal(), pageResult.getRecords()));
    }

    // 导出电子书 CSV
    // CSV 可以直接用 Excel 打开，适合后台管理员临时导出数据
    @Operation(summary = "导出电子书 CSV")
    @GetMapping("/export")
    public void export(String name, Long categoryId, String status, HttpServletResponse response) throws IOException {
        List<Ebook> list = ebookService.list(buildEbookQuery(name, categoryId, status));
        writeCsvResponse(response, "电子书列表.csv");
        response.getWriter().println("ID,名称,分类ID,状态,文档数,阅读数,点赞数,下架原因");
        for (Ebook ebook : list) {
            response.getWriter().println(csv(ebook.getId())
                    + "," + csv(ebook.getName())
                    + "," + csv(ebook.getCategoryId())
                    + "," + csv(ebook.getStatus())
                    + "," + csv(ebook.getDocCount())
                    + "," + csv(ebook.getViewCount())
                    + "," + csv(ebook.getVoteCount())
                    + "," + csv(ebook.getOfflineReason()));
        }
    }

    // 搜索电子书
    // keyword 会同时匹配电子书名称、简介和已发布文档标题
    @Operation(summary = "搜索电子书")
    @GetMapping("/search")
    public CommonResp<List<Ebook>> search(String keyword, Long categoryId) {
        if (!StringUtils.hasText(keyword)) {
            return list(null, categoryId);
        }

        List<Long> ebookIds = docService.list(new QueryWrapper<Doc>()
                        .select("ebook_id")
                        .like("name", keyword)
                        .eq("status", "published"))
                .stream()
                .map(Doc::getEbookId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper<Ebook> wrapper = new QueryWrapper<>();
        wrapper.and(item -> {
            item.like("name", keyword).or().like("description", keyword);
            if (!ebookIds.isEmpty()) {
                item.or().in("id", ebookIds);
            }
        });
        applyCategoryFilter(wrapper, categoryId);
        wrapper.eq("status", STATUS_PUBLISHED);
        wrapper.orderByAsc("category_id", "category1_id", "category2_id", "id");
        return CommonResp.ok(ebookService.list(wrapper));
    }

    public CommonResp<ListByPageResp<Ebook>> listByPage(Integer page,
                                                        Integer size,
                                                        String name,
                                                        Long categoryId,
                                                        String status) {
        return listByPage(page, size, name, categoryId, status, null);
    }

    private QueryWrapper<Ebook> buildEbookQuery(String name, Long categoryId, String status) {
        QueryWrapper<Ebook> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like("name", name);
        }
        applyCategoryFilter(wrapper, categoryId);
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("category_id", "category1_id", "category2_id", "id");
        return wrapper;
    }

    private String resolveAdminQueryStatus(String status, HttpServletRequest request) {
        if (isReviewOnlyUser(request)) {
            return STATUS_PENDING;
        }
        return status;
    }

    private boolean isReviewOnlyUser(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        var loginUserInfo = AuthUtil.getLoginUserInfo(request);
        if (loginUserInfo == null) {
            return false;
        }
        if (loginUserInfo.roleCodes() != null && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE)) {
            return false;
        }
        boolean canManage = loginUserInfo.permissions() != null && loginUserInfo.permissions().contains(EBOOK_MANAGE_PERMISSION);
        boolean canReview = loginUserInfo.permissions() != null && loginUserInfo.permissions().contains(EBOOK_REVIEW_PERMISSION);
        return canReview && !canManage;
    }

    private void applyCategoryFilter(QueryWrapper<Ebook> wrapper, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        List<Long> categoryIds = findCategoryAndChildrenIds(categoryId);
        wrapper.and(item -> item.in("category_id", categoryIds)
                .or()
                .in("category2_id", categoryIds)
                .or()
                .in("category1_id", categoryIds));
    }

    private List<Long> findCategoryAndChildrenIds(Long categoryId) {
        List<Category> categories = categoryService.list();
        Set<Long> result = new LinkedHashSet<>();
        collectCategoryIds(categoryId, categories, result);
        return result.stream().collect(Collectors.toList());
    }

    private void collectCategoryIds(Long categoryId, List<Category> categories, Set<Long> result) {
        if (categoryId == null || result.contains(categoryId)) {
            return;
        }
        result.add(categoryId);
        for (Category category : categories) {
            if (categoryId.equals(category.getParent())) {
                collectCategoryIds(category.getId(), categories, result);
            }
        }
    }

    private List<Ebook> buildColdStartRecommend(List<Ebook> publishedEbooks, int limit) {
        int maxViewCount = publishedEbooks.stream().mapToInt(item -> safeNumber(item.getViewCount())).max().orElse(0);
        int maxVoteCount = publishedEbooks.stream().mapToInt(item -> safeNumber(item.getVoteCount())).max().orElse(0);
        int maxDocCount = publishedEbooks.stream().mapToInt(item -> safeNumber(item.getDocCount())).max().orElse(0);
        return publishedEbooks.stream()
                .peek(item -> item.setRecommendScore(calculateColdStartScore(item, maxViewCount, maxVoteCount, maxDocCount)))
                .sorted(Comparator
                        .comparing(Ebook::getRecommendScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(item -> safeNumber(item.getVoteCount()), Comparator.reverseOrder())
                        .thenComparing(item -> safeNumber(item.getViewCount()), Comparator.reverseOrder())
                        .thenComparing(Ebook::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Map<Long, Map<Long, Double>> buildUserScoreMatrix(Map<Long, Ebook> publishedEbookMap) {
        Map<Long, Long> docEbookMap = docService.list(new QueryWrapper<Doc>().select("id", "ebook_id")).stream()
                .filter(item -> item.getId() != null && item.getEbookId() != null)
                .collect(Collectors.toMap(Doc::getId, Doc::getEbookId, (oldValue, newValue) -> oldValue));
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        for (ReadingHistory history : historyService.list()) {
            addUserEbookScore(matrix, history.getUserId(), history.getEbookId(), publishedEbookMap, 1.0);
        }

        for (UserFavorite favorite : favoriteService.list()) {
            Long ebookId = resolveFavoriteEbookId(favorite, docEbookMap);
            addUserEbookScore(matrix, favorite.getUserId(), ebookId, publishedEbookMap, 3.0);
        }

        for (DocVote docVote : docVoteService.list()) {
            Long ebookId = docEbookMap.get(docVote.getDocId());
            addUserEbookScore(matrix, docVote.getUserId(), ebookId, publishedEbookMap, 2.0);
        }

        return matrix;
    }

    private Long resolveFavoriteEbookId(UserFavorite favorite, Map<Long, Long> docEbookMap) {
        if ("ebook".equals(favorite.getTargetType())) {
            return favorite.getTargetId();
        }
        if ("doc".equals(favorite.getTargetType())) {
            return docEbookMap.get(favorite.getTargetId());
        }
        return null;
    }

    private void addUserEbookScore(Map<Long, Map<Long, Double>> matrix,
                                   Long userId,
                                   Long ebookId,
                                   Map<Long, Ebook> publishedEbookMap,
                                   double score) {
        if (userId == null || ebookId == null || !publishedEbookMap.containsKey(ebookId)) {
            return;
        }
        matrix.computeIfAbsent(userId, key -> new HashMap<>()).merge(ebookId, score, Double::sum);
    }

    private Map<Long, Double> calculateUserBasedScores(Long currentUserId,
                                                       Map<Long, Map<Long, Double>> userScoreMatrix,
                                                       Set<Long> operatedEbookIds,
                                                       Set<Long> publishedEbookIds) {
        Map<Long, Double> currentScores = userScoreMatrix.get(currentUserId);
        Map<Long, Double> result = new HashMap<>();
        userScoreMatrix.forEach((otherUserId, otherScores) -> {
            if (currentUserId.equals(otherUserId)) {
                return;
            }
            double similarity = cosineSimilarity(currentScores, otherScores);
            if (similarity <= 0) {
                return;
            }
            otherScores.forEach((ebookId, score) -> {
                if (!operatedEbookIds.contains(ebookId) && publishedEbookIds.contains(ebookId)) {
                    addScore(result, ebookId, similarity * score);
                }
            });
        });
        return result;
    }

    private Map<Long, Double> calculateItemBasedScores(Map<Long, Double> currentUserScores,
                                                       Map<Long, Map<Long, Double>> userScoreMatrix,
                                                       Set<Long> operatedEbookIds,
                                                       Set<Long> publishedEbookIds) {
        Map<Long, Map<Long, Double>> itemScoreMatrix = buildItemScoreMatrix(userScoreMatrix);
        Map<Long, Double> result = new HashMap<>();
        for (Long candidateEbookId : publishedEbookIds) {
            if (operatedEbookIds.contains(candidateEbookId)) {
                continue;
            }
            Map<Long, Double> candidateScores = itemScoreMatrix.get(candidateEbookId);
            if (candidateScores == null) {
                continue;
            }
            double totalScore = 0;
            for (Map.Entry<Long, Double> operatedItem : currentUserScores.entrySet()) {
                Map<Long, Double> operatedItemScores = itemScoreMatrix.get(operatedItem.getKey());
                double similarity = cosineSimilarity(candidateScores, operatedItemScores);
                totalScore += similarity * operatedItem.getValue();
            }
            if (totalScore > 0) {
                result.put(candidateEbookId, totalScore);
            }
        }
        return result;
    }

    private Map<Long, Map<Long, Double>> buildItemScoreMatrix(Map<Long, Map<Long, Double>> userScoreMatrix) {
        Map<Long, Map<Long, Double>> itemScoreMatrix = new HashMap<>();
        userScoreMatrix.forEach((userId, ebookScores) -> ebookScores.forEach((ebookId, score) ->
                itemScoreMatrix.computeIfAbsent(ebookId, key -> new HashMap<>()).put(userId, score)));
        return itemScoreMatrix;
    }

    private double cosineSimilarity(Map<Long, Double> left, Map<Long, Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        double dotProduct = 0;
        double leftLength = 0;
        double rightLength = 0;
        for (Double score : left.values()) {
            leftLength += score * score;
        }
        for (Double score : right.values()) {
            rightLength += score * score;
        }
        for (Map.Entry<Long, Double> entry : left.entrySet()) {
            dotProduct += entry.getValue() * right.getOrDefault(entry.getKey(), 0.0);
        }
        if (leftLength <= 0 || rightLength <= 0) {
            return 0;
        }
        return dotProduct / (Math.sqrt(leftLength) * Math.sqrt(rightLength));
    }

    private void addScore(Map<Long, Double> scoreMap, Long ebookId, double score) {
        scoreMap.merge(ebookId, score, Double::sum);
    }

    private double calculateColdStartScore(Ebook ebook, int maxViewCount, int maxVoteCount, int maxDocCount) {
        double viewScore = normalize(safeNumber(ebook.getViewCount()), maxViewCount);
        double voteScore = normalize(safeNumber(ebook.getVoteCount()), maxVoteCount);
        double docScore = normalize(safeNumber(ebook.getDocCount()), maxDocCount);
        return roundScore(voteScore * 50 + viewScore * 35 + docScore * 15);
    }

    private double roundScore(double score) {
        return Math.round(score * 100.0) / 100.0;
    }

    private double normalize(int value, int maxValue) {
        if (maxValue <= 0) {
            return 0;
        }
        return (double) value / maxValue;
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value;
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

    private long safeCurrent(Integer current) {
        return current == null || current < 1 ? 1 : current;
    }

    private long safePageSize(Integer pageSize) {
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

    // 保存电子书
    // 前端新增和修改都走这个接口：id 为空是新增，id 有值是修改
    @Operation(summary = "保存电子书")
    @PostMapping("/save")
    public CommonResp<Object> save(@RequestBody Ebook ebook, HttpServletRequest request) {
        boolean create = ebook.getId() == null;
        Ebook oldEbook = create ? null : ebookService.getById(ebook.getId());
        if (!create && oldEbook == null) {
            throw new BusinessException(BusinessExceptionCode.EBOOK_NOT_FOUND);
        }
        if (create) {
            ebook.setStatus(STATUS_DRAFT);
            ebook.setReviewRemark(null);
            ebook.setOfflineReason(null);
        } else {
            // 编辑电子书只改基础资料，不允许通过保存接口绕过审核流程直接改发布状态
            ebook.setStatus(oldEbook.getStatus());
            ebook.setReviewRemark(oldEbook.getReviewRemark());
            ebook.setOfflineReason(oldEbook.getOfflineReason());
        }
        fillCompatibleCategoryFields(ebook);
        ebookService.saveOrUpdate(ebook);
        OperationLogUtil.save(operationLogService,
                request,
                "电子书管理",
                create ? "新增电子书" : "修改电子书",
                ebook.getName(),
                formatEbookLog(oldEbook),
                formatEbookLog(ebook));
        return CommonResp.ok("保存成功", null);
    }

    // 提交审核
    // 科研人员把草稿、已驳回或已下架的电子书提交给内容审核员处理
    @Operation(summary = "提交电子书审核")
    @PostMapping("/submitReview/{id}")
    public CommonResp<Object> submitReview(@PathVariable Long id, HttpServletRequest request) {
        Ebook ebook = getExistingEbook(id);
        String oldStatus = ebook.getStatus();
        if (STATUS_PENDING.equals(oldStatus)) {
            return CommonResp.ok("已经是待审核状态", null);
        }
        if (STATUS_PUBLISHED.equals(oldStatus)) {
            return CommonResp.fail("已发布电子书不需要提交审核，如需停止展示请先下架");
        }
        ebook.setStatus(STATUS_PENDING);
        ebook.setReviewRemark(null);
        ebookService.updateById(ebook);
        OperationLogUtil.save(operationLogService,
                request,
                "电子书审核",
                "提交审核",
                ebook.getName(),
                "状态：" + oldStatus,
                "状态：" + ebook.getStatus());
        return CommonResp.ok("提交审核成功", null);
    }

    // 审核电子书
    // 内容审核员只能在待审核状态下审核，通过后前台可见，驳回后科研人员需要修改后重新提交
    @Operation(summary = "审核电子书")
    @PostMapping("/review")
    public CommonResp<Object> review(@RequestBody EbookReviewReq req, HttpServletRequest request) {
        Ebook ebook = getExistingEbook(req.getId());
        if (!STATUS_PENDING.equals(ebook.getStatus())) {
            return CommonResp.fail("只有待审核的电子书才能审核");
        }
        if (!STATUS_PUBLISHED.equals(req.getStatus()) && !STATUS_REJECTED.equals(req.getStatus())) {
            return CommonResp.fail("审核结果只能是通过或驳回");
        }
        if (STATUS_REJECTED.equals(req.getStatus()) && !StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("审核驳回必须填写原因");
        }
        String oldStatus = ebook.getStatus();
        ebook.setStatus(req.getStatus());
        ebook.setReviewRemark(trimToNull(req.getRemark()));
        ebook.setOfflineReason(null);
        ebookService.updateById(ebook);
        OperationLogUtil.save(operationLogService,
                request,
                "电子书审核",
                STATUS_PUBLISHED.equals(req.getStatus()) ? "审核通过" : "审核驳回",
                ebook.getName(),
                "状态：" + oldStatus,
                "状态：" + ebook.getStatus() + "，审核备注：" + emptyText(ebook.getReviewRemark()));
        return CommonResp.ok("审核处理成功", null);
    }

    // 下架电子书
    // 已发布电子书下架后不会出现在首页和阅读入口，后续可以重新提交审核
    @Operation(summary = "下架电子书")
    @PostMapping("/offline")
    public CommonResp<Object> offline(@RequestBody EbookReviewReq req, HttpServletRequest request) {
        Ebook ebook = getExistingEbook(req.getId());
        if (!STATUS_PUBLISHED.equals(ebook.getStatus())) {
            return CommonResp.fail("只有已发布的电子书才能下架");
        }
        if (!StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("下架必须填写原因");
        }
        String oldStatus = ebook.getStatus();
        ebook.setStatus(STATUS_OFFLINE);
        ebook.setOfflineReason(req.getRemark().trim());
        ebook.setReviewRemark(null);
        ebookService.updateById(ebook);
        OperationLogUtil.save(operationLogService,
                request,
                "电子书管理",
                "下架电子书",
                ebook.getName(),
                "状态：" + oldStatus,
                "状态：" + ebook.getStatus() + "，下架原因：" + ebook.getOfflineReason());
        return CommonResp.ok("下架成功", null);
    }

    private String formatEbookLog(Ebook ebook) {
        if (ebook == null) {
            return null;
        }
        return "名称：" + ebook.getName()
                + "，分类：" + ebook.getCategoryId()
                + "，状态：" + ebook.getStatus()
                + "，审核备注：" + emptyText(ebook.getReviewRemark())
                + "，下架原因：" + emptyText(ebook.getOfflineReason());
    }

    private Ebook getExistingEbook(Long id) {
        Ebook ebook = ebookService.getById(id);
        if (ebook == null) {
            throw new BusinessException(BusinessExceptionCode.EBOOK_NOT_FOUND);
        }
        return ebook;
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private String emptyText(String text) {
        return StringUtils.hasText(text) ? text : "无";
    }

    // 电子书审核、下架请求参数
    // status 用来接收审核结果，remark 用来接收审核说明或下架原因
    public static class EbookReviewReq {
        private Long id;
        private String status;
        private String remark;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    private void fillCompatibleCategoryFields(Ebook ebook) {
        if (ebook.getCategoryId() == null) {
            ebook.setCategoryId(ebook.getCategory2Id() == null ? ebook.getCategory1Id() : ebook.getCategory2Id());
        }
        List<Long> path = findCategoryPath(ebook.getCategoryId());
        if (!path.isEmpty()) {
            ebook.setCategory1Id(path.get(0));
            ebook.setCategory2Id(path.size() > 1 ? path.get(1) : null);
        }
    }

    private List<Long> findCategoryPath(Long categoryId) {
        Map<Long, Category> categoryMap = categoryService.list().stream()
                .collect(Collectors.toMap(Category::getId, item -> item, (oldValue, newValue) -> oldValue));
        LinkedHashSet<Long> reversedPath = new LinkedHashSet<>();
        Long currentId = categoryId;
        while (currentId != null && currentId != 0 && categoryMap.containsKey(currentId)) {
            reversedPath.add(currentId);
            currentId = categoryMap.get(currentId).getParent();
        }
        List<Long> path = reversedPath.stream().collect(Collectors.toList());
        java.util.Collections.reverse(path);
        return path;
    }

    // 删除电子书
    @Operation(summary = "删除电子书")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Ebook ebook = ebookService.getById(id);
        if (ebook == null) {
            throw new BusinessException(BusinessExceptionCode.EBOOK_NOT_FOUND);
        }
        checkCanDeleteEbook(id);
        ebookService.removeById(id);
        ebookService.refreshEbookInfo();
        OperationLogUtil.save(operationLogService, request, "电子书管理", "删除电子书", ebook == null ? "ID：" + id : ebook.getName());
        return CommonResp.ok("删除成功", null);
    }

    private void checkCanDeleteEbook(Long ebookId) {
        long docCount = docService.count(new QueryWrapper<Doc>().eq("ebook_id", ebookId));
        if (docCount > 0) {
            throw new BusinessException(BusinessExceptionCode.EBOOK_HAS_DOC);
        }

        long favoriteCount = favoriteService.count(new QueryWrapper<UserFavorite>()
                .eq("target_type", "ebook")
                .eq("target_id", ebookId));
        long historyCount = historyService.count(new QueryWrapper<ReadingHistory>().eq("ebook_id", ebookId));
        long commentCount = commentService.count(new QueryWrapper<UserComment>()
                .eq("target_type", "ebook")
                .eq("target_id", ebookId)
                .ne("status", "deleted"));
        if (favoriteCount + historyCount + commentCount > 0) {
            throw new BusinessException(BusinessExceptionCode.EBOOK_HAS_USER_DATA);
        }
    }

    // 手动刷新电子书统计字段
    // 你改过 doc 数据后，也可以在 Knife4j 里点这个接口手动刷新
    @Operation(summary = "刷新电子书统计")
    @GetMapping("/refresh")
    public CommonResp<Object> refresh(HttpServletRequest request) {
        ebookService.refreshEbookInfo();
        OperationLogUtil.save(operationLogService, request, "电子书管理", "刷新电子书统计", "刷新 doc_count、view_count、vote_count");
        return CommonResp.ok("刷新成功", null);
    }
}
