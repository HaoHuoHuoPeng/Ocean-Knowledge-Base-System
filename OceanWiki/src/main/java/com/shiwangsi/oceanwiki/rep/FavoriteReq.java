// 文件说明：这个请求参数类用来接收前端提交的收藏表单数据。
package com.shiwangsi.oceanwiki.rep;

import lombok.Data;

// 收藏请求参数
// targetType 表示收藏对象类型，ebook 是电子书，doc 是文档
// targetId 表示被收藏的电子书 id 或文档 id
@Data
public class FavoriteReq {

    private String targetType;

    private Long targetId;

    // 收藏分组名称，不传时使用默认分组
    private String folderName;
}
