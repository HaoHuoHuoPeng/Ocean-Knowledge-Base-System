// 文件说明：这个请求参数类用来接收前端提交的反馈表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 用户反馈请求参数
// 普通建议可以不传 targetType 和 targetId
// 针对某本电子书或某篇文档纠错时，再把目标信息传上来
@Data
public class FeedbackReq {

    private String targetType;

    private Long targetId;

    private String type;

    private String title;

    private String content;
}
