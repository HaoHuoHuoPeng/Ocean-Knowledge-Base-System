// 文件说明：这个 Controller 负责敏感词相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiwangsi.oceanwiki.entity.SensitiveWord;
import com.shiwangsi.oceanwiki.excel.SensitiveWordImportExcel;
import com.shiwangsi.oceanwiki.rep.BatchSensitiveWordStatusReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.ImportResp;
import com.shiwangsi.oceanwiki.resp.PageResp;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.ISensitiveWordService;
import com.shiwangsi.oceanwiki.utils.ExcelImportUtil;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 敏感词管理接口
// 管理员维护启用中的敏感词后，评论发布时会自动使用这些词进行检查
@Tag(name = "敏感词管理接口")
@RestController
@RequestMapping("/sensitiveWord")
public class SensitiveWordController {

    private final ISensitiveWordService sensitiveWordService;
    private final IOperationLogService operationLogService;

    public SensitiveWordController(ISensitiveWordService sensitiveWordService, IOperationLogService operationLogService) {
        this.sensitiveWordService = sensitiveWordService;
        this.operationLogService = operationLogService;
    }

    // 分页查询敏感词
    @Operation(summary = "分页查询敏感词")
    @GetMapping("/page")
    public CommonResp<PageResp<SensitiveWord>> page(String keyword, Long current, Long pageSize) {
        QueryWrapper<SensitiveWord> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("word", keyword).or().like("remark", keyword));
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        Page<SensitiveWord> page = sensitiveWordService.page(new Page<>(safeCurrent(current), safePageSize(pageSize)), wrapper);
        return CommonResp.ok(PageResp.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    // 保存敏感词
    @Operation(summary = "保存敏感词")
    @PostMapping("/save")
    public CommonResp<Object> save(@RequestBody SensitiveWord sensitiveWord, HttpServletRequest request) {
        if (!StringUtils.hasText(sensitiveWord.getWord())) {
            return CommonResp.fail("敏感词不能为空");
        }

        QueryWrapper<SensitiveWord> wrapper = new QueryWrapper<>();
        wrapper.eq("word", sensitiveWord.getWord());
        SensitiveWord dbWord = sensitiveWordService.getOne(wrapper);
        if (dbWord != null && !dbWord.getId().equals(sensitiveWord.getId())) {
            return CommonResp.fail("敏感词已存在");
        }

        boolean create = sensitiveWord.getId() == null;
        if (sensitiveWord.getEnabled() == null) {
            sensitiveWord.setEnabled(1);
        }
        if (create) {
            sensitiveWord.setCreateTime(LocalDateTime.now());
        }
        sensitiveWordService.saveOrUpdate(sensitiveWord);
        OperationLogUtil.save(operationLogService, request, "敏感词管理", create ? "新增敏感词" : "保存敏感词", sensitiveWord.getWord());
        return CommonResp.ok("保存成功", null);
    }

    // 删除敏感词
    @Operation(summary = "删除敏感词")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        SensitiveWord sensitiveWord = sensitiveWordService.getById(id);
        sensitiveWordService.removeById(id);
        OperationLogUtil.save(operationLogService, request, "敏感词管理", "删除敏感词", sensitiveWord == null ? "ID：" + id : sensitiveWord.getWord());
        return CommonResp.ok("删除成功", null);
    }

