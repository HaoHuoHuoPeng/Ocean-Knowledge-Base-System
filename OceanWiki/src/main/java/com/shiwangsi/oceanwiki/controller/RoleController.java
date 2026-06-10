// 文件说明：这个 Controller 负责角色相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.entity.Role;
import com.shiwangsi.oceanwiki.entity.RolePermission;
import com.shiwangsi.oceanwiki.entity.UserRole;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.exception.BusinessExceptionCode;
import com.shiwangsi.oceanwiki.rep.AssignRolePermissionReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.service.IRolePermissionService;
import com.shiwangsi.oceanwiki.service.IRoleService;
import com.shiwangsi.oceanwiki.service.IUserRoleService;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 角色接口
// 这里负责角色列表、新增修改、删除、给角色分配权限
@Tag(name = "角色接口")
@RestController
@RequestMapping("/role")
public class RoleController {

    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String NORMAL_USER_CODE = "NORMAL_USER";

    private final IRoleService roleService;
    private final IRolePermissionService rolePermissionService;
    private final IUserRoleService userRoleService;
    private final IOperationLogService operationLogService;

    public RoleController(IRoleService roleService,
                          IRolePermissionService rolePermissionService,
                          IUserRoleService userRoleService,
                          IOperationLogService operationLogService) {
        this.roleService = roleService;
        this.rolePermissionService = rolePermissionService;
        this.userRoleService = userRoleService;
        this.operationLogService = operationLogService;
    }

