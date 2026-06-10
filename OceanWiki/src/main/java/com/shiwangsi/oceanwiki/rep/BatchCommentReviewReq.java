// 文件说明：这个请求参数类用来接收评论批量审核时前端提交的数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

import java.util.List;

// 评论批量审核请求
// 管理员勾选多条评论后，可以统一审核通过或驳回
@Data
public class BatchCommentReviewReq {

    // 要批量审核的评论 id 列表
    private List<Long> ids;

    // 审核状态：published 通过，rejected 驳回
    private String status;

    // 审核备注，会写到每条评论上并通知评论作者
    private String remark;
}
