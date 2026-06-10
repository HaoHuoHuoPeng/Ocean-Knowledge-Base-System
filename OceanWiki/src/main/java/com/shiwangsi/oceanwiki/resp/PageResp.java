// 文件说明：这个返回对象负责封装分页数据，前端表格需要用它显示总条数和当前页数据。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

import java.util.List;

// 分页返回对象
// 前端表格需要 records 展示当前页数据，也需要 total 计算分页器总页数
@Data
public class PageResp<T> {

    private List<T> records;

    private Long total;

    private Long current;

    private Long pageSize;

    public static <T> PageResp<T> of(List<T> records, Long total, Long current, Long pageSize) {
        PageResp<T> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(total);
        resp.setCurrent(current);
        resp.setPageSize(pageSize);
        return resp;
    }
}