    // 查询角色列表
    @Operation(summary = "查询角色列表")
    @GetMapping("/list")
    public CommonResp<List<Role>> list(String keyword) {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item
                    .like("name", keyword)
                    .or()
                    .like("code", keyword));
        }
        wrapper.orderByAsc("sort").orderByAsc("id");
        return CommonResp.ok(roleService.list(wrapper));
    }

    // 查询全部角色，给用户分配角色时使用
    @Operation(summary = "查询全部角色")
    @GetMapping("/all")
    public CommonResp<List<Role>> all() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        return CommonResp.ok(roleService.list(wrapper));
    }

    // 保存角色
    @Operation(summary = "保存角色")
    @PostMapping("/save")
    public CommonResp<Object> save(@RequestBody Role role, HttpServletRequest request) {
        boolean create = role.getId() == null;
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.eq("code", role.getCode());
        Role dbRole = roleService.getOne(wrapper);
        if (dbRole != null && !dbRole.getId().equals(role.getId())) {
            throw new BusinessException(BusinessExceptionCode.ROLE_CODE_EXIST);
        }

        if (role.getBuiltIn() == null) {
            role.setBuiltIn(0);
        }
        if (role.getId() == null) {
            role.setSort(getNextSort());
        } else if (role.getSort() == null) {
            Role oldRole = roleService.getById(role.getId());
            role.setSort(oldRole == null || oldRole.getSort() == null ? getNextSort() : oldRole.getSort());
        }
        roleService.saveOrUpdate(role);

        // 角色表单里选择了权限，就在保存角色资料时一起保存角色权限
        if (role.getPermissionIds() != null) {
            saveRolePermissions(role, role.getPermissionIds());
        }

        OperationLogUtil.save(operationLogService, request, "角色权限管理", create ? "新增角色" : "修改角色", role.getCode());
        return CommonResp.ok("保存成功", null);
    }

    // 删除角色
    @Operation(summary = "删除角色")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Role role = roleService.getById(id);
        if (role == null) {
            throw new BusinessException(BusinessExceptionCode.ROLE_NOT_FOUND);
        }

        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BusinessException(BusinessExceptionCode.SUPER_ADMIN_ROLE_CAN_NOT_DELETE);
        }

        if (NORMAL_USER_CODE.equals(role.getCode())) {
            throw new BusinessException(BusinessExceptionCode.NORMAL_USER_ROLE_CAN_NOT_DELETE);
        }

        if (role.getBuiltIn() != null && role.getBuiltIn() == 1 && !isSuperAdmin(request)) {
            throw new BusinessException(BusinessExceptionCode.ROLE_BUILT_IN_CAN_NOT_DELETE);
        }

        Role normalRole = getNormalRole();
        List<Long> affectedUserIds = userRoleService.list(new QueryWrapper<UserRole>().eq("role_id", id))
                .stream()
                .map(UserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());

        roleService.removeById(id);
        rolePermissionService.remove(new QueryWrapper<RolePermission>().eq("role_id", id));
        userRoleService.remove(new QueryWrapper<UserRole>().eq("role_id", id));
        assignNormalRoleWhenUserHasNoRole(affectedUserIds, normalRole);
        OperationLogUtil.save(operationLogService, request, "角色权限管理", "删除角色", role.getCode());
        return CommonResp.ok("删除成功", null);
    }

    // 查询角色拥有的权限 id
    @Operation(summary = "查询角色权限")
    @GetMapping("/permissionIds/{roleId}")
    public CommonResp<List<Long>> permissionIds(@PathVariable Long roleId) {
        List<Long> permissionIds = rolePermissionService.list(new QueryWrapper<RolePermission>().eq("role_id", roleId))
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        return CommonResp.ok(permissionIds);
    }

    // 给角色分配权限
    @Operation(summary = "给角色分配权限")
    @PostMapping("/assignPermissions")
    public CommonResp<Object> assignPermissions(@RequestBody AssignRolePermissionReq req, HttpServletRequest request) {
        Role role = roleService.getById(req.getRoleId());
        if (role == null) {
            throw new BusinessException(BusinessExceptionCode.ROLE_NOT_FOUND);
        }

        saveRolePermissions(role, req.getPermissionIds());
        OperationLogUtil.save(operationLogService, request, "角色权限管理", "分配权限", role.getCode());
        return CommonResp.ok("分配成功", null);
    }

    private void saveRolePermissions(Role role, List<Long> submittedPermissionIds) {
        List<Long> permissionIds = submittedPermissionIds == null ? new ArrayList<>() : submittedPermissionIds;

        // 超级管理员固定拥有全部权限，页面上也不建议改它
        if (SUPER_ADMIN_CODE.equals(role.getCode()) && CollectionUtils.isEmpty(permissionIds)) {
            throw new BusinessException(BusinessExceptionCode.SUPER_ADMIN_PERMISSION_CAN_NOT_EMPTY);
        }

        rolePermissionService.remove(new QueryWrapper<RolePermission>().eq("role_id", role.getId()));
        for (Long permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permissionId);
            rolePermissionService.save(rolePermission);
        }
    }

    private Integer getNextSort() {
        Role lastRole = roleService.getOne(new QueryWrapper<Role>().orderByDesc("sort").orderByDesc("id").last("limit 1"));
        if (lastRole == null || lastRole.getSort() == null) {
            return 1;
        }
        return lastRole.getSort() + 1;
    }

    private Role getNormalRole() {
        Role normalRole = roleService.getOne(new QueryWrapper<Role>().eq("code", NORMAL_USER_CODE));
        if (normalRole == null) {
            throw new BusinessException(BusinessExceptionCode.NORMAL_USER_ROLE_NOT_FOUND);
        }
        return normalRole;
    }

    private void assignNormalRoleWhenUserHasNoRole(List<Long> userIds, Role normalRole) {
        for (Long userId : userIds) {
            long userRoleCount = userRoleService.count(new QueryWrapper<UserRole>().eq("user_id", userId));
            if (userRoleCount == 0) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(normalRole.getId());
                userRoleService.save(userRole);
            }
        }
    }

    private boolean isSuperAdmin(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        AuthTokenStore.LoginUserInfo loginUserInfo = AuthTokenStore.get(token);
        return loginUserInfo != null
                && loginUserInfo.roleCodes() != null
                && loginUserInfo.roleCodes().contains(SUPER_ADMIN_CODE);
    }
}
