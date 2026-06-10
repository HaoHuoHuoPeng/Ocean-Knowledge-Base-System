// 文件说明：这个返回对象负责封装通过地址参数分页查询出来的数据。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

import java.util.List;

// 分页列表返回对象
// 这个结构和教案截图里的 content.total、content.list 保持一致
@Data
public class ListByPageResp<T> {

    // 符合条件的数据总数
    private Long total;

    // 当前页的数据列表
    private List<T> list;

    public static <T> ListByPageResp<T> of(Long total, List<T> list) {
        ListByPageResp<T> resp = new ListByPageResp<>();
        resp.setTotal(total);
        resp.setList(list);
        return resp;
    }
}
