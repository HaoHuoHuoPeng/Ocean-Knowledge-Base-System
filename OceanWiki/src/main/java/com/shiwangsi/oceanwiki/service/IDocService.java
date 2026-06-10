// 文件说明：这个业务接口定义文档模块可以做哪些业务操作。
package com.shiwangsi.oceanwiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shiwangsi.oceanwiki.entity.Doc;
import com.shiwangsi.oceanwiki.resp.DocVoteResp;

import java.util.List;

// 文档业务接口
public interface IDocService extends IService<Doc> {

    // 保存文档基础信息和正文内容
    void saveDoc(Doc doc);

    // 删除单个文档，同时删除正文内容
    void deleteDoc(Long id);

    // 根据电子书 id 查询文档目录
    List<Doc> listByEbookId(Long ebookId);

    // 查询正文，同时把阅读数加 1
    String findContent(Long id);

    // 查询当前用户是否已经点赞
    DocVoteResp getVoteStatus(Long id, Long userId);

    // 点赞或取消点赞，同时刷新电子书统计字段
    DocVoteResp toggleVote(Long id, Long userId);
}
