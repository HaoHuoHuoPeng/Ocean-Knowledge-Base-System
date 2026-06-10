// 文件说明：这个 Controller 负责权限相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Permission;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 权限接口
// 当前权限由系统预置，不在页面新增删除，角色只负责勾选这些权限
@Tag(name = "权限接口")
@RestController
@RequestMapping("/permission")
public class PermissionController {

    private final IPermissionService permissionService;

    public PermissionController(IPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // 查询权限列表
    @Operation(summary = "查询权限列表")
    @GetMapping("/list")
    public CommonResp<List<Permission>> list() {
        QueryWrapper<Permission> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        return CommonResp.ok(permissionService.list(wrapper));
    }
}
