// 文件说明：这个 Controller 负责文档相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Content;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.DocVersion;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.entity.SensitiveWord;
import com.shiwangsi.oceanwiki.entity.UserNotice;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.DocVoteResp;
import com.shiwangsi.oceanwiki.service.IContentService;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IDocVersionService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.IReadingHistoryService;
import com.shiwangsi.oceanwiki.service.ISensitiveWordService;
import com.shiwangsi.oceanwiki.service.IUserNoticeService;
import com.shiwangsi.oceanwiki.utils.AuthUtil;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

// 文档管理接口
// 后台文档管理和前台阅读页面都会调用这里
@Tag(name = "文档管理接口")
@RestController
@RequestMapping("/doc")
public class DocController {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_REJECTED = "rejected";
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String DOC_MANAGE_PERMISSION = "doc:manage";
    private static final String DOC_REVIEW_PERMISSION = "doc:review";

    private final IContentService contentService;
    private final IDocService docService;
    private final IDocVersionService docVersionService;
    private final IReadingHistoryService historyService;
    private final IOperationLogService operationLogService;
    private final ISensitiveWordService sensitiveWordService;
    private final IEbookService ebookService;
    private final IUserNoticeService noticeService;

    public DocController(IContentService contentService,
                         IDocService docService,
                         IDocVersionService docVersionService,
                         IReadingHistoryService historyService,
                         IOperationLogService operationLogService,
                         ISensitiveWordService sensitiveWordService,
                         IEbookService ebookService,
                         IUserNoticeService noticeService) {
        this.contentService = contentService;
        this.docService = docService;
        this.docVersionService = docVersionService;
        this.historyService = historyService;
        this.operationLogService = operationLogService;
        this.sensitiveWordService = sensitiveWordService;
        this.ebookService = ebookService;
        this.noticeService = noticeService;
    }

