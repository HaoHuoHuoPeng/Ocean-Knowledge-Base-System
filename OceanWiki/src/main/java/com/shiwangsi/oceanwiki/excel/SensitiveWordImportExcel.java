// 文件说明：这个类描述敏感词批量导入 Excel 每一行有哪些列。
package com.shiwangsi.oceanwiki.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 敏感词导入行对象
// 状态不从 Excel 导入，导入成功后统一设为停用，管理员确认后再启用
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveWordImportExcel {

    @ExcelProperty("敏感词")
    private String word;

    @ExcelProperty("备注")
    private String remark;
}
