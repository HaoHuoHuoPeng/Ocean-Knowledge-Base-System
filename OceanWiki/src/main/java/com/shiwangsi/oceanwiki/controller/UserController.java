// 文件说明：这个 Controller 负责用户相关接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.entity.Permission;
import com.shiwangsi.oceanwiki.entity.Role;
import com.shiwangsi.oceanwiki.entity.RolePermission;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.entity.UserRole;
import com.shiwangsi.oceanwiki.excel.UserImportExcel;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.exception.BusinessExceptionCode;
import com.shiwangsi.oceanwiki.rep.AssignUserRoleReq;
import com.shiwangsi.oceanwiki.rep.BatchUserReq;
import com.shiwangsi.oceanwiki.rep.UpdateProfileReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.ImportResp;
import com.shiwangsi.oceanwiki.service.IPermissionService;
import com.shiwangsi.oceanwiki.service.IRolePermissionService;
import com.shiwangsi.oceanwiki.service.IRoleService;
import com.shiwangsi.oceanwiki.service.IUserRoleService;
import com.shiwangsi.oceanwiki.service.IUserService;
import com.shiwangsi.oceanwiki.service.IOperationLogService;
import com.shiwangsi.oceanwiki.utils.ExcelImportUtil;
import com.shiwangsi.oceanwiki.utils.OperationLogUtil;
import com.shiwangsi.oceanwiki.utils.PasswordUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 用户管理接口
// 这里负责登录、注册、用户管理、给用户分配角色
@Tag(name = "用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {

    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final String NORMAL_USER_CODE = "NORMAL_USER";

    private final IUserService userService;
    private final IRoleService roleService;
    private final IPermissionService permissionService;
    private final IUserRoleService userRoleService;
    private final IRolePermissionService rolePermissionService;
    private final IOperationLogService operationLogService;

    public UserController(IUserService userService,
                          IRoleService roleService,
                          IPermissionService permissionService,
                          IUserRoleService userRoleService,
                          IRolePermissionService rolePermissionService,
                          IOperationLogService operationLogService) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userRoleService = userRoleService;
        this.rolePermissionService = rolePermissionService;
        this.operationLogService = operationLogService;
    }

    // 查询用户列表
    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    public CommonResp<List<User>> list(String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item
                    .like("login_name", keyword)
                    .or()
                    .like("name", keyword));
        }
        wrapper.orderByAsc("id");

        List<User> users = userService.list(wrapper);
        users.forEach(user -> {
            fillRoleAndPermission(user);

            // 列表不返回密码，避免前端表格里看到明文密码
            user.setPassword(null);
        });
        return CommonResp.ok(users);
    }

    // 保存用户
    @Operation(summary = "保存用户")
    @PostMapping("/save")
    public CommonResp<Object> save(@RequestBody User user, HttpServletRequest request) {
        boolean create = user.getId() == null;
        checkLoginName(user);

        if (user.getId() == null && !StringUtils.hasText(user.getPassword())) {
            user.setPassword("123456");
        }

        if (user.getId() != null && !StringUtils.hasText(user.getPassword())) {
            User dbUser = userService.getById(user.getId());
            if (dbUser != null) {
                user.setPassword(dbUser.getPassword());
            }
        }

        if (StringUtils.hasText(user.getPassword()) && !PasswordUtil.isBCrypt(user.getPassword())) {
            user.setPassword(PasswordUtil.encode(user.getPassword()));
        }

        boolean hasSubmittedRoles = user.getRoleIds() != null;
        if (hasSubmittedRoles) {
            checkUserRoles(user.getId(), user.getRoleIds());
        }
        userService.saveOrUpdate(user);

        // 用户表单里选择了角色，就在保存用户资料时一起保存用户角色
        if (hasSubmittedRoles) {
            assignRoles(user.getId(), user.getRoleIds());
        }

        // 如果前端没有提交角色，并且这个用户还没有任何角色，就默认给普通用户角色
        if (!hasSubmittedRoles && user.getId() != null && CollectionUtils.isEmpty(getRoleIdsByUserId(user.getId()))) {
            Role normalRole = getRoleByCode(NORMAL_USER_CODE);
            if (normalRole != null) {
                assignRoles(user.getId(), List.of(normalRole.getId()));
            }
        }

        OperationLogUtil.save(operationLogService, request, "用户管理", create ? "新增用户" : "修改用户", user.getLoginName());
        return CommonResp.ok("保存成功", null);
    }

    // 删除用户
    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (id.equals(currentUserId)) {
            throw new BusinessException(BusinessExceptionCode.CURRENT_USER_CAN_NOT_DELETE);
        }

        if (isSuperAdminUser(id)) {
            throw new BusinessException(BusinessExceptionCode.SUPER_ADMIN_CAN_NOT_DELETE);
        }

        userService.removeById(id);
        userRoleService.remove(new QueryWrapper<UserRole>().eq("user_id", id));
        OperationLogUtil.save(operationLogService, request, "用户管理", "删除用户", "用户ID：" + id);
        return CommonResp.ok("删除成功", null);
    }

    // 重置密码
    @Operation(summary = "重置密码")
    @PostMapping("/resetPassword")
    public CommonResp<Object> resetPassword(@RequestBody User user, HttpServletRequest request) {
        User dbUser = userService.getById(user.getId());
        if (dbUser != null) {
            String rawPassword = StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456";
            dbUser.setPassword(PasswordUtil.encode(rawPassword));
            userService.updateById(dbUser);
            OperationLogUtil.save(operationLogService, request, "用户管理", "重置密码", dbUser.getLoginName());
        }
        return CommonResp.ok("重置成功", null);
    }

    // 批量重置密码
    // 管理员勾选多个用户后，可以一次性把这些用户的密码重置成默认密码
    @Operation(summary = "批量重置密码")
    @PostMapping("/batchResetPassword")
    public CommonResp<Object> batchResetPassword(@RequestBody BatchUserReq req, HttpServletRequest request) {
        List<Long> userIds = safeUserIds(req.getUserIds());
        if (CollectionUtils.isEmpty(userIds)) {
            return CommonResp.fail("请选择要重置密码的用户");
        }

        Long currentUserId = getCurrentUserId(request);
        if (userIds.contains(currentUserId)) {
            return CommonResp.fail("批量重置密码不能包含当前登录用户");
        }
        for (Long userId : userIds) {
            if (isSuperAdminUser(userId)) {
                return CommonResp.fail("批量重置密码不能包含超级管理员");
            }
        }

        String rawPassword = StringUtils.hasText(req.getPassword()) ? req.getPassword() : "123456";
        List<User> users = userService.listByIds(userIds);
        if (users.size() != userIds.size()) {
            return CommonResp.fail("部分用户不存在，请刷新后重新选择");
        }
        String encodedPassword = PasswordUtil.encode(rawPassword);
        for (User dbUser : users) {
            dbUser.setPassword(encodedPassword);
        }
        userService.updateBatchById(users);
        OperationLogUtil.save(operationLogService, request, "用户管理", "批量重置密码", "用户ID：" + userIds);
        return CommonResp.ok("批量重置成功", null);
    }

    // 下载用户导入模板
    // 模板里不提供角色列，导入成功后统一分配普通用户角色
    @Operation(summary = "下载用户导入模板")
    @GetMapping("/get-import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<UserImportExcel> templateRows = List.of(
                UserImportExcel.builder()
                        .loginName("reader001")
                        .name("普通读者")
                        .password("123456")
                        .build()
        );
        ExcelImportUtil.write(response, "用户导入模板.xlsx", "用户", UserImportExcel.class, templateRows);
    }

    // 批量导入用户 Excel
    // Excel 表头：账号、姓名、密码
    // 密码为空时默认 123456；角色不从文件导入，统一分配普通用户角色
    @Operation(summary = "批量导入用户 Excel")
    @PostMapping("/import")
    public CommonResp<ImportResp> importExcel(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        List<UserImportExcel> rows;
        try {
            rows = ExcelImportUtil.read(file, UserImportExcel.class);
        } catch (IllegalArgumentException e) {
            return CommonResp.fail(e.getMessage());
        }
        if (rows.isEmpty()) {
            return CommonResp.fail("Excel 中没有可导入的数据");
        }

        Role normalRole = getRoleByCode(NORMAL_USER_CODE);
        if (normalRole == null) {
            throw new BusinessException(BusinessExceptionCode.NORMAL_USER_ROLE_NOT_FOUND);
        }

        List<User> importList = new ArrayList<>();
        ImportResp importResp = new ImportResp();
        Set<String> fileLoginNames = new LinkedHashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            UserImportExcel row = rows.get(i);
            int lineNo = i + 2;
            String rowKey = "第 " + lineNo + " 行";
            String loginName = row.getLoginName();
            String name = row.getName();
            String password = row.getPassword();

            if (!StringUtils.hasText(loginName)) {
                importResp.addFailure(rowKey, "账号不能为空");
                continue;
            }
            rowKey = "第 " + lineNo + " 行：" + loginName;
            if (!StringUtils.hasText(name)) {
                importResp.addFailure(rowKey, "姓名不能为空");
                continue;
            }
            if (!fileLoginNames.add(loginName)) {
                importResp.addFailure(rowKey, "Excel 文件内账号重复");
                continue;
            }
            if (userService.getOne(new QueryWrapper<User>().eq("login_name", loginName)) != null) {
                importResp.addFailure(rowKey, "账号已存在");
                continue;
            }

            User user = new User();
            user.setLoginName(loginName);
            user.setName(name);
            user.setPassword(PasswordUtil.encode(StringUtils.hasText(password) ? password : "123456"));
            importList.add(user);
            importResp.addSuccess(loginName);
        }

        if (!importList.isEmpty()) {
            userService.saveBatch(importList);
            for (User importedUser : importList) {
                assignRoles(importedUser.getId(), List.of(normalRole.getId()));
            }
        }
        OperationLogUtil.save(operationLogService, request, "用户管理", "批量导入用户", "导入数量：" + importList.size());
        return CommonResp.ok("导入完成，成功 " + importResp.getSuccessNames().size() + " 条，失败 " + importResp.getFailureMap().size() + " 条，成功用户默认角色为普通用户", importResp);
    }

    // 登录接口
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public CommonResp<Map<String, Object>> login(@RequestBody User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", user.getLoginName());
        User dbUser = userService.getOne(wrapper);
        if (dbUser == null || !PasswordUtil.matches(user.getPassword(), dbUser.getPassword())) {
            throw new BusinessException(BusinessExceptionCode.LOGIN_USER_ERROR);
        }

        if (!PasswordUtil.isBCrypt(dbUser.getPassword())) {
            dbUser.setPassword(PasswordUtil.encode(user.getPassword()));
            userService.updateById(dbUser);
        }

        fillRoleAndPermission(dbUser);
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthTokenStore.put(token, dbUser.getId(), dbUser.getRoleCodes(), dbUser.getPermissions());

        Map<String, Object> result = buildLoginUserInfo(dbUser);
        result.put("token", token);
        return CommonResp.ok("登录成功", result);
    }

    // 查询当前登录用户资料
    @Operation(summary = "查询当前用户资料")
    @GetMapping("/profile")
    public CommonResp<Map<String, Object>> profile(HttpServletRequest request) {
        User dbUser = getCurrentUser(request);
        fillRoleAndPermission(dbUser);
        Map<String, Object> result = buildLoginUserInfo(dbUser);
        result.put("token", request.getHeader("Authorization"));
        return CommonResp.ok(result);
    }

    // 修改当前登录用户自己的账号、姓名和密码
    @Operation(summary = "修改当前用户资料")
    @PostMapping("/profile/update")
    public CommonResp<Map<String, Object>> updateProfile(@RequestBody UpdateProfileReq req, HttpServletRequest request) {
        User dbUser = getCurrentUser(request);
        if (!StringUtils.hasText(req.getLoginName()) || !StringUtils.hasText(req.getName())) {
            throw new BusinessException(BusinessExceptionCode.REGISTER_INFO_ERROR);
        }

        User updateUser = new User();
        updateUser.setId(dbUser.getId());
        updateUser.setLoginName(req.getLoginName());
        updateUser.setName(req.getName());
        checkLoginName(updateUser);

        dbUser.setLoginName(req.getLoginName());
        dbUser.setName(req.getName());

        if (StringUtils.hasText(req.getNewPassword())) {
            if (!StringUtils.hasText(req.getCurrentPassword())
                    || !PasswordUtil.matches(req.getCurrentPassword(), dbUser.getPassword())) {
                throw new BusinessException(BusinessExceptionCode.CURRENT_PASSWORD_ERROR);
            }
            dbUser.setPassword(PasswordUtil.encode(req.getNewPassword()));
        }

        userService.updateById(dbUser);
        OperationLogUtil.save(operationLogService, request, "账号资料", "修改个人资料", dbUser.getLoginName());
        fillRoleAndPermission(dbUser);

        String token = request.getHeader("Authorization");
        AuthTokenStore.put(token, dbUser.getId(), dbUser.getRoleCodes(), dbUser.getPermissions());
        Map<String, Object> result = buildLoginUserInfo(dbUser);
        result.put("token", token);
        return CommonResp.ok("资料保存成功", result);
    }

    // 注册接口
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public CommonResp<Object> register(@RequestBody User user) {
        if (!StringUtils.hasText(user.getLoginName())
                || !StringUtils.hasText(user.getName())
                || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(BusinessExceptionCode.REGISTER_INFO_ERROR);
        }

        checkLoginName(user);
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        userService.save(user);

        // 注册用户默认普通用户，不能直接注册成管理员
        Role normalRole = getRoleByCode(NORMAL_USER_CODE);
        if (normalRole != null) {
            assignRoles(user.getId(), List.of(normalRole.getId()));
        }
        return CommonResp.ok("注册成功，请登录", null);
    }

    // 查询某个用户当前拥有的角色 id
    @Operation(summary = "查询用户角色")
    @GetMapping("/roleIds/{userId}")
    public CommonResp<List<Long>> roleIds(@PathVariable Long userId) {
        return CommonResp.ok(getRoleIdsByUserId(userId));
    }

    // 给用户分配角色
    @Operation(summary = "给用户分配角色")
    @PostMapping("/assignRoles")
    public CommonResp<Object> assignRoles(@RequestBody AssignUserRoleReq req, HttpServletRequest request) {
        assignRoles(req.getUserId(), req.getRoleIds());
        OperationLogUtil.save(operationLogService, request, "用户管理", "分配角色", "用户ID：" + req.getUserId());
        return CommonResp.ok("分配成功", null);
    }

    // 批量分配角色
    // 一个用户只能拥有一个角色，所以批量操作时也是把勾选用户统一改成同一个角色
    @Operation(summary = "批量分配角色")
    @PostMapping("/batchAssignRoles")
    public CommonResp<Object> batchAssignRoles(@RequestBody BatchUserReq req, HttpServletRequest request) {
        List<Long> userIds = safeUserIds(req.getUserIds());
        if (CollectionUtils.isEmpty(userIds)) {
            return CommonResp.fail("请选择要分配角色的用户");
        }
        if (req.getRoleId() == null) {
            return CommonResp.fail("请选择要分配的角色");
        }

        Role role = roleService.getById(req.getRoleId());
        if (role == null) {
            throw new BusinessException(BusinessExceptionCode.ROLE_NOT_FOUND);
        }
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BusinessException(BusinessExceptionCode.SUPER_ADMIN_CAN_NOT_ASSIGN);
        }
        if (userService.listByIds(userIds).size() != userIds.size()) {
            return CommonResp.fail("部分用户不存在，请刷新后重新选择");
        }
        for (Long userId : userIds) {
            if (isSuperAdminUser(userId)) {
                return CommonResp.fail("批量分配角色不能包含超级管理员");
            }
        }

        for (Long userId : userIds) {
            assignRoles(userId, List.of(req.getRoleId()));
        }
        OperationLogUtil.save(operationLogService, request, "用户管理", "批量分配角色", "用户ID：" + userIds + "，角色：" + role.getCode());
        return CommonResp.ok("批量分配成功", null);
    }

    // 退出登录，当前版本前端清掉 token 即可，后端先返回成功
    @Operation(summary = "退出登录")
    @GetMapping("/logout/{token}")
    public CommonResp<Object> logout(@PathVariable String token) {
        AuthTokenStore.remove(token);
        return CommonResp.ok("退出成功", null);
    }

    private void checkLoginName(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", user.getLoginName());
        User dbUser = userService.getOne(wrapper);
        if (dbUser != null && !dbUser.getId().equals(user.getId())) {
            throw new BusinessException(BusinessExceptionCode.USER_LOGIN_NAME_EXIST);
        }
    }

    private List<Long> safeUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new ArrayList<>();
        }
        return userIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private Role getRoleByCode(String code) {
        return roleService.getOne(new QueryWrapper<Role>().eq("code", code));
    }

    private List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleService.list(new QueryWrapper<UserRole>().eq("user_id", userId))
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        List<Long> finalRoleIds = roleIds == null ? new ArrayList<>() : roleIds;
        checkUserRoles(userId, finalRoleIds);

        userRoleService.remove(new QueryWrapper<UserRole>().eq("user_id", userId));
        for (Long roleId : finalRoleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleService.save(userRole);
        }
    }

    private void checkUserRoles(Long userId, List<Long> roleIds) {
        List<Long> finalRoleIds = roleIds == null ? new ArrayList<>() : roleIds;
        Role superAdminRole = getRoleByCode(SUPER_ADMIN_CODE);

        if (finalRoleIds.size() > 1) {
            throw new BusinessException(BusinessExceptionCode.USER_ROLE_ONLY_ONE);
        }

        if (superAdminRole != null) {
            boolean willHaveSuperAdmin = finalRoleIds.contains(superAdminRole.getId());
            boolean alreadyHasSuperAdmin = getRoleIdsByUserId(userId).contains(superAdminRole.getId());

            // 用户管理页面不能新增超级管理员，也不能把普通用户改成超级管理员
            if (willHaveSuperAdmin) {
                throw new BusinessException(BusinessExceptionCode.SUPER_ADMIN_CAN_NOT_ASSIGN);
            }

            // 已经是唯一超级管理员时，不允许把这个角色移除
            if (alreadyHasSuperAdmin && !willHaveSuperAdmin) {
                throw new BusinessException(BusinessExceptionCode.SUPER_ADMIN_ONLY_ONE);
            }
        }
    }

    private boolean isSuperAdminUser(Long userId) {
        Role superAdminRole = getRoleByCode(SUPER_ADMIN_CODE);
        return superAdminRole != null && getRoleIdsByUserId(userId).contains(superAdminRole.getId());
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        AuthTokenStore.LoginUserInfo loginUserInfo = AuthTokenStore.get(token);
        return loginUserInfo == null ? null : loginUserInfo.userId();
    }

    private User getCurrentUser(HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            throw new BusinessException(BusinessExceptionCode.LOGIN_USER_ERROR);
        }

        User dbUser = userService.getById(currentUserId);
        if (dbUser == null) {
            throw new BusinessException(BusinessExceptionCode.LOGIN_USER_ERROR);
        }
        return dbUser;
    }

    private Map<String, Object> buildLoginUserInfo(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("loginName", user.getLoginName());
        result.put("name", user.getName());
        result.put("roleIds", user.getRoleIds());
        result.put("roleNames", user.getRoleNames());
        result.put("roleCodes", user.getRoleCodes());
        result.put("permissions", user.getPermissions());
        return result;
    }

    private void fillRoleAndPermission(User user) {
        List<Long> roleIds = getRoleIdsByUserId(user.getId());
        user.setRoleIds(roleIds);

        if (CollectionUtils.isEmpty(roleIds)) {
            user.setRoleNames(new ArrayList<>());
            user.setRoleCodes(new ArrayList<>());
            user.setPermissions(new ArrayList<>());
            return;
        }

        List<Role> roles = roleService.list(new QueryWrapper<Role>().in("id", roleIds));
        user.setRoleNames(roles.stream().map(Role::getName).collect(Collectors.toList()));
        user.setRoleCodes(roles.stream().map(Role::getCode).collect(Collectors.toList()));

        List<Long> permissionIds = rolePermissionService.list(new QueryWrapper<RolePermission>().in("role_id", roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(permissionIds)) {
            user.setPermissions(new ArrayList<>());
            return;
        }

        List<String> permissions = permissionService.list(new QueryWrapper<Permission>().in("id", permissionIds))
                .stream()
                .map(Permission::getCode)
                .distinct()
                .collect(Collectors.toList());
        user.setPermissions(permissions);
    }
}
