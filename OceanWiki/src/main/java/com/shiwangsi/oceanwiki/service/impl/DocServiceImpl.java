// 文件说明：这个业务实现类负责文档的具体业务逻辑，并调用 Mapper 操作数据库。
package com.shiwangsi.oceanwiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shiwangsi.oceanwiki.entity.Content;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.entity.DocVersion;
import com.shiwangsi.oceanwiki.entity.DocVote;
import com.shiwangsi.oceanwiki.entity.ReadingHistory;
import com.shiwangsi.oceanwiki.entity.UserComment;
import com.shiwangsi.oceanwiki.entity.UserFavorite;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.exception.BusinessExceptionCode;
import com.shiwangsi.oceanwiki.mapper.ContentMapper;
import com.shiwangsi.oceanwiki.mapper.DocMapper;
import com.shiwangsi.oceanwiki.mapper.DocVersionMapper;
import com.shiwangsi.oceanwiki.mapper.EbookMapper;
import com.shiwangsi.oceanwiki.mapper.ReadingHistoryMapper;
import com.shiwangsi.oceanwiki.mapper.UserCommentMapper;
import com.shiwangsi.oceanwiki.mapper.UserFavoriteMapper;
import com.shiwangsi.oceanwiki.resp.DocVoteResp;
import com.shiwangsi.oceanwiki.service.IDocService;
import com.shiwangsi.oceanwiki.service.IDocVoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 文档业务实现类
// 这个类负责把 doc 表和 content 表一起维护好
@Service
public class DocServiceImpl extends ServiceImpl<DocMapper, Doc> implements IDocService {

    private final ContentMapper contentMapper;
    private final EbookMapper ebookMapper;
    private final IDocVoteService docVoteService;
    private final DocVersionMapper docVersionMapper;
    private final ReadingHistoryMapper readingHistoryMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserCommentMapper userCommentMapper;

