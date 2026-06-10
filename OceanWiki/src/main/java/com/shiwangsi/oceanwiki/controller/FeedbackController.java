// 文件说明：这个 Controller 负责反馈相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiwangsi.oceanwiki.entity.FeedbackReply;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.entity.UserFeedback;
import com.shiwangsi.oceanwiki.entity.UserNotice;
import com.shiwangsi.oceanwiki.rep.BatchFeedbackHandleReq;
import com.shiwangsi.oceanwiki.rep.FeedbackHandleReq;
import com.shiwangsi.oceanwiki.rep.FeedbackReplyReq;
import com.shiwangsi.oceanwiki.rep.FeedbackReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import com.shiwangsi.oceanwiki.service.IFeedbackReplyService;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.IUserFeedbackService;
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
import java.util.List;
import java.util.stream.Collectors;

// 用户反馈接口
// 普通用户可以提交建议或纠错，管理员处理和回复后会给用户发送通知
@Tag(name = "用户反馈接口")
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private static final String STATUS_OPEN = "open";
    private static final String STATUS_HANDLED = "handled";
    private static final String STATUS_REJECTED = "rejected";
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String CONTENT_REVIEWER_CODE = "CONTENT_REVIEWER";

    private final IUserFeedbackService feedbackService;
    private final IFeedbackReplyService feedbackReplyService;
    private final IUserNoticeService noticeService;
    private final IOperationLogService operationLogService;
    private final IUserService userService;

    public FeedbackController(IUserFeedbackService feedbackService,
                              IFeedbackReplyService feedbackReplyService,
                              IUserNoticeService noticeService,
                              IOperationLogService operationLogService,
                              IUserService userService) {
        this.feedbackService = feedbackService;
        this.feedbackReplyService = feedbackReplyService;
        this.noticeService = noticeService;
        this.operationLogService = operationLogService;
        this.userService = userService;
    }

    // 提交反馈
    @Operation(summary = "提交反馈")
    @PostMapping("/submit")
    public CommonResp<Object> submit(@RequestBody FeedbackReq req, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        if (!StringUtils.hasText(req.getTitle()) || !StringUtils.hasText(req.getContent())) {
            return CommonResp.fail("请填写反馈标题和内容");
        }

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setTargetType(req.getTargetType());
        feedback.setTargetId(req.getTargetId());
        feedback.setType(StringUtils.hasText(req.getType()) ? req.getType() : "suggestion");
        feedback.setTitle(req.getTitle());
        feedback.setContent(req.getContent());
        feedback.setStatus(STATUS_OPEN);
        feedback.setCreateTime(LocalDateTime.now());
        feedbackService.save(feedback);
        return CommonResp.ok("反馈已提交，管理员处理后会通知你", null);
    }

    // 查询我的反馈
    @Operation(summary = "查询我的反馈")
    @GetMapping("/my")
    public CommonResp<List<UserFeedback>> my(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        List<UserFeedback> list = feedbackService.list(new QueryWrapper<UserFeedback>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
        list.forEach(this::fillUserName);
        return CommonResp.ok(list);
    }

    // 管理员分页查询反馈
    @Operation(summary = "管理员分页查询反馈")
    @GetMapping("/admin/page")
    public CommonResp<PageResp<UserFeedback>> adminPage(String status,
                                                        String keyword,
                                                        Long current,
                                                        Long pageSize,
                                                        HttpServletRequest request) {
        Page<UserFeedback> page = feedbackService.page(new Page<>(safeCurrent(current), safePageSize(pageSize)), buildAdminQuery(resolveAdminQueryStatus(status, request), keyword));
        page.getRecords().forEach(this::fillUserName);
        return CommonResp.ok(PageResp.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    public CommonResp<PageResp<UserFeedback>> adminPage(String status, String keyword, Long current, Long pageSize) {
        return adminPage(status, keyword, current, pageSize, null);
    }

    // 兼容旧前端接口
    @Operation(summary = "管理员查询反馈")
    @GetMapping("/admin/list")
    public CommonResp<List<UserFeedback>> adminList(String status, String keyword, HttpServletRequest request) {
        List<UserFeedback> list = feedbackService.list(buildAdminQuery(resolveAdminQueryStatus(status, request), keyword));
        list.forEach(this::fillUserName);
        return CommonResp.ok(list);
    }

    public CommonResp<List<UserFeedback>> adminList(String status, String keyword) {
        return adminList(status, keyword, null);
    }

    // 查询某条反馈的回复对话
    @Operation(summary = "查询反馈回复")
    @GetMapping("/reply/list/{feedbackId}")
    public CommonResp<List<FeedbackReply>> replyList(@PathVariable Long feedbackId, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        UserFeedback feedback = feedbackService.getById(feedbackId);
        if (feedback == null) {
            return CommonResp.fail("反馈不存在");
        }
        if (!userId.equals(feedback.getUserId()) && !isFeedbackAdmin(request)) {
            return CommonResp.fail("只能查看自己的反馈回复");
        }

        List<FeedbackReply> replies = feedbackReplyService.list(new QueryWrapper<FeedbackReply>()
                .eq("feedback_id", feedbackId)
                .orderByAsc("create_time")
                .orderByAsc("id"));
        replies.forEach(this::fillReplyUserName);
        return CommonResp.ok(replies);
    }

    // 管理员回复反馈
    @Operation(summary = "管理员回复反馈")
    @PostMapping("/admin/reply")
    public CommonResp<Object> reply(@RequestBody FeedbackReplyReq req, HttpServletRequest request) {
        Long adminUserId = AuthUtil.requireLogin(request);
        UserFeedback feedback = feedbackService.getById(req.getFeedbackId());
        if (feedback == null) {
            return CommonResp.fail("反馈不存在");
        }
        if (!StringUtils.hasText(req.getContent())) {
            return CommonResp.fail("回复内容不能为空");
        }

        saveReply(feedback.getId(), adminUserId, "admin", req.getContent());
        saveNotice(feedback.getUserId(), "反馈收到新回复", "你的反馈《" + feedback.getTitle() + "》有新的管理员回复：" + req.getContent());
        OperationLogUtil.save(operationLogService, adminUserId, "反馈管理", "回复反馈", "反馈ID：" + feedback.getId());
        return CommonResp.ok("回复成功", null);
    }

    // 管理员处理反馈
    @Operation(summary = "管理员处理反馈")
    @PostMapping("/admin/handle")
    public CommonResp<Object> handle(@RequestBody FeedbackHandleReq req, HttpServletRequest request) {
        Long adminUserId = AuthUtil.requireLogin(request);
        UserFeedback feedback = feedbackService.getById(req.getId());
        if (feedback == null) {
            return CommonResp.fail("反馈不存在");
        }
        if (!STATUS_HANDLED.equals(req.getStatus()) && !STATUS_REJECTED.equals(req.getStatus())) {
            return CommonResp.fail("处理状态只能是已处理或驳回");
        }
        if (!StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("请填写处理原因");
        }
        if (hasHandled(feedback) && !isSuperAdmin(request)) {
            return CommonResp.fail("该反馈已经处理过，不能再次更改");
        }

        handleOneFeedback(feedback, req.getStatus(), req.getRemark(), adminUserId);
        String resultText = statusText(req.getStatus());
        OperationLogUtil.save(operationLogService, adminUserId, "反馈管理", resultText, "反馈ID：" + feedback.getId());
        return CommonResp.ok("处理成功", null);
    }

    // 管理员批量处理反馈
    // 批量处理会逐条写入处理结果、回复对话和用户通知，保证用户能看到每条反馈的处理情况
    @Operation(summary = "管理员批量处理反馈")
    @PostMapping("/admin/batchHandle")
    public CommonResp<Object> batchHandle(@RequestBody BatchFeedbackHandleReq req, HttpServletRequest request) {
        Long adminUserId = AuthUtil.requireLogin(request);
        List<Long> ids = safeIds(req.getIds());
        if (ids.isEmpty()) {
            return CommonResp.fail("请选择要处理的反馈");
        }
        if (!STATUS_HANDLED.equals(req.getStatus()) && !STATUS_REJECTED.equals(req.getStatus())) {
            return CommonResp.fail("处理状态只能是已处理或驳回");
        }
        if (!StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("请填写处理原因");
        }

        List<UserFeedback> feedbackList = feedbackService.listByIds(ids);
        if (feedbackList.size() != ids.size()) {
            return CommonResp.fail("部分反馈不存在，请刷新后重新选择");
        }
        if (!isSuperAdmin(request) && feedbackList.stream().anyMatch(this::hasHandled)) {
            return CommonResp.fail("选中的反馈里存在已处理反馈，不能再次更改");
        }

        for (UserFeedback feedback : feedbackList) {
            handleOneFeedback(feedback, req.getStatus(), req.getRemark(), adminUserId);
        }
        OperationLogUtil.save(operationLogService, adminUserId, "反馈管理", "批量" + statusText(req.getStatus()), "反馈ID：" + ids);
        return CommonResp.ok("批量处理成功", null);
    }

    private void handleOneFeedback(UserFeedback feedback, String status, String remarkText, Long adminUserId) {
        feedback.setStatus(status);
        feedback.setHandleTime(LocalDateTime.now());
        feedback.setHandleUserId(adminUserId);
        feedback.setHandleRemark(remarkText);
        feedbackService.updateById(feedback);

        String resultText = statusText(status);
        String remark = safeRemark(remarkText);
        saveReply(feedback.getId(), adminUserId, "admin", "处理结果：" + resultText + "。" + remark);
        saveNotice(feedback.getUserId(), "反馈处理结果", "你的反馈《" + feedback.getTitle() + "》" + resultText + "：" + remark);
    }

    private boolean hasHandled(UserFeedback feedback) {
        return STATUS_HANDLED.equals(feedback.getStatus()) || STATUS_REJECTED.equals(feedback.getStatus()) || feedback.getHandleTime() != null;
    }

    private String statusText(String status) {
        return STATUS_HANDLED.equals(status) ? "已处理" : "已驳回";
    }

    private QueryWrapper<UserFeedback> buildAdminQuery(String status, String keyword) {
        QueryWrapper<UserFeedback> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("title", keyword).or().like("content", keyword));
        }
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    private void fillUserName(UserFeedback feedback) {
        User user = userService.getById(feedback.getUserId());
        feedback.setUserName(user == null ? "用户已删除" : user.getName());
    }

    private void fillReplyUserName(FeedbackReply reply) {
        User user = userService.getById(reply.getUserId());
        reply.setUserName(user == null ? "用户已删除" : user.getName());
    }

    private void saveReply(Long feedbackId, Long userId, String replyType, String content) {
        FeedbackReply reply = new FeedbackReply();
        reply.setFeedbackId(feedbackId);
        reply.setUserId(userId);
        reply.setReplyType(replyType);
        reply.setContent(content);
        reply.setCreateTime(LocalDateTime.now());
        feedbackReplyService.save(reply);
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

    private boolean isFeedbackAdmin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String token = request.getHeader("Authorization");
        com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.LoginUserInfo loginUserInfo =
                com.shiwangsi.oceanwiki.config.auth.AuthTokenStore.get(token);
        if (loginUserInfo == null) {
            return false;
        }
        boolean superAdmin = loginUserInfo.roleCodes() != null && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE);
        boolean feedbackAdmin = loginUserInfo.permissions() != null && loginUserInfo.permissions().contains("feedback:manage");
        return superAdmin || feedbackAdmin;
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
        return isContentReviewer(request) && !isSuperAdmin(request) ? STATUS_OPEN : status;
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
