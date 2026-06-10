// 文件说明：这个类描述用户批量导入 Excel 每一行有哪些列。
package com.shiwangsi.oceanwiki.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 用户导入行对象
// 角色不从 Excel 导入，导入成功后统一分配普通用户角色
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserImportExcel {

    @ExcelProperty("账号")
    private String loginName;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("密码")
    private String password;
}
