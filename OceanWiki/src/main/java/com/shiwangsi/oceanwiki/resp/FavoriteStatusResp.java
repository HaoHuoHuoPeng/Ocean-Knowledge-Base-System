// 文件说明：这个返回对象类用来封装收藏接口返回给前端的数据。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

// 收藏状态返回对象
// 前端用 favorited 控制按钮显示“收藏”还是“取消收藏”
@Data
public class FavoriteStatusResp {

    private Boolean favorited;

    // 当前收藏所在分组；未收藏时返回默认分组，方便前端下拉框回显
    private String folderName;
}
