// 文件说明：这个请求参数类用来接收前端提交的评论表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 发表评论请求参数
// 评论可以挂在电子书上，也可以挂在具体文档上
// parentId 预留给回复评论使用，当前传空或 0 表示一级评论
@Data
public class CommentReq {

    private String targetType;

    private Long targetId;

    private Long parentId;

    private String content;
}
