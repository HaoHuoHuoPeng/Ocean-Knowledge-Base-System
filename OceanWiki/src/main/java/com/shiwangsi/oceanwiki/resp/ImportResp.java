// 文件说明：这个返回对象负责承载批量导入结果，前端可以展示成功和失败明细。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 批量导入结果
// successNames 保存导入成功的数据名称，failureMap 保存失败行和原因
@Data
public class ImportResp {

    // 导入成功的数据名称，例如电子书名称、用户账号、敏感词
    private List<String> successNames = new ArrayList<>();

    // 导入失败的数据，key 一般是“第几行 + 名称”，value 是失败原因
    private Map<String, String> failureMap = new LinkedHashMap<>();

    public void addSuccess(String name) {
        successNames.add(name);
    }

    public void addFailure(String key, String reason) {
        failureMap.put(key, reason);
    }
}
