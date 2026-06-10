// 文件说明：这个请求参数类用来接收前端提交的评论表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 评论审核请求参数
// status 只能使用 published 或 rejected，不能由前端直接改成 deleted
@Data
public class CommentReviewReq {

    private Long id;

    private String status;

    private String remark;
}
