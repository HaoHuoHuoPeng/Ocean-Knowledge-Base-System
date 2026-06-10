// 文件说明：这个 Controller 负责评论相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.entity.SensitiveWord;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.entity.UserNotice;
import com.shiwangsi.oceanwiki.rep.BatchCommentReviewReq;
import com.shiwangsi.oceanwiki.rep.CommentReq;
import com.shiwangsi.oceanwiki.rep.CommentReviewReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.ISensitiveWordService;
import com.shiwangsi.oceanwiki.service.IUserCommentService;
import com.shiwangsi.oceanwiki.service.IUserNoticeService;
import com.shiwangsi.oceanwiki.service.IUserService;
import com.shiwangsi.oceanwiki.utils.AuthUtil;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 用户评论接口
// 普通用户可以评论电子书或文档，管理员可以审核命中敏感词的评论
@Tag(name = "用户评论接口")
@RestController
@RequestMapping("/comment")
public class CommentController {

    private static final String TARGET_EBOOK = "ebook";
    private static final String TARGET_DOC = "doc";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_DELETED = "deleted";
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String CONTENT_REVIEWER_CODE = "CONTENT_REVIEWER";

    // 数据库敏感词为空时使用这个兜底列表，避免新表还没执行 SQL 时评论审核失效
    private static final List<String> DEFAULT_SENSITIVE_WORDS = Arrays.asList(
            "违法", "赌博", "诈骗", "色情", "暴力", "辱骂", "广告"
    );

    private final IUserCommentService commentService;
    private final IUserNoticeService noticeService;
    private final IOperationLogService operationLogService;
    private final ISensitiveWordService sensitiveWordService;
    private final IUserService userService;
    private final IEbookService ebookService;
    private final IDocService docService;

    public CommentController(IUserCommentService commentService,
                             IUserNoticeService noticeService,
                             IOperationLogService operationLogService,
                             ISensitiveWordService sensitiveWordService,
                             IUserService userService,
                             IEbookService ebookService,
                             IDocService docService) {
        this.commentService = commentService;
        this.noticeService = noticeService;
        this.operationLogService = operationLogService;
        this.sensitiveWordService = sensitiveWordService;
        this.userService = userService;
        this.ebookService = ebookService;
        this.docService = docService;
    }

    // 查询公开评论
    // 阅读页只展示已发布评论，待审核、驳回、删除的评论都不会展示给普通用户
    @Operation(summary = "查询公开评论")
    @GetMapping("/public")
    public CommonResp<List<UserComment>> publicList(String targetType, Long targetId) {
        List<UserComment> list = commentService.list(new QueryWrapper<UserComment>()
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .eq("status", STATUS_PUBLISHED)
                .orderByDesc("create_time"));
        list.forEach(this::fillDisplayName);
        return CommonResp.ok(buildCommentTree(list));
    }

    // 发表评论
    // 没命中敏感词会直接发布，命中敏感词会进入待审核，避免敏感内容直接出现在页面上
    @Operation(summary = "发表评论")
    @PostMapping("/submit")
    public CommonResp<UserComment> submit(@RequestBody CommentReq req, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        if (!StringUtils.hasText(req.getContent())) {
            return CommonResp.fail("评论内容不能为空");
        }
        if (req.getContent().length() > 1000) {
            return CommonResp.fail("评论内容不能超过 1000 个字");
        }
        if (!targetExists(req.getTargetType(), req.getTargetId())) {
            return CommonResp.fail("评论对象不存在");
        }

        List<String> hits = findSensitiveWords(req.getContent());
        UserComment comment = new UserComment();
        comment.setUserId(userId);
        comment.setTargetType(req.getTargetType());
        comment.setTargetId(req.getTargetId());
        comment.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        comment.setContent(req.getContent());
        comment.setStatus(hits.isEmpty() ? STATUS_PUBLISHED : STATUS_PENDING);
        comment.setSensitiveHit(String.join(",", hits));
        comment.setCreateTime(LocalDateTime.now());
        commentService.save(comment);

        if (hits.isEmpty()) {
            return CommonResp.ok("评论发布成功", comment);
        }
        return CommonResp.ok("评论已提交，命中敏感词，等待管理员审核", comment);
    }

    // 查询我的评论
    // 用户可以看到自己发过的评论，以及每条评论当前是已发布、待审核还是被驳回
    @Operation(summary = "查询我的评论")
    @GetMapping("/my")
    public CommonResp<List<UserComment>> my(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        List<UserComment> list = commentService.list(new QueryWrapper<UserComment>()
                .eq("user_id", userId)
                .ne("status", STATUS_DELETED)
                .orderByDesc("create_time"));
        list.forEach(this::fillDisplayName);
        return CommonResp.ok(list);
    }