    // 批量设置敏感词状态
    // 管理员可以一次性把勾选的敏感词启用或停用，避免逐条编辑
    @Operation(summary = "批量设置敏感词状态")
    @PostMapping("/batchStatus")
    public CommonResp<Object> batchStatus(@RequestBody BatchSensitiveWordStatusReq req, HttpServletRequest request) {
        List<Long> ids = safeIds(req.getIds());
        if (ids.isEmpty()) {
            return CommonResp.fail("请选择要设置状态的敏感词");
        }
        if (req.getEnabled() == null || (req.getEnabled() != 0 && req.getEnabled() != 1)) {
            return CommonResp.fail("敏感词状态不正确");
        }

        List<SensitiveWord> words = sensitiveWordService.listByIds(ids);
        if (words.size() != ids.size()) {
            return CommonResp.fail("部分敏感词不存在，请刷新后重新选择");
        }

        for (SensitiveWord word : words) {
            word.setEnabled(req.getEnabled());
        }
        sensitiveWordService.updateBatchById(words);
        OperationLogUtil.save(
                operationLogService,
                request,
                "敏感词管理",
                req.getEnabled() == 1 ? "批量启用敏感词" : "批量停用敏感词",
                "敏感词ID：" + ids
        );
        return CommonResp.ok("批量设置成功", null);
    }

    // 下载敏感词导入模板
    // 模板只填写敏感词和备注，状态由系统统一设为停用
    @Operation(summary = "下载敏感词导入模板")
    @GetMapping("/get-import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<SensitiveWordImportExcel> templateRows = List.of(
                SensitiveWordImportExcel.builder()
                        .word("示例敏感词")
                        .remark("这里填写加入原因，导入后默认停用")
                        .build()
        );
        ExcelImportUtil.write(response, "敏感词导入模板.xlsx", "敏感词", SensitiveWordImportExcel.class, templateRows);
    }

    // 批量导入敏感词 Excel
    // Excel 表头：敏感词、备注
    // 状态不从文件导入，统一初始化为停用，管理员确认后再批量启用
    @Operation(summary = "批量导入敏感词 Excel")
    @PostMapping("/import")
    public CommonResp<ImportResp> importExcel(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        List<SensitiveWordImportExcel> rows;
        try {
            rows = ExcelImportUtil.read(file, SensitiveWordImportExcel.class);
        } catch (IllegalArgumentException e) {
            return CommonResp.fail(e.getMessage());
        }
        if (rows.isEmpty()) {
            return CommonResp.fail("Excel 中没有可导入的数据");
        }

        List<SensitiveWord> importList = new ArrayList<>();
        ImportResp importResp = new ImportResp();
        Set<String> fileWords = new LinkedHashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            SensitiveWordImportExcel row = rows.get(i);
            int lineNo = i + 2;
            String rowKey = "第 " + lineNo + " 行";
            String word = row.getWord();
            String remark = row.getRemark();

            if (!StringUtils.hasText(word)) {
                importResp.addFailure(rowKey, "敏感词不能为空");
                continue;
            }
            rowKey = "第 " + lineNo + " 行：" + word;
            QueryWrapper<SensitiveWord> wrapper = new QueryWrapper<>();
            wrapper.eq("word", word);
            boolean existsInDatabase = sensitiveWordService.getOne(wrapper) != null;
            boolean existsInFile = !fileWords.add(word);
            if (existsInDatabase) {
                importResp.addFailure(rowKey, "敏感词已存在");
                continue;
            }
            if (existsInFile) {
                importResp.addFailure(rowKey, "Excel 文件内敏感词重复");
                continue;
            }

            SensitiveWord sensitiveWord = new SensitiveWord();
            sensitiveWord.setWord(word);
            sensitiveWord.setRemark(remark);
            sensitiveWord.setEnabled(0);
            sensitiveWord.setCreateTime(LocalDateTime.now());
            importList.add(sensitiveWord);
            importResp.addSuccess(word);
        }

        if (!importList.isEmpty()) {
            sensitiveWordService.saveBatch(importList);
        }
        OperationLogUtil.save(operationLogService, request, "敏感词管理", "批量导入敏感词", "导入数量：" + importList.size());
        return CommonResp.ok("导入完成，成功 " + importResp.getSuccessNames().size() + " 条，失败 " + importResp.getFailureMap().size() + " 条，成功敏感词初始状态为停用", importResp);
    }

    private List<Long> safeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private long safeCurrent(Long current) {
        return current == null || current < 1 ? 1 : current;
    }

    private long safePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
