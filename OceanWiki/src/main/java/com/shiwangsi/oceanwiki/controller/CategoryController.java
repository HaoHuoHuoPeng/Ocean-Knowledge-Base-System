// 文件说明：这个 Controller 负责分类相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Category;
import com.shiwangsi.oceanwiki.entity.Ebook;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.exception.BusinessExceptionCode;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.ICategoryService;
import com.shiwangsi.oceanwiki.service.IEbookService;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

// 分类管理接口
// 分类数据本身数量不大，所以这里不做分页，直接返回全部分类给前端组装树
@Tag(name = "分类管理接口")
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final ICategoryService categoryService;
    private final IEbookService ebookService;
    private final IOperationLogService operationLogService;

    public CategoryController(ICategoryService categoryService, IEbookService ebookService, IOperationLogService operationLogService) {
        this.categoryService = categoryService;
        this.ebookService = ebookService;
        this.operationLogService = operationLogService;
    }

    // 查询全部分类
    @Operation(summary = "查询全部分类")
    @GetMapping("/all")
    public CommonResp<List<Category>> all() {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort", "id");
        return CommonResp.ok(categoryService.list(wrapper));
    }

    // 兼容教案中的 /category/list 写法
    @Operation(summary = "查询分类列表")
    @GetMapping("/list")
    public CommonResp<List<Category>> list() {
        return all();
    }

    // 保存分类
    @Operation(summary = "保存分类")
    @PostMapping("/save")
    public CommonResp<Object> save(@RequestBody Category category, HttpServletRequest request) {
        boolean create = category.getId() == null;
        if (category.getParent() == null) {
            category.setParent(0L);
        }
        if (!create && isSelfOrChild(category.getId(), category.getParent())) {
            throw new BusinessException(BusinessExceptionCode.CATEGORY_HAS_CHILD);
        }
        if (create) {
            category.setSort(getNextSort(category.getParent()));
        } else {
            Category oldCategory = categoryService.getById(category.getId());
            if (oldCategory == null || oldCategory.getSort() == null || !Objects.equals(oldCategory.getParent(), category.getParent())) {
                category.setSort(getNextSort(category.getParent()));
            } else {
                category.setSort(oldCategory.getSort());
            }
        }
        categoryService.saveOrUpdate(category);
        OperationLogUtil.save(operationLogService, request, "分类管理", create ? "新增分类" : "修改分类", category.getName());
        return CommonResp.ok("保存成功", null);
    }

    // 删除分类
    // 如果分类下面还有子分类，先不允许删除，避免树结构断掉
    @Operation(summary = "删除分类")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Category category = categoryService.getById(id);
        long childCount = categoryService.count(new QueryWrapper<Category>().eq("parent", id));
        if (childCount > 0) {
            throw new BusinessException(BusinessExceptionCode.CATEGORY_HAS_CHILD);
        }
        List<Long> categoryIds = findCategoryAndChildrenIds(id);
        long ebookCount = ebookService.count(new QueryWrapper<Ebook>()
                .in("category_id", categoryIds)
                .or()
                .in("category2_id", categoryIds)
                .or()
                .in("category1_id", categoryIds));
        if (ebookCount > 0) {
            throw new BusinessException(BusinessExceptionCode.CATEGORY_HAS_EBOOK);
        }
        categoryService.removeById(id);
        OperationLogUtil.save(operationLogService, request, "分类管理", "删除分类", category == null ? "ID：" + id : category.getName());
        return CommonResp.ok("删除成功", null);
    }

    private boolean isSelfOrChild(Long id, Long parent) {
        if (id == null || parent == null || parent == 0) {
            return false;
        }
        return findCategoryAndChildrenIds(id).contains(parent);
    }

    private List<Long> findCategoryAndChildrenIds(Long categoryId) {
        List<Category> categories = categoryService.list();
        Set<Long> result = new LinkedHashSet<>();
        collectCategoryIds(categoryId, categories, result);
        return result.stream().toList();
    }

    private void collectCategoryIds(Long categoryId, List<Category> categories, Set<Long> result) {
        if (categoryId == null || result.contains(categoryId)) {
            return;
        }
        result.add(categoryId);
        for (Category category : categories) {
            if (categoryId.equals(category.getParent())) {
                collectCategoryIds(category.getId(), categories, result);
            }
        }
    }

    private Integer getNextSort(Long parent) {
        Category lastCategory = categoryService.getOne(new QueryWrapper<Category>()
                .eq("parent", parent)
                .orderByDesc("sort")
                .orderByDesc("id")
                .last("limit 1"));
        if (lastCategory == null || lastCategory.getSort() == null) {
            return 1;
        }
        return lastCategory.getSort() + 1;
    }
}
