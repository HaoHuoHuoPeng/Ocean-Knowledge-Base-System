// 文件说明：这个文件负责AdminFeatureIntegrationTest对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.controller.CommentController;
import com.shiwangsi.oceanwiki.controller.FeedbackController;
import com.shiwangsi.oceanwiki.controller.OperationLogController;
import com.shiwangsi.oceanwiki.controller.SensitiveWordController;
import com.shiwangsi.oceanwiki.entity.FeedbackReply;
import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.entity.SensitiveWord;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.entity.UserFeedback;
import com.shiwangsi.oceanwiki.rep.CommentReq;
import com.shiwangsi.oceanwiki.rep.FeedbackReplyReq;
import com.shiwangsi.oceanwiki.rep.FeedbackReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 后台新增功能测试
// 这里集中验证敏感词管理、分页查询、反馈回复对话、操作日志这些后台能力
class AdminFeatureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SensitiveWordController sensitiveWordController;

    @Autowired
    private CommentController commentController;

    @Autowired
    private FeedbackController feedbackController;

    @Autowired
    private OperationLogController operationLogController;

    @Test
    void sensitiveWordShouldSavePageDeleteAndWriteOperationLog() {
        MockHttpServletRequest adminRequest = loginRequest(
                TEST_PREFIX + "sensitive_admin_token",
                anotherUser.getId(),
                List.of("SUPER_ADMIN"),
                List.of("sensitive:manage")
        );

        SensitiveWord word = new SensitiveWord();
        word.setWord(TEST_PREFIX + "禁用词");
        word.setEnabled(1);
        word.setRemark("测试新增敏感词");

        CommonResp<Object> saveResp = sensitiveWordController.save(word, adminRequest);
        CommonResp<PageResp<SensitiveWord>> pageResp = sensitiveWordController.page(TEST_PREFIX + "禁用词", 1L, 10L);
        SensitiveWord dbWord = sensitiveWordService.getOne(new QueryWrapper<SensitiveWord>().eq("word", word.getWord()));
        CommonResp<Object> deleteResp = sensitiveWordController.delete(dbWord.getId(), adminRequest);
        long logCount = operationLogService.count(new QueryWrapper<OperationLog>()
                .eq("module", "敏感词管理")
                .like("content", word.getWord()));

        assertThat(saveResp.isSuccess()).isTrue();
        assertThat(pageResp.getContent().getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(pageResp.getContent().getRecords()).extracting(SensitiveWord::getWord).contains(word.getWord());
        assertThat(deleteResp.isSuccess()).isTrue();
        assertThat(logCount).isGreaterThanOrEqualTo(2);

        AuthTokenStore.remove(TEST_PREFIX + "sensitive_admin_token");
    }

    @Test
    void commentAdminPageShouldReturnPagedRecordsAndUseDatabaseSensitiveWords() {
        SensitiveWord word = new SensitiveWord();
        word.setWord(TEST_PREFIX + "敏感");
        word.setEnabled(1);
        word.setRemark("测试评论敏感词");
        sensitiveWordService.save(word);

        MockHttpServletRequest userRequest = loginRequest(
                TEST_PREFIX + "comment_page_user_token",
                normalUser.getId(),
                List.of("NORMAL_USER"),
                List.of("ebook:view")
        );
        CommentReq req = new CommentReq();
        req.setTargetType("doc");
        req.setTargetId(doc.getId());
        req.setParentId(0L);
        req.setContent("这条评论包含" + TEST_PREFIX + "敏感词");

        CommonResp<UserComment> submitResp = commentController.submit(req, userRequest);
        CommonResp<PageResp<UserComment>> pageResp = commentController.adminPage("pending", TEST_PREFIX + "敏感", 1L, 5L);

        assertThat(submitResp.isSuccess()).isTrue();
        assertThat(submitResp.getContent().getStatus()).isEqualTo("pending");
        assertThat(submitResp.getContent().getSensitiveHit()).contains(TEST_PREFIX + "敏感");
        assertThat(pageResp.getContent().getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(pageResp.getContent().getRecords()).extracting(UserComment::getId).contains(submitResp.getContent().getId());

        AuthTokenStore.remove(TEST_PREFIX + "comment_page_user_token");
    }

    @Test
    void feedbackAdminReplyShouldCreateConversationNoticeLogAndPageResult() {
        MockHttpServletRequest userRequest = loginRequest(
                TEST_PREFIX + "feedback_reply_user_token",
                normalUser.getId(),
                List.of("NORMAL_USER"),
                List.of("ebook:view")
        );
        FeedbackReq feedbackReq = new FeedbackReq();
        feedbackReq.setTargetType("doc");
        feedbackReq.setTargetId(doc.getId());
        feedbackReq.setType("suggestion");
        feedbackReq.setTitle(TEST_PREFIX + "反馈回复测试");
        feedbackReq.setContent("希望管理员回复这条反馈");
        feedbackController.submit(feedbackReq, userRequest);
        UserFeedback feedback = feedbackService.getOne(new QueryWrapper<UserFeedback>().eq("title", feedbackReq.getTitle()));

        MockHttpServletRequest adminRequest = loginRequest(
                TEST_PREFIX + "feedback_reply_admin_token",
                anotherUser.getId(),
                List.of("SUPER_ADMIN"),
                List.of("feedback:manage")
        );
        FeedbackReplyReq replyReq = new FeedbackReplyReq();
        replyReq.setFeedbackId(feedback.getId());
        replyReq.setContent("管理员已经收到，会尽快处理");

        CommonResp<Object> replyResp = feedbackController.reply(replyReq, adminRequest);
        CommonResp<List<FeedbackReply>> repliesResp = feedbackController.replyList(feedback.getId(), userRequest);
        CommonResp<PageResp<UserFeedback>> pageResp = feedbackController.adminPage("open", TEST_PREFIX + "反馈回复测试", 1L, 5L);
        long logCount = operationLogService.count(new QueryWrapper<OperationLog>()
                .eq("module", "反馈管理")
                .eq("action", "回复反馈")
                .like("content", "反馈ID：" + feedback.getId()));

        assertThat(replyResp.isSuccess()).isTrue();
        assertThat(repliesResp.getContent()).extracting(FeedbackReply::getContent).contains(replyReq.getContent());
        assertThat(pageResp.getContent().getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(pageResp.getContent().getRecords()).extracting(UserFeedback::getId).contains(feedback.getId());
        assertThat(logCount).isEqualTo(1);

        AuthTokenStore.remove(TEST_PREFIX + "feedback_reply_user_token");
        AuthTokenStore.remove(TEST_PREFIX + "feedback_reply_admin_token");
    }

    @Test
    void contentReviewerShouldOnlySeePendingCommentsAndOpenFeedback() {
        UserComment pendingComment = createComment(TEST_PREFIX + "审核过滤待审评论", "pending", normalUser.getId());
        UserComment publishedComment = createComment(TEST_PREFIX + "审核过滤已发布评论", "published", normalUser.getId());

        UserFeedback openFeedback = new UserFeedback();
        openFeedback.setUserId(normalUser.getId());
        openFeedback.setTargetType("doc");
        openFeedback.setTargetId(doc.getId());
        openFeedback.setType("suggestion");
        openFeedback.setTitle(TEST_PREFIX + "审核过滤待处理反馈");
        openFeedback.setContent("需要内容审核员处理");
        openFeedback.setStatus("open");
        openFeedback.setCreateTime(LocalDateTime.now());
        feedbackService.save(openFeedback);

        UserFeedback handledFeedback = new UserFeedback();
        handledFeedback.setUserId(normalUser.getId());
        handledFeedback.setTargetType("doc");
        handledFeedback.setTargetId(doc.getId());
        handledFeedback.setType("suggestion");
        handledFeedback.setTitle(TEST_PREFIX + "审核过滤已处理反馈");
        handledFeedback.setContent("已经处理的反馈");
        handledFeedback.setStatus("handled");
        handledFeedback.setCreateTime(LocalDateTime.now());
        feedbackService.save(handledFeedback);

        MockHttpServletRequest reviewerRequest = loginRequest(
                TEST_PREFIX + "content_reviewer_filter_token",
                anotherUser.getId(),
                List.of("CONTENT_REVIEWER"),
                List.of("ebook:review", "comment:manage", "feedback:manage")
        );

        CommonResp<PageResp<UserComment>> commentPage = commentController.adminPage(null, TEST_PREFIX + "审核过滤", 1L, 20L, reviewerRequest);
        CommonResp<PageResp<UserFeedback>> feedbackPage = feedbackController.adminPage(null, TEST_PREFIX + "审核过滤", 1L, 20L, reviewerRequest);

        assertThat(commentPage.getContent().getRecords()).extracting(UserComment::getId).contains(pendingComment.getId());
        assertThat(commentPage.getContent().getRecords()).extracting(UserComment::getId).doesNotContain(publishedComment.getId());
        assertThat(feedbackPage.getContent().getRecords()).extracting(UserFeedback::getId).contains(openFeedback.getId());
        assertThat(feedbackPage.getContent().getRecords()).extracting(UserFeedback::getId).doesNotContain(handledFeedback.getId());

        AuthTokenStore.remove(TEST_PREFIX + "content_reviewer_filter_token");
    }

    @Test
    void commentDeleteShouldAllowOwnerDocAuthorAndAdminOnly() {
        doc.setCreateUserId(normalUser.getId());
        docService.updateById(doc);

        UserComment selfComment = createComment(TEST_PREFIX + "自己删除自己的评论", "published", normalUser.getId());
        UserComment docAuthorTargetComment = createComment(TEST_PREFIX + "文章作者删除文章下评论", "published", anotherUser.getId());
        UserComment blockedComment = createComment(TEST_PREFIX + "非作者不能删除", "published", normalUser.getId());

        MockHttpServletRequest normalRequest = loginRequest(
                TEST_PREFIX + "comment_delete_normal_token",
                normalUser.getId(),
                List.of("NORMAL_USER"),
                List.of("ebook:view")
        );
        MockHttpServletRequest otherUserRequest = loginRequest(
                TEST_PREFIX + "comment_delete_other_token",
                anotherUser.getId(),
                List.of("NORMAL_USER"),
                List.of("ebook:view")
        );
        MockHttpServletRequest adminRequest = loginRequest(
                TEST_PREFIX + "comment_delete_admin_token",
                anotherUser.getId(),
                List.of("SUPER_ADMIN"),
                List.of("comment:manage")
        );

        CommonResp<Object> selfDeleteResp = commentController.delete(selfComment.getId(), normalRequest);
        CommonResp<Object> docAuthorDeleteResp = commentController.delete(docAuthorTargetComment.getId(), normalRequest);
        CommonResp<Object> blockedDeleteResp = commentController.delete(blockedComment.getId(), otherUserRequest);
        CommonResp<Object> adminDeleteResp = commentController.delete(blockedComment.getId(), adminRequest);

        assertThat(selfDeleteResp.isSuccess()).isTrue();
        assertThat(commentService.getById(selfComment.getId()).getStatus()).isEqualTo("deleted");
        assertThat(docAuthorDeleteResp.isSuccess()).isTrue();
        assertThat(commentService.getById(docAuthorTargetComment.getId()).getStatus()).isEqualTo("deleted");
        assertThat(blockedDeleteResp.isSuccess()).isFalse();
        assertThat(adminDeleteResp.isSuccess()).isTrue();
        assertThat(commentService.getById(blockedComment.getId()).getStatus()).isEqualTo("deleted");

        AuthTokenStore.remove(TEST_PREFIX + "comment_delete_normal_token");
        AuthTokenStore.remove(TEST_PREFIX + "comment_delete_other_token");
        AuthTokenStore.remove(TEST_PREFIX + "comment_delete_admin_token");
    }

    @Test
    void operationLogPageShouldReturnPagedRecords() {
        OperationLog log = new OperationLog();
        log.setUserId(normalUser.getId());
        log.setModule("测试分页模块");
        log.setAction("测试分页动作");
        log.setContent(TEST_PREFIX + "日志分页内容");
        log.setCreateTime(LocalDateTime.now());
        operationLogService.save(log);

        CommonResp<PageResp<OperationLog>> pageResp = operationLogController.page(TEST_PREFIX + "日志分页内容", 1L, 10L);

        assertThat(pageResp.isSuccess()).isTrue();
        assertThat(pageResp.getContent().getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(pageResp.getContent().getRecords()).extracting(OperationLog::getId).contains(log.getId());
    }

    private MockHttpServletRequest loginRequest(String token, Long userId, List<String> roleCodes, List<String> permissions) {
        AuthTokenStore.put(token, userId, roleCodes, permissions);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);
        return request;
    }

    private UserComment createComment(String content, String status, Long userId) {
        UserComment comment = new UserComment();
        comment.setUserId(userId);
        comment.setTargetType("doc");
        comment.setTargetId(doc.getId());
        comment.setParentId(0L);
        comment.setContent(content);
        comment.setStatus(status);
        comment.setSensitiveHit("");
        comment.setCreateTime(LocalDateTime.now());
        commentService.save(comment);
        return comment;
    }
}