    public DocServiceImpl(ContentMapper contentMapper,
                          EbookMapper ebookMapper,
                          IDocVoteService docVoteService,
                          DocVersionMapper docVersionMapper,
                          ReadingHistoryMapper readingHistoryMapper,
                          UserFavoriteMapper userFavoriteMapper,
                          UserCommentMapper userCommentMapper) {
        this.contentMapper = contentMapper;
        this.ebookMapper = ebookMapper;
        this.docVoteService = docVoteService;
        this.docVersionMapper = docVersionMapper;
        this.readingHistoryMapper = readingHistoryMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userCommentMapper = userCommentMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDoc(Doc doc) {
        // 新增文档时，阅读数和点赞数默认从 0 开始
        if (doc.getId() == null) {
            doc.setViewCount(0);
            doc.setVoteCount(0);
            doc.setSort(nextSort(doc));
        }

        if (!StringUtils.hasText(doc.getStatus())) {
            doc.setStatus("draft");
        }

        // saveOrUpdate 会根据 id 是否为空自动判断新增还是修改
        this.saveOrUpdate(doc);

        // 正文内容单独保存在 content 表中，id 和 doc.id 保持一致
        Content content = new Content();
        content.setId(doc.getId());
        content.setContent(StringUtils.hasText(doc.getContent()) ? doc.getContent() : "");

        // content 表的主键不是自增，所以这里手动判断是插入还是更新
        if (contentMapper.selectById(doc.getId()) == null) {
            contentMapper.insert(content);
        } else {
            contentMapper.updateById(content);
        }

        ebookMapper.refreshEbookInfo();
    }

    private Integer nextSort(Doc doc) {
        // 排序不再让管理员手动填写，而是按同一本电子书、同一个父文档下面已有的最大排序自动加 1
        QueryWrapper<Doc> wrapper = new QueryWrapper<>();
        wrapper.eq("ebook_id", doc.getEbookId());
        wrapper.eq("parent", doc.getParent() == null ? 0 : doc.getParent());
        wrapper.orderByDesc("sort").orderByDesc("id").last("limit 1");
        Doc lastDoc = this.getOne(wrapper);
        return lastDoc == null || lastDoc.getSort() == null ? 1 : lastDoc.getSort() + 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDoc(Long id) {
        List<Long> ids = findDocAndChildrenIds(id);
        if (ids.isEmpty()) {
            return;
        }

        // 删除父文档时，必须把所有子文档一起删除，否则阅读目录会出现没有父级的残留文档
        this.removeBatchByIds(ids);
        // 同步删除正文，避免 content 表留下无用数据
        contentMapper.deleteBatchIds(ids);
        // 删除历史版本，避免版本表指向已经不存在的文档
        docVersionMapper.delete(new QueryWrapper<DocVersion>().in("doc_id", ids));
        // 删除点赞记录，避免点赞表指向已经不存在的文档
        docVoteService.remove(new QueryWrapper<DocVote>().in("doc_id", ids));
        // 删除阅读历史，避免“我的阅读历史”里继续出现已删除文档
        readingHistoryMapper.delete(new QueryWrapper<ReadingHistory>().in("doc_id", ids));
        // 删除文档收藏，避免“我的收藏”里继续出现已删除文档
        userFavoriteMapper.delete(new QueryWrapper<UserFavorite>()
                .eq("target_type", "doc")
                .in("target_id", ids));
        // 删除文档评论，避免评论表指向已经不存在的文档
        userCommentMapper.delete(new QueryWrapper<UserComment>()
                .eq("target_type", "doc")
                .in("target_id", ids));

        ebookMapper.refreshEbookInfo();
    }

    @Override
    public List<Doc> listByEbookId(Long ebookId) {
        QueryWrapper<Doc> wrapper = new QueryWrapper<>();
        wrapper.eq("ebook_id", ebookId);
        wrapper.eq("status", "published");
        wrapper.orderByAsc("sort", "id");
        return filterOrphanDocs(this.list(wrapper));
    }

    private List<Long> findDocAndChildrenIds(Long id) {
        if (id == null || this.getById(id) == null) {
            return List.of();
        }

        List<Doc> allDocs = this.list();
        List<Long> result = new ArrayList<>();
        collectDocAndChildrenIds(id, allDocs, result);
        return result;
    }

    private void collectDocAndChildrenIds(Long id, List<Doc> allDocs, List<Long> result) {
        result.add(id);
        for (Doc doc : allDocs) {
            if (id.equals(doc.getParent())) {
                collectDocAndChildrenIds(doc.getId(), allDocs, result);
            }
        }
    }

    private List<Doc> filterOrphanDocs(List<Doc> docs) {
        Map<Long, Doc> docMap = docs.stream()
                .filter(doc -> doc.getId() != null)
                .collect(Collectors.toMap(Doc::getId, doc -> doc, (oldValue, newValue) -> oldValue));
        return docs.stream()
                .filter(doc -> hasValidParentChain(doc, docMap, new HashSet<>()))
                .toList();
    }

    private boolean hasValidParentChain(Doc doc, Map<Long, Doc> docMap, Set<Long> visitedIds) {
        Long parent = doc.getParent();
        if (parent == null || parent == 0) {
            return true;
        }
        if (!visitedIds.add(doc.getId())) {
            return false;
        }
        Doc parentDoc = docMap.get(parent);
        if (parentDoc == null) {
            return false;
        }
        return hasValidParentChain(parentDoc, docMap, visitedIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String findContent(Long id) {
        Doc doc = this.getById(id);
        if (doc == null) {
            throw new BusinessException(BusinessExceptionCode.DOC_NOT_FOUND);
        }

        // 每次打开文档，阅读数加 1
        baseMapper.increaseViewCount(id);
        ebookMapper.refreshEbookInfo();

        Content content = contentMapper.selectById(id);
        return content == null ? "" : content.getContent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocVoteResp getVoteStatus(Long id, Long userId) {
        Doc doc = getExistingDoc(id);
        DocVoteResp resp = new DocVoteResp();
        resp.setVoted(hasVoted(id, userId));
        resp.setVoteCount(doc.getVoteCount() == null ? 0 : doc.getVoteCount());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocVoteResp toggleVote(Long id, Long userId) {
        getExistingDoc(id);

        DocVote docVote = getDocVote(id, userId);
        boolean voted;
        if (docVote == null) {
            // 没有点赞记录，说明本次操作是点赞
            DocVote newVote = new DocVote();
            newVote.setDocId(id);
            newVote.setUserId(userId);
            newVote.setCreateTime(LocalDateTime.now());
            docVoteService.save(newVote);
            baseMapper.increaseVoteCount(id);
            voted = true;
        } else {
            // 已经点过赞，再点一次就是取消点赞
            docVoteService.removeById(docVote.getId());
            baseMapper.decreaseVoteCount(id);
            voted = false;
        }

        ebookMapper.refreshEbookInfo();

        Doc latestDoc = this.getById(id);
        DocVoteResp resp = new DocVoteResp();
        resp.setVoted(voted);
        resp.setVoteCount(latestDoc.getVoteCount() == null ? 0 : latestDoc.getVoteCount());
        return resp;
    }

    private Doc getExistingDoc(Long id) {
        Doc doc = this.getById(id);
        if (doc == null) {
            throw new BusinessException(BusinessExceptionCode.DOC_NOT_FOUND);
        }
        return doc;
    }

    private boolean hasVoted(Long docId, Long userId) {
        return getDocVote(docId, userId) != null;
    }

    private DocVote getDocVote(Long docId, Long userId) {
        if (userId == null) {
            return null;
        }
        return docVoteService.getOne(new QueryWrapper<DocVote>()
                .eq("doc_id", docId)
                .eq("user_id", userId));
    }
}