    // 管理员分页查询评论
    // status 不传表示查询全部未删除评论，传 pending 可以只看待审核评论
    @Operation(summary = "管理员分页查询评论")
    @GetMapping("/admin/page")
    public CommonResp<PageResp<UserComment>> adminPage(String status,
                                                       String keyword,
                                                       Long current,
                                                       Long pageSize,
                                                       HttpServletRequest request) {
        QueryWrapper<UserComment> wrapper = buildAdminQuery(resolveAdminQueryStatus(status, request), keyword);
        Page<UserComment> page = commentService.page(new Page<>(safeCurrent(current), safePageSize(pageSize)), wrapper);
        page.getRecords().forEach(this::fillDisplayName);
        return CommonResp.ok(PageResp.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    // 兼容旧前端接口
    @Operation(summary = "管理员查询评论")
    @GetMapping("/admin/list")
    public CommonResp<List<UserComment>> adminList(String status, String keyword, HttpServletRequest request) {
        List<UserComment> list = commentService.list(buildAdminQuery(resolveAdminQueryStatus(status, request), keyword));
        list.forEach(this::fillDisplayName);
        return CommonResp.ok(list);
    }

    // 管理员审核评论
    // 通过后评论变成已发布，驳回后只给作者自己看，不再公开展示
    @Operation(summary = "管理员审核评论")
    @PostMapping("/admin/review")
    public CommonResp<Object> review(@RequestBody CommentReviewReq req, HttpServletRequest request) {
        Long adminUserId = AuthUtil.requireLogin(request);
        UserComment comment = commentService.getById(req.getId());
        if (comment == null || STATUS_DELETED.equals(comment.getStatus())) {
            return CommonResp.fail("评论不存在");
        }
        if (!STATUS_PUBLISHED.equals(req.getStatus()) && !STATUS_REJECTED.equals(req.getStatus())) {
            return CommonResp.fail("审核状态只能是通过或驳回");
        }
        if (!StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("请填写审核原因");
        }
        if (hasReviewed(comment) && !isSuperAdmin(request)) {
            return CommonResp.fail("该评论已经处理过，不能再次更改");
        }

        reviewOneComment(comment, req.getStatus(), req.getRemark(), adminUserId);
        String resultText = reviewStatusText(req.getStatus());
        OperationLogUtil.save(operationLogService, adminUserId, "评论管理", resultText, "评论ID：" + comment.getId());
        return CommonResp.ok("审核成功", null);
    }

    // 管理员批量审核评论
    // 批量通过或驳回时，每条评论都会写入审核人、审核时间、审核备注，并通知评论作者
    @Operation(summary = "管理员批量审核评论")
    @PostMapping("/admin/batchReview")
    public CommonResp<Object> batchReview(@RequestBody BatchCommentReviewReq req, HttpServletRequest request) {
        Long adminUserId = AuthUtil.requireLogin(request);
        List<Long> ids = safeIds(req.getIds());
        if (ids.isEmpty()) {
            return CommonResp.fail("请选择要审核的评论");
        }
        if (!STATUS_PUBLISHED.equals(req.getStatus()) && !STATUS_REJECTED.equals(req.getStatus())) {
            return CommonResp.fail("审核状态只能是通过或驳回");
        }
        if (!StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("请填写审核原因");
        }

        List<UserComment> comments = commentService.listByIds(ids);
        if (comments.size() != ids.size()) {
            return CommonResp.fail("部分评论不存在，请刷新后重新选择");
        }
        boolean hasDeleted = comments.stream().anyMatch(item -> STATUS_DELETED.equals(item.getStatus()));
        if (hasDeleted) {
            return CommonResp.fail("已删除评论不能审核，请刷新后重新选择");
        }
        if (!isSuperAdmin(request) && comments.stream().anyMatch(this::hasReviewed)) {
            return CommonResp.fail("选中的评论里存在已处理评论，不能再次更改");
        }

        for (UserComment comment : comments) {
            reviewOneComment(comment, req.getStatus(), req.getRemark(), adminUserId);
        }
        OperationLogUtil.save(operationLogService, adminUserId, "评论管理", "批量" + reviewStatusText(req.getStatus()), "评论ID：" + ids);
        return CommonResp.ok("批量审核成功", null);
    }

    private void reviewOneComment(UserComment comment, String status, String remark, Long adminUserId) {
        comment.setStatus(status);
        comment.setReviewTime(LocalDateTime.now());
        comment.setReviewUserId(adminUserId);
        comment.setReviewRemark(remark);
        commentService.updateById(comment);

        String resultText = reviewStatusText(status);
        saveNotice(comment.getUserId(), "评论审核结果", "你的评论已被管理员" + resultText + "：" + safeRemark(remark));
    }

    private boolean hasReviewed(UserComment comment) {
        return STATUS_PUBLISHED.equals(comment.getStatus()) || STATUS_REJECTED.equals(comment.getStatus()) || comment.getReviewTime() != null;
    }

    private String reviewStatusText(String status) {
        return STATUS_PUBLISHED.equals(status) ? "审核通过" : "审核驳回";
    }

    // 删除评论
    // 评论作者可以删自己的评论；文档发布者可以删自己文章底下的评论；管理员可以删全部评论
    @Operation(summary = "删除评论")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        UserComment comment = commentService.getById(id);
        if (comment == null) {
            return CommonResp.fail("评论不存在");
        }

        boolean owner = userId.equals(comment.getUserId());
        boolean admin = isAdmin(request);
        boolean docAuthor = isDocAuthor(userId, comment);
        if (!owner && !docAuthor && !admin) {
            return CommonResp.fail("只能删除自己的评论或自己文档下的评论");
        }

        comment.setStatus(STATUS_DELETED);
        commentService.updateById(comment);
        OperationLogUtil.save(operationLogService, userId, "评论管理", "删除评论", "评论ID：" + comment.getId());
        return CommonResp.ok("删除成功", null);
    }

    private boolean isDocAuthor(Long userId, UserComment comment) {
        if (userId == null || comment == null || !TARGET_DOC.equals(comment.getTargetType())) {
            return false;
        }
        Doc doc = docService.getById(comment.getTargetId());
        return doc != null && userId.equals(doc.getCreateUserId());
    }

    private QueryWrapper<UserComment> buildAdminQuery(String status, String keyword) {
        QueryWrapper<UserComment> wrapper = new QueryWrapper<>();
        wrapper.ne("status", STATUS_DELETED);
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like("content", keyword);
        }
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    private List<String> findSensitiveWords(String content) {
        List<String> words = sensitiveWordService.list(new QueryWrapper<SensitiveWord>()
                        .eq("enabled", 1)
                        .orderByAsc("id"))
                .stream()
                .map(SensitiveWord::getWord)
                .filter(StringUtils::hasText)
                .toList();
        if (words.isEmpty()) {
            words = DEFAULT_SENSITIVE_WORDS;
        }
        String normalizedContent = normalizeSensitiveText(content);
        return words.stream()
                .filter(word -> normalizedContent.contains(normalizeSensitiveText(word)))
                .distinct()
                .toList();
    }

    private String normalizeSensitiveText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.toLowerCase().replaceAll("\\s+", "");
    }

