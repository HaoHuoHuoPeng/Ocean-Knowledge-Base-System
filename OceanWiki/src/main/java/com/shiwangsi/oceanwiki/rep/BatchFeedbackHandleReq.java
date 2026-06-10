// 文件说明：这个请求参数类用来接收反馈批量处理时前端提交的数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

import java.util.List;

// 反馈批量处理请求
// 管理员勾选多条反馈后，可以统一标记为已处理或已驳回
@Data
public class BatchFeedbackHandleReq {

    // 要批量处理的反馈 id 列表
    private List<Long> ids;

    // 处理状态：handled 已处理，rejected 已驳回
    private String status;

    // 批量处理说明，会写入每条反馈的处理备注并通知用户
    private String remark;
}
