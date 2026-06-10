// 文件说明：这个 Controller 负责处理图片上传，电子书封面和文档正文图片都会走这里。
package com.shiwangsi.oceanwiki.controller;

import com.shiwangsi.oceanwiki.resp.CommonResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 文件上传接口
// 真实项目里不建议把图片直接转成 base64 存数据库，数据库只保存图片访问地址会更稳定
@Tag(name = "文件上传接口")
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOW_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Path EBOOK_COVER_DIR = Paths.get("uploads", "ebook-cover");
    private static final Path DOC_IMAGE_DIR = Paths.get("uploads", "doc-image");

    // 上传电子书封面
    @Operation(summary = "上传电子书封面")
    @PostMapping("/ebook-cover")
    public CommonResp<Map<String, String>> uploadEbookCover(@RequestParam("file") MultipartFile file) throws IOException {
        return uploadImage(file, EBOOK_COVER_DIR, "/uploads/ebook-cover/", "封面图片");
    }

    // 上传文档正文图片
    // wangEditor 会把本地图片传到这里，正文里只保存图片 URL，不保存 base64 大文本
    @Operation(summary = "上传文档正文图片")
    @PostMapping("/doc-image")
    public CommonResp<Map<String, String>> uploadDocImage(@RequestParam("file") MultipartFile file) throws IOException {
        return uploadImage(file, DOC_IMAGE_DIR, "/uploads/doc-image/", "正文图片");
    }

    private CommonResp<Map<String, String>> uploadImage(MultipartFile file,
                                                        Path saveDir,
                                                        String urlPrefix,
                                                        String imageName) throws IOException {
        if (file == null || file.isEmpty()) {
            return CommonResp.fail("请选择要上传的" + imageName);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return CommonResp.fail(imageName + "不能超过 5MB");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return CommonResp.fail("只能上传图片文件");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOW_EXTENSIONS.contains(extension)) {
            return CommonResp.fail("只支持 jpg、jpeg、png、gif、webp 格式图片");
        }

        Files.createDirectories(saveDir);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = saveDir.resolve(fileName).toAbsolutePath().normalize();

        // 防止文件名里带特殊路径，确保最终保存位置仍然在指定上传目录里
        Path safeRoot = saveDir.toAbsolutePath().normalize();
        if (!targetPath.startsWith(safeRoot)) {
            return CommonResp.fail("文件名不合法");
        }

        file.transferTo(targetPath.toFile());
        String url = urlPrefix + fileName;
        return CommonResp.ok("上传成功", Map.of("url", url));
    }

    private String getExtension(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "";
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