    // 查询全部文档，后台管理页使用
    @Operation(summary = "查询全部文档")
    @GetMapping("/all")
    public CommonResp<List<Doc>> all(String keyword, HttpServletRequest request) {
        QueryWrapper<Doc> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword);
        }
        if (isDocReviewOnlyUser(request)) {
            wrapper.eq("status", STATUS_PENDING);
            wrapper.isNotNull("create_user_id");
        }
        wrapper.orderByAsc("ebook_id", "sort", "id");
        return CommonResp.ok(docService.list(wrapper));
    }

    public CommonResp<List<Doc>> all(String keyword) {
        return all(keyword, null);
    }

    // 根据电子书 id 查询已发布文档目录，阅读页使用
    @Operation(summary = "根据电子书查询文档")
    @GetMapping("/all/{ebookId}")
    public CommonResp<List<Doc>> allByEbookId(@PathVariable Long ebookId) {
        return CommonResp.ok(docService.listByEbookId(ebookId));
    }

    // 保存文档和正文内容
    @Operation(summary = "保存文档")
    @PostMapping("/save")
    public CommonResp<Object> save(@RequestBody Doc doc, HttpServletRequest request) {
        boolean create = doc.getId() == null;
        Doc oldDoc = create ? null : docService.getById(doc.getId());
        if (!create) {
            saveDocVersion(doc.getId(), request);
        }
        docService.saveDoc(doc);
        if (create) {
            saveDocVersion(doc.getId(), request);
        }
        sendReviewNoticeIfNeeded(oldDoc, doc);
        OperationLogUtil.save(operationLogService, request, "文档管理", create ? "新增文档" : "修改文档", doc.getName());
        return CommonResp.ok("保存成功", null);
    }

    // 普通用户投稿文档
    // 投稿先做敏感词检查；命中敏感词会直接保存为驳回，未命中才进入待审核
    @Operation(summary = "普通用户投稿文档")
    @PostMapping("/submit")
    public CommonResp<Object> submit(@RequestBody Doc doc, HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        if (doc.getEbookId() == null || ebookService.getById(doc.getEbookId()) == null) {
            return CommonResp.fail("请选择有效的电子书");
        }
        if (doc.getParent() != null && doc.getParent() != 0) {
            Doc parentDoc = docService.getById(doc.getParent());
            if (parentDoc == null || !doc.getEbookId().equals(parentDoc.getEbookId())) {
                return CommonResp.fail("请选择当前电子书下面的父文档");
            }
        }
        if (!StringUtils.hasText(doc.getName())) {
            return CommonResp.fail("文档名称不能为空");
        }
        if (!StringUtils.hasText(doc.getContent())) {
            return CommonResp.fail("正文内容不能为空");
        }

        List<String> hits = findSensitiveWords(doc.getName() + " " + stripHtml(doc.getContent()));
        doc.setId(null);
        doc.setCreateUserId(userId);
        doc.setReviewRemark(null);
        if (hits.isEmpty()) {
            doc.setStatus(STATUS_PENDING);
        } else {
            doc.setStatus(STATUS_REJECTED);
            doc.setReviewRemark("投稿命中敏感词：" + String.join("、", hits));
        }
        docService.saveDoc(doc);
        saveDocVersion(doc.getId(), request);
        OperationLogUtil.save(operationLogService, request, "文档投稿", hits.isEmpty() ? "提交待审核" : "敏感词驳回", doc.getName());

        if (hits.isEmpty()) {
            return CommonResp.ok("投稿已提交，等待内容审核员审核", null);
        }
        return CommonResp.ok("投稿命中敏感词，已自动驳回：" + String.join("、", hits), null);
    }

    // 审核用户投稿文档
    // 内容审核员只处理待审核投稿；通过后进入阅读目录，驳回后投稿人可以在“我的投稿”看到原因
    @Operation(summary = "审核投稿文档")
    @PostMapping("/review")
    public CommonResp<Object> review(@RequestBody DocReviewReq req, HttpServletRequest request) {
        Long reviewUserId = AuthUtil.requireLogin(request);
        Doc doc = docService.getById(req.getId());
        if (doc == null) {
            return CommonResp.fail("文档不存在");
        }
        if (doc.getCreateUserId() == null) {
            return CommonResp.fail("只能审核用户投稿文档");
        }
        if (!STATUS_PENDING.equals(doc.getStatus())) {
            return CommonResp.fail("只有待审核文档才能审核");
        }
        if (!STATUS_PUBLISHED.equals(req.getStatus()) && !STATUS_REJECTED.equals(req.getStatus())) {
            return CommonResp.fail("审核结果只能是通过或驳回");
        }
        if (STATUS_REJECTED.equals(req.getStatus()) && !StringUtils.hasText(req.getRemark())) {
            return CommonResp.fail("审核驳回必须填写原因");
        }

        Doc oldDoc = docService.getById(doc.getId());
        saveDocVersion(doc.getId(), request);
        doc.setStatus(req.getStatus());
        doc.setReviewRemark(trimToNull(req.getRemark()));
        docService.updateById(doc);
        ebookService.refreshEbookInfo();
        sendReviewNoticeIfNeeded(oldDoc, doc);
        OperationLogUtil.save(operationLogService,
                reviewUserId,
                "文档审核",
                STATUS_PUBLISHED.equals(req.getStatus()) ? "审核通过" : "审核驳回",
                "文档ID：" + doc.getId() + "，标题：" + doc.getName());
        return CommonResp.ok("审核处理成功", null);
    }

    // 查询我的投稿
    // 普通用户可以在个人中心看到投稿当前是待审核、已发布还是已驳回
    @Operation(summary = "查询我的投稿")
    @GetMapping("/my-submissions")
    public CommonResp<List<Doc>> mySubmissions(HttpServletRequest request) {
        Long userId = AuthUtil.requireLogin(request);
        List<Doc> list = docService.list(new QueryWrapper<Doc>()
                .eq("create_user_id", userId)
                .orderByDesc("id"));
        return CommonResp.ok(list);
    }

    // 查询某篇文档的历史版本
    // 管理员可以在文档管理页查看，确认内容后再决定是否回滚
    @Operation(summary = "查询文档版本")
    @GetMapping("/versions/{docId}")
    public CommonResp<List<DocVersion>> versions(@PathVariable Long docId) {
        return CommonResp.ok(docVersionService.list(new QueryWrapper<DocVersion>()
                .eq("doc_id", docId)
                .orderByDesc("version_no")));
    }

    // 删除指定历史版本
    // 只删除 doc_version 表里的快照，不会删除当前文档正文
    @Operation(summary = "删除文档版本")
    @DeleteMapping("/version/{versionId}")
    public CommonResp<Object> deleteVersion(@PathVariable Long versionId, HttpServletRequest request) {
        DocVersion version = docVersionService.getById(versionId);
        if (version == null) {
            return CommonResp.fail("版本不存在");
        }
        docVersionService.removeById(versionId);
        OperationLogUtil.save(operationLogService, request, "文档管理", "删除文档版本", "文档ID：" + version.getDocId() + "，版本：v" + version.getVersionNo());
        return CommonResp.ok("删除成功", null);
    }

    // 回滚到指定历史版本
    // 回滚前也会先备份当前内容，避免回滚操作本身无法撤销
    @Operation(summary = "回滚文档版本")
    @PostMapping("/rollback/{versionId}")
    public CommonResp<Object> rollback(@PathVariable Long versionId, HttpServletRequest request) {
        DocVersion version = docVersionService.getById(versionId);
        if (version == null) {
            return CommonResp.fail("版本不存在");
        }

        saveDocVersion(version.getDocId(), request);

        Doc doc = docService.getById(version.getDocId());
        if (doc == null) {
            return CommonResp.fail("文档不存在");
        }
        doc.setName(version.getName());
        doc.setStatus(version.getStatus());
        doc.setContent(version.getContent());
        docService.saveDoc(doc);
        OperationLogUtil.save(operationLogService, request, "文档管理", "回滚文档版本", doc.getName() + " -> v" + version.getVersionNo());
        return CommonResp.ok("回滚成功", null);
    }

    // 删除单个文档
    @Operation(summary = "删除文档")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Doc doc = docService.getById(id);
        docService.deleteDoc(id);
        OperationLogUtil.save(operationLogService, request, "文档管理", "删除文档", doc == null ? "ID：" + id : doc.getName());
        return CommonResp.ok("删除成功", null);
    }

    // 批量删除文档
    // ids 用英文逗号分隔，例如 1,2,3
    @Operation(summary = "批量删除文档")
    @GetMapping("/remove")
    public CommonResp<Object> remove(String ids, HttpServletRequest request) {
        if (ids != null && !ids.isBlank()) {
            Arrays.stream(ids.split(","))
                    .map(Long::valueOf)
                    .forEach(docService::deleteDoc);
            OperationLogUtil.save(operationLogService, request, "文档管理", "批量删除文档", ids);
        }
        return CommonResp.ok("删除成功", null);
    }

    // 查询正文内容
    // 每次打开正文都会让阅读数加 1；如果用户已登录，还会写入或更新阅读历史
    @Operation(summary = "查询文档正文")
    @GetMapping("/findContent/{id}")
    public CommonResp<String> findContent(@PathVariable Long id, HttpServletRequest request) {
        String content = docService.findContent(id);
        saveReadingHistory(id, request);
        return CommonResp.ok(content);
    }

    // 文档点赞或取消点赞
    // 当前用户没点过赞时会点赞，已经点过赞时会取消点赞
    @Operation(summary = "文档点赞或取消点赞")
    @GetMapping("/vote/{id}")
    public CommonResp<DocVoteResp> vote(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return CommonResp.fail("请先登录后再点赞");
        }
        DocVoteResp resp = docService.toggleVote(id, userId);
        return CommonResp.ok(resp.getVoted() ? "点赞成功" : "已取消点赞", resp);
    }

    // 查询当前用户对某篇文档的点赞状态
    @Operation(summary = "查询文档点赞状态")
    @GetMapping("/voteStatus/{id}")
    public CommonResp<DocVoteResp> voteStatus(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return CommonResp.fail("请先登录");
        }
        return CommonResp.ok(docService.getVoteStatus(id, userId));
    }

    private void saveReadingHistory(Long docId, HttpServletRequest request) {
        Long userId = AuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return;
        }

        Doc doc = docService.getById(docId);
        if (doc == null || doc.getEbookId() == null) {
            return;
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
        history.setReadTime(LocalDateTime.now());
        historyService.saveOrUpdate(history);
    }

    private void saveDocVersion(Long docId, HttpServletRequest request) {
        Doc oldDoc = docService.getById(docId);
        if (oldDoc == null) {
            return;
        }

        DocVersion lastVersion = docVersionService.getOne(new QueryWrapper<DocVersion>()
                .eq("doc_id", docId)
                .orderByDesc("version_no")
                .last("limit 1"));

        DocVersion version = new DocVersion();
        version.setDocId(docId);
        version.setVersionNo(lastVersion == null || lastVersion.getVersionNo() == null ? 1 : lastVersion.getVersionNo() + 1);
        version.setName(oldDoc.getName());
        version.setStatus(oldDoc.getStatus());
        Content content = contentService.getById(docId);
        version.setContent(content == null ? "" : content.getContent());
        version.setCreateUserId(AuthUtil.getCurrentUserId(request));
        version.setCreateTime(LocalDateTime.now());
        docVersionService.save(version);
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
            words = Arrays.asList("违法", "赌博", "诈骗", "色情", "暴力", "辱骂", "广告");
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

    private String stripHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ");
    }

    private void sendReviewNoticeIfNeeded(Doc oldDoc, Doc newDoc) {
        if (oldDoc == null || oldDoc.getCreateUserId() == null || !StringUtils.hasText(newDoc.getStatus())) {
            return;
        }
        String oldStatus = oldDoc.getStatus();
        String newStatus = newDoc.getStatus();
        if (newStatus.equals(oldStatus)) {
            return;
        }
        if (!"published".equals(newStatus) && !"rejected".equals(newStatus)) {
            return;
        }

        String result = STATUS_PUBLISHED.equals(newStatus) ? "审核通过" : "审核驳回";
        String remark = StringUtils.hasText(newDoc.getReviewRemark()) ? "，备注：" + newDoc.getReviewRemark() : "";
        UserNotice notice = new UserNotice();
        notice.setUserId(oldDoc.getCreateUserId());
        notice.setTitle("投稿审核结果");
        notice.setContent("你的投稿《" + newDoc.getName() + "》已" + result + remark);
        notice.setReadFlag(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeService.save(notice);
    }

    private boolean isDocReviewOnlyUser(HttpServletRequest request) {
        var loginUserInfo = AuthUtil.getLoginUserInfo(request);
        if (loginUserInfo == null) {
            return false;
        }
        if (loginUserInfo.roleCodes() != null && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE)) {
            return false;
        }
        boolean canManage = loginUserInfo.permissions() != null && loginUserInfo.permissions().contains(DOC_MANAGE_PERMISSION);
        boolean canReview = loginUserInfo.permissions() != null && loginUserInfo.permissions().contains(DOC_REVIEW_PERMISSION);
        return canReview && !canManage;
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    // 文档审核请求参数
    // status 只能传 published 或 rejected，remark 是审核说明
    public static class DocReviewReq {
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
}
