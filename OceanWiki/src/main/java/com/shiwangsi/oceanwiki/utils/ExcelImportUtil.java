// 文件说明：这个工具类负责 Excel 导入读取和模板下载，批量导入电子书、用户、敏感词都会用到。
package com.shiwangsi.oceanwiki.utils;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Excel 导入工具类
// 这里没有继续使用 CSV，是为了和参考项目一样支持 xls、xlsx 模板导入
public class ExcelImportUtil {

    private ExcelImportUtil() {
    }

    // 读取上传的 Excel 文件
    // headClass 是每一行对应的 Java 类，字段上的 @ExcelProperty 会对应 Excel 表头
    public static <T> List<T> read(MultipartFile file, Class<T> headClass) throws IOException {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
            throw new IllegalArgumentException("请上传 xls 或 xlsx 格式的 Excel 文件");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcelFactory.read(inputStream, headClass, null)
                    .autoCloseStream(false)
                    .doReadAllSync();
        }
    }

    // 输出 Excel 模板给浏览器下载
    // data 里放几行示例数据，用户下载后照着示例补充自己的数据
    public static <T> void write(HttpServletResponse response,
                                 String filename,
                                 String sheetName,
                                 Class<T> headClass,
                                 List<T> data) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        FastExcelFactory.write(response.getOutputStream(), headClass)
                .autoCloseStream(false)
                // Long 类型直接写成数字时，前端 Excel 可能显示科学计数法，这里转成字符串更稳
                .registerConverter(new LongStringConverter())
                .sheet(sheetName)
                .doWrite(data);
    }
}
