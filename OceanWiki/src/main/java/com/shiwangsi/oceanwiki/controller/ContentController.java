// 文件说明：这个 Controller 负责正文内容相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.shiwangsi.oceanwiki.entity.Content;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

// 正文内容接口
// 大多数页面会通过 /doc/save 和 /doc/findContent 使用正文，这里保留单独接口方便你调试
@Tag(name = "正文内容接口")
@RestController
@RequestMapping("/content")
public class ContentController {

    private final IContentService contentService;

    public ContentController(IContentService contentService) {
        this.contentService = contentService;
    }

    @Operation(summary = "根据 id 查询正文")
    @GetMapping("/{id}")
    public CommonResp<Content> get(@PathVariable Long id) {
        return CommonResp.ok(contentService.getById(id));
    }
}
