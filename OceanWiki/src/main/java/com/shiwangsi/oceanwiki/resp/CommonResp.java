// 文件说明：这个返回对象负责统一接口返回格式，前端统一读取 success、message、content。
package com.shiwangsi.oceanwiki.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 后端统一返回对象
// 前端所有接口都按 success、message、content 三个字段读取，写页面时会更稳定
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResp<T> {

    // true 表示请求成功，false 表示请求失败
    private boolean success = true;

    // 返回给前端看的提示信息
    private String message = "操作成功";

    // 真正的数据内容，可以是列表、对象、字符串等
    private T content;

    public static <T> CommonResp<T> ok(T content) {
        return new CommonResp<>(true, "操作成功", content);
    }

    public static <T> CommonResp<T> ok(String message, T content) {
        return new CommonResp<>(true, message, content);
    }

    public static <T> CommonResp<T> fail(String message) {
        return new CommonResp<>(false, message, null);
    }
}
