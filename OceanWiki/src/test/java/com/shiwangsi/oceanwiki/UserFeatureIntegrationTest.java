// 文件说明：这个文件负责用户对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.controller.CommentController;
import com.shiwangsi.oceanwiki.controller.DocController;
import com.shiwangsi.oceanwiki.controller.FavoriteController;
import com.shiwangsi.oceanwiki.controller.FeedbackController;
import com.shiwangsi.oceanwiki.controller.NoticeController;
import com.shiwangsi.oceanwiki.controller.ReadingHistoryController;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.entity.UserFeedback;
import com.shiwangsi.oceanwiki.entity.UserFavorite;
import com.shiwangsi.oceanwiki.entity.UserNotice;
import com.shiwangsi.oceanwiki.rep.CommentReq;
import com.shiwangsi.oceanwiki.rep.CommentReviewReq;
import com.shiwangsi.oceanwiki.rep.FavoriteReq;
import com.shiwangsi.oceanwiki.rep.FeedbackHandleReq;
import com.shiwangsi.oceanwiki.rep.FeedbackReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.FavoriteStatusResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.IReadingHistoryService;
import com.shiwangsi.oceanwiki.service.IUserCommentService;
import com.shiwangsi.oceanwiki.service.IUserFavoriteService;
import com.shiwangsi.oceanwiki.service.IUserFeedbackService;
import com.shiwangsi.oceanwiki.service.IUserNoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 普通用户互动功能测试
// 这些测试覆盖收藏、阅读历史、评论审核、反馈处理、通知等容易互相影响的功能
class UserFeatureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FavoriteController favoriteController;

    @Autowired
    private DocController docController;

    @Autowired
    private CommentController commentController;

    @Autowired
    private FeedbackController feedbackController;

    @Autowired
    private NoticeController noticeController;

    @Autowired
    private ReadingHistoryController historyController;

    @Autowired
    private IUserFavoriteService favoriteService;

    @Autowired
    private IReadingHistoryService historyService;

    @Autowired
    private IUserCommentService commentService;

    @Autowired
    private IUserFeedbackService feedbackService;

    @Autowired
    private IUserNoticeService noticeService;

    @Autowired
    private IOperationLogService operationLogService;

    @Test
    void toggleFavoriteShouldCreateAndRemoveFavoriteForCurrentUser() {
        String token = TEST_PREFIX + "favorite_token";
        MockHttpServletRequest request = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        FavoriteReq req = new FavoriteReq();
        req.setTargetType("doc");
        req.setTargetId(doc.getId());

        CommonResp<FavoriteStatusResp> firstResp = favoriteController.toggle(req, request);
        UserFavorite dbFavorite = favoriteService.getOne(new QueryWrapper<UserFavorite>()
                .eq("user_id", normalUser.getId())
                .eq("target_type", "doc")
                .eq("target_id", doc.getId()));

        assertThat(firstResp.isSuccess()).isTrue();
        assertThat(firstResp.getContent().getFavorited()).isTrue();
        assertThat(dbFavorite).isNotNull();

        CommonResp<FavoriteStatusResp> statusResp = favoriteController.status("doc", doc.getId(), request);
        assertThat(statusResp.getContent().getFavorited()).isTrue();

        CommonResp<FavoriteStatusResp> secondResp = favoriteController.toggle(req, request);
        long favoriteCount = favoriteService.count(new QueryWrapper<UserFavorite>()
                .eq("user_id", normalUser.getId())
                .eq("target_type", "doc")
                .eq("target_id", doc.getId()));

        assertThat(secondResp.getContent().getFavorited()).isFalse();
        assertThat(favoriteCount).isZero();
        AuthTokenStore.remove(token);
    }

    @Test
    void findContentShouldRecordReadingHistoryForLoginUser() {
        String token = TEST_PREFIX + "history_token";
        MockHttpServletRequest request = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        CommonResp<String> resp = docController.findContent(doc.getId(), request);
        ReadingHistory history = historyService.getOne(new QueryWrapper<ReadingHistory>()
                .eq("user_id", normalUser.getId())
                .eq("doc_id", doc.getId()));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent()).contains("测试正文");
        assertThat(history).isNotNull();
        assertThat(history.getEbookId()).isEqualTo(ebook.getId());

        AuthTokenStore.remove(token);
    }

    @Test
    void readingHistoryProgressShouldUseWholeEbookLeafDocs() {
        Doc secondDoc = createDoc(ebook.getId(), "第二篇文档");
        String token = TEST_PREFIX + "ebook_progress_token";
        MockHttpServletRequest request = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        docController.findContent(doc.getId(), request);
        historyController.updateProgress(progressReq(doc.getId(), 100), request);

        CommonResp<List<ReadingHistory>> halfResp = historyController.my(request);
        ReadingHistory firstHistory = halfResp.getContent().stream()
                .filter(item -> doc.getId().equals(item.getDocId()))
                .findFirst()
                .orElseThrow();

        docController.findContent(secondDoc.getId(), request);
        historyController.updateProgress(progressReq(secondDoc.getId(), 100), request);

        CommonResp<List<ReadingHistory>> fullResp = historyController.my(request);
        ReadingHistory latestEbookHistory = fullResp.getContent().stream()
                .filter(item -> ebook.getId().equals(item.getEbookId()))
                .findFirst()
                .orElseThrow();

        assertThat(halfResp.getContent()).filteredOn(item -> ebook.getId().equals(item.getEbookId())).hasSize(1);
        assertThat(fullResp.getContent()).filteredOn(item -> ebook.getId().equals(item.getEbookId())).hasSize(1);
        assertThat(firstHistory.getProgress()).isEqualTo(50);
        assertThat(latestEbookHistory.getProgress()).isEqualTo(100);
        assertThat(latestEbookHistory.getDocId()).isEqualTo(secondDoc.getId());
        assertThat(latestEbookHistory.getDocName()).contains("第二篇文档");

        historyController.delete(latestEbookHistory.getId(), request);
        long remainingHistoryCount = historyService.count(new QueryWrapper<ReadingHistory>()
                .eq("user_id", normalUser.getId())
                .eq("ebook_id", ebook.getId()));
        assertThat(remainingHistoryCount).isZero();

        AuthTokenStore.remove(token);
    }

    @Test
    void docAllShouldSupportFuzzySearchByEbookName() {
        ebook.setName(TEST_PREFIX + "虎鲸电子书");
        ebookService.updateById(ebook);

        CommonResp<List<Doc>> matchedResp = docController.all(null, null, "虎鲸", null);
        CommonResp<List<Doc>> emptyResp = docController.all(null, null, "不存在的电子书名称", null);

        assertThat(matchedResp.getContent()).extracting(Doc::getEbookId).contains(ebook.getId());
        assertThat(emptyResp.getContent()).isEmpty();
    }

    @Test
    void docPageTreeShouldPageRootDocsAndKeepChildren() {
        Doc parentDoc = createDoc(ebook.getId(), "父文档");
        Doc childDoc = createDoc(ebook.getId(), "子文档");
        childDoc.setParent(parentDoc.getId());
        docService.updateById(childDoc);

        CommonResp<PageResp<Doc>> resp = docController.pageTree(ebook.getId(), "父文档", null, null, 1L, 10L, null);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent().getTotal()).isEqualTo(1);
        assertThat(resp.getContent().getRecords()).hasSize(1);
        assertThat(resp.getContent().getRecords().get(0).getName()).contains("父文档");
        assertThat(resp.getContent().getRecords().get(0).getChildren())
                .extracting(Doc::getName)
                .anyMatch(name -> name.contains("子文档"));

        CommonResp<PageResp<Doc>> childSearchResp = docController.pageTree(ebook.getId(), "子文档", null, null, 1L, 10L, null);
        assertThat(childSearchResp.getContent().getRecords()).hasSize(1);
        assertThat(childSearchResp.getContent().getRecords().get(0).getName()).contains("父文档");
        assertThat(childSearchResp.getContent().getRecords().get(0).getChildren())
                .extracting(Doc::getName)
                .anyMatch(name -> name.contains("子文档"));
    }

    @Test
    void docPageTreeShouldPutPendingSubmissionFirstAndReturnSubmitterName() {
        Doc publishedDoc = createDoc(ebook.getId(), "普通已发布文档");
        Doc pendingDoc = createDoc(ebook.getId(), "用户投稿待审核文档");
        pendingDoc.setStatus("pending");
        pendingDoc.setCreateUserId(normalUser.getId());
        docService.updateById(pendingDoc);

        CommonResp<PageResp<Doc>> resp = docController.pageTree(ebook.getId(), null, null, null, 1L, 10L, null);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent().getRecords()).extracting(Doc::getId).contains(publishedDoc.getId(), pendingDoc.getId());
        assertThat(resp.getContent().getRecords().get(0).getId()).isEqualTo(pendingDoc.getId());
        assertThat(resp.getContent().getRecords().get(0).getCreateUserName()).isEqualTo(normalUser.getName());
    }

    @Test
    void cleanCommentShouldPublishAndOwnerCanDeleteIt() {
        String token = TEST_PREFIX + "comment_owner_token";
        MockHttpServletRequest ownerRequest = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        CommentReq req = new CommentReq();
        req.setTargetType("doc");
        req.setTargetId(doc.getId());
        req.setParentId(0L);
        req.setContent("这是一条正常评论");

        CommonResp<UserComment> submitResp = commentController.submit(req, ownerRequest);
        UserComment comment = commentService.getById(submitResp.getContent().getId());
        CommonResp<List<UserComment>> publicResp = commentController.publicList("doc", doc.getId());

        assertThat(submitResp.isSuccess()).isTrue();
        assertThat(comment.getStatus()).isEqualTo("published");
        assertThat(publicResp.getContent()).extracting(UserComment::getId).contains(comment.getId());

        String otherToken = TEST_PREFIX + "comment_other_token";
        MockHttpServletRequest otherRequest = loginRequest(otherToken, anotherUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));
        CommonResp<Object> otherDeleteResp = commentController.delete(comment.getId(), otherRequest);

        assertThat(otherDeleteResp.isSuccess()).isFalse();

        CommonResp<Object> ownerDeleteResp = commentController.delete(comment.getId(), ownerRequest);
        UserComment deletedComment = commentService.getById(comment.getId());

        assertThat(ownerDeleteResp.isSuccess()).isTrue();
        assertThat(deletedComment.getStatus()).isEqualTo("deleted");

        AuthTokenStore.remove(token);
        AuthTokenStore.remove(otherToken);
    }

    @Test
    void sensitiveCommentShouldBePendingUntilAdminApprovesIt() {
        String token = TEST_PREFIX + "sensitive_comment_token";
        MockHttpServletRequest userRequest = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        CommentReq req = new CommentReq();
        req.setTargetType("doc");
        req.setTargetId(doc.getId());
        req.setParentId(0L);
        req.setContent("这条评论包含广告，需要审核");

        CommonResp<UserComment> submitResp = commentController.submit(req, userRequest);
        UserComment pendingComment = commentService.getById(submitResp.getContent().getId());
        CommonResp<List<UserComment>> publicBeforeReview = commentController.publicList("doc", doc.getId());

        assertThat(submitResp.isSuccess()).isTrue();
        assertThat(pendingComment.getStatus()).isEqualTo("pending");
        assertThat(pendingComment.getSensitiveHit()).contains("广告");
        assertThat(publicBeforeReview.getContent()).extracting(UserComment::getId).doesNotContain(pendingComment.getId());

        String adminToken = TEST_PREFIX + "comment_admin_token";
        MockHttpServletRequest adminRequest = loginRequest(adminToken, anotherUser.getId(), List.of("SUPER_ADMIN"), List.of("comment:manage"));

        CommentReviewReq reviewReq = new CommentReviewReq();
        reviewReq.setId(pendingComment.getId());
        reviewReq.setStatus("published");
        reviewReq.setRemark("测试审核通过");

        CommonResp<Object> reviewResp = commentController.review(reviewReq, adminRequest);
        UserComment reviewedComment = commentService.getById(pendingComment.getId());
        long noticeCount = noticeService.count(new QueryWrapper<UserNotice>()
                .eq("user_id", normalUser.getId())
                .like("title", "评论审核结果"));
        long logCount = operationLogService.count(new QueryWrapper<OperationLog>()
                .eq("module", "评论管理")
                .like("content", "评论ID：" + pendingComment.getId()));

        assertThat(reviewResp.isSuccess()).isTrue();
        assertThat(reviewedComment.getStatus()).isEqualTo("published");
        assertThat(noticeCount).isEqualTo(1);
        assertThat(logCount).isEqualTo(1);

        AuthTokenStore.remove(token);
        AuthTokenStore.remove(adminToken);
    }

    @Test
    void feedbackHandleShouldCreateNoticeAndOperationLog() {
        String token = TEST_PREFIX + "feedback_user_token";
        MockHttpServletRequest userRequest = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        FeedbackReq req = new FeedbackReq();
        req.setTargetType("doc");
        req.setTargetId(doc.getId());
        req.setType("correction");
        req.setTitle("测试纠错");
        req.setContent("这里有一处内容需要修改");

        CommonResp<Object> submitResp = feedbackController.submit(req, userRequest);
        UserFeedback feedback = feedbackService.getOne(new QueryWrapper<UserFeedback>()
                .eq("user_id", normalUser.getId())
                .eq("title", req.getTitle()));

        assertThat(submitResp.isSuccess()).isTrue();
        assertThat(feedback).isNotNull();
        assertThat(feedback.getStatus()).isEqualTo("open");

        String adminToken = TEST_PREFIX + "feedback_admin_token";
        MockHttpServletRequest adminRequest = loginRequest(adminToken, anotherUser.getId(), List.of("SUPER_ADMIN"), List.of("feedback:manage"));

        FeedbackHandleReq handleReq = new FeedbackHandleReq();
        handleReq.setId(feedback.getId());
        handleReq.setStatus("handled");
        handleReq.setRemark("测试已处理");

        CommonResp<Object> handleResp = feedbackController.handle(handleReq, adminRequest);
        UserFeedback handledFeedback = feedbackService.getById(feedback.getId());
        long noticeCount = noticeService.count(new QueryWrapper<UserNotice>()
                .eq("user_id", normalUser.getId())
                .like("title", "反馈处理结果"));
        long logCount = operationLogService.count(new QueryWrapper<OperationLog>()
                .eq("module", "反馈管理")
                .like("content", "反馈ID：" + feedback.getId()));

        assertThat(handleResp.isSuccess()).isTrue();
        assertThat(handledFeedback.getStatus()).isEqualTo("handled");
        assertThat(noticeCount).isEqualTo(1);
        assertThat(logCount).isEqualTo(1);

        AuthTokenStore.remove(token);
        AuthTokenStore.remove(adminToken);
    }

    @Test
    void noticeShouldOnlyMarkCurrentUsersNoticeAsRead() {
        UserNotice ownNotice = createNotice(normalUser.getId(), "自己的通知");
        UserNotice otherNotice = createNotice(anotherUser.getId(), "别人的通知");

        String token = TEST_PREFIX + "notice_token";
        MockHttpServletRequest request = loginRequest(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        CommonResp<List<UserNotice>> myResp = noticeController.my(request);
        CommonResp<Object> readOtherResp = noticeController.read(otherNotice.getId(), request);
        UserNotice latestOtherNotice = noticeService.getById(otherNotice.getId());
        CommonResp<Object> readOwnResp = noticeController.read(ownNotice.getId(), request);
        UserNotice latestOwnNotice = noticeService.getById(ownNotice.getId());

        assertThat(myResp.getContent()).extracting(UserNotice::getId).contains(ownNotice.getId());
        assertThat(myResp.getContent()).extracting(UserNotice::getId).doesNotContain(otherNotice.getId());
        assertThat(readOtherResp.isSuccess()).isTrue();
        assertThat(latestOtherNotice.getReadFlag()).isZero();
        assertThat(readOwnResp.isSuccess()).isTrue();
        assertThat(latestOwnNotice.getReadFlag()).isEqualTo(1);

        AuthTokenStore.remove(token);
    }

    private MockHttpServletRequest loginRequest(String token, Long userId, List<String> roleCodes, List<String> permissions) {
        AuthTokenStore.put(token, userId, roleCodes, permissions);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);
        return request;
    }

    private java.util.Map<String, Object> progressReq(Long docId, Integer progress) {
        return java.util.Map.of(
                "docId", docId,
                "progress", progress
        );
    }

    private UserNotice createNotice(Long userId, String title) {
        UserNotice notice = new UserNotice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent("测试通知内容");
        notice.setReadFlag(0);
        noticeService.save(notice);
        return notice;
    }
}