    private List<UserComment> buildCommentTree(List<UserComment> comments) {
        Map<Long, UserComment> commentMap = comments.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(UserComment::getId, item -> item, (oldValue, newValue) -> oldValue));
        List<UserComment> roots = new ArrayList<>();
        for (UserComment comment : comments) {
            comment.setChildren(new ArrayList<>());
        }
        for (UserComment comment : comments) {
            Long parentId = comment.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(comment);
            } else if (commentMap.containsKey(parentId)) {
                commentMap.get(parentId).getChildren().add(comment);
            }
        }
        return roots;
    }

    private boolean targetExists(String targetType, Long targetId) {
        if (targetId == null) {
            return false;
        }
        if (TARGET_EBOOK.equals(targetType)) {
            return ebookService.getById(targetId) != null;
        }
        if (TARGET_DOC.equals(targetType)) {
            return docService.getById(targetId) != null;
        }
        return false;
    }

    private void fillDisplayName(UserComment comment) {
        User user = userService.getById(comment.getUserId());
        comment.setUserName(user == null ? "用户已删除" : user.getName());
        comment.setTargetName(getTargetName(comment.getTargetType(), comment.getTargetId()));
    }

    private String getTargetName(String targetType, Long targetId) {
        if (TARGET_EBOOK.equals(targetType)) {
            Ebook ebook = ebookService.getById(targetId);
            return ebook == null ? "电子书已删除" : ebook.getName();
        }
        if (TARGET_DOC.equals(targetType)) {
            Doc doc = docService.getById(targetId);
            return doc == null ? "文档已删除" : doc.getName();
        }
        return "未知对象";
    }

    private void saveNotice(Long userId, String title, String content) {
        UserNotice notice = new UserNotice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setReadFlag(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeService.save(notice);
    }

    private String safeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark : "无备注";
    }

    private List<Long> safeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    public CommonResp<PageResp<UserComment>> adminPage(String status,
                                                       String keyword,
                                                       Long current,
                                                       Long pageSize) {
        return adminPage(status, keyword, current, pageSize, null);
    }

    private boolean isAdmin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String token = request.getHeader("Authorization");
        com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.LoginUserInfo loginUserInfo =
                com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.get(token);
        if (loginUserInfo == null) {
            return false;
        }
        boolean superAdmin = loginUserInfo.roleCodes() != null && loginUserInfo.roleCodes().contains("SUPER_ADMIN");
        boolean commentAdmin = loginUserInfo.permissions() != null && loginUserInfo.permissions().contains("comment:manage");
        return superAdmin || commentAdmin;
    }

    private boolean isSuperAdmin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String token = request.getHeader("Authorization");
        com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.LoginUserInfo loginUserInfo =
                com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.get(token);
        return loginUserInfo != null
                && loginUserInfo.roleCodes() != null
                && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE);
    }

    private String resolveAdminQueryStatus(String status, HttpServletRequest request) {
        return isContentReviewer(request) && !isSuperAdmin(request) ? STATUS_PENDING : status;
    }

    private boolean isContentReviewer(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String token = request.getHeader("Authorization");
        com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.LoginUserInfo loginUserInfo =
                com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.get(token);
        return loginUserInfo != null
                && loginUserInfo.roleCodes() != null
                && loginUserInfo.roleCodes().contains(CONTENT_REVIEWER_CODE);
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
}
