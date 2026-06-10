// 文件说明：这个请求参数类用来接收敏感词批量设置状态时前端提交的数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

import java.util.List;

// 敏感词批量状态请求
// 管理员勾选多个敏感词后，可以统一启用或停用
@Data
public class BatchSensitiveWordStatusReq {

    // 要批量设置状态的敏感词 id 列表
    private List<Long> ids;

    // 目标状态：1 启用，0 停用
    private Integer enabled;
}
