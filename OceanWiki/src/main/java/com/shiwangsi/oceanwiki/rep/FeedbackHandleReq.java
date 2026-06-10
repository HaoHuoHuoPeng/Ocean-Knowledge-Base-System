// 文件说明：这个请求参数类用来接收前端提交的反馈表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 反馈处理请求参数
// status 使用 handled 表示已处理，使用 rejected 表示驳回
@Data
public class FeedbackHandleReq {

    private Long id;

    private String status;

    private String remark;
}
