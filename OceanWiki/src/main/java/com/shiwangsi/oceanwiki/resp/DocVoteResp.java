// 文件说明：这个返回对象类用来封装文档点赞接口返回给前端的数据。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

// 文档点赞接口返回对象
// 前端根据 voted 判断按钮显示“点赞”还是“取消点赞”
@Data
public class DocVoteResp {

    // true 表示当前用户已点赞，false 表示当前用户未点赞
    private Boolean voted;

    // 当前文档最新点赞数
    private Integer voteCount;
}
