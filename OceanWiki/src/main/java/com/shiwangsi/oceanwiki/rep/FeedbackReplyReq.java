// 文件说明：这个请求参数类用来接收前端提交的反馈回复表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 反馈回复请求参数
// 管理员针对某一条反馈填写回复内容
@Data
public class FeedbackReplyReq {

    private Long feedbackId;

    private String content;
}
