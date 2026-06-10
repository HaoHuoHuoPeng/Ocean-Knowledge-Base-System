// 文件说明：这个文件负责角色对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.controller.RoleController;
import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.entity.Permission;
import com.shiwangsi.oceanwiki.entity.Role;
import com.shiwangsi.oceanwiki.entity.RolePermission;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.entity.UserRole;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 角色接口测试
// 主要验证超级管理员删除角色的边界规则
class RoleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoleController roleController;

    @Test
    void normalUserShouldNotDeleteBuiltInRole() {
        Role builtInRole = createBuiltInRole(TEST_PREFIX + "BUILT_IN_NORMAL");
        MockHttpServletRequest request = tokenRequest("normal-token", List.of("NORMAL_USER"));

        assertThatThrownBy(() -> roleController.delete(builtInRole.getId(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void superAdminShouldDeleteBuiltInRoleExceptSuperAdminRole() {
        Role builtInRole = createBuiltInRole(TEST_PREFIX + "BUILT_IN_ADMIN");
        MockHttpServletRequest request = tokenRequest("super-token", List.of("SUPER_ADMIN"));

        roleController.delete(builtInRole.getId(), request);

        assertThat(roleService.getById(builtInRole.getId())).isNull();
    }

    @Test
    void superAdminRoleShouldNeverBeDeleted() {
        Role superAdminRole = roleService.getOne(new QueryWrapper<Role>().eq("code", "SUPER_ADMIN"));
        if (superAdminRole == null) {
            superAdminRole = createBuiltInRole("SUPER_ADMIN");
        }
        Long superAdminRoleId = superAdminRole.getId();
        MockHttpServletRequest request = tokenRequest("super-token-2", List.of("SUPER_ADMIN"));

        assertThatThrownBy(() -> roleController.delete(superAdminRoleId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void saveShouldCreateRoleAndAssignSubmittedPermissions() {
        Permission firstPermission = createPermission(TEST_PREFIX + "role_form_first", "角色表单权限一");
        Permission secondPermission = createPermission(TEST_PREFIX + "role_form_second", "角色表单权限二");
        MockHttpServletRequest request = tokenRequest("save-role-token", List.of("SUPER_ADMIN"));
        Integer maxSortBeforeSave = roleService.getOne(new QueryWrapper<Role>()
                        .orderByDesc("sort")
                        .orderByDesc("id")
                        .last("limit 1"))
                .getSort();

        Role saveReq = new Role();
        saveReq.setCode(TEST_PREFIX + "ROLE_FORM");
        saveReq.setName("表单新增角色");
        saveReq.setDescription("新增角色时一起分配权限");
        saveReq.setBuiltIn(0);
        saveReq.setPermissionIds(List.of(firstPermission.getId(), secondPermission.getId()));

        CommonResp<Object> resp = roleController.save(saveReq, request);
        Role dbRole = roleService.getOne(new QueryWrapper<Role>().eq("code", saveReq.getCode()));
        List<RolePermission> rolePermissions = rolePermissionService.list(
                new QueryWrapper<RolePermission>().eq("role_id", dbRole.getId()));
        long logCount = operationLogService.count(new QueryWrapper<OperationLog>()
                .eq("module", "角色权限管理")
                .eq("action", "新增角色")
                .like("content", saveReq.getCode()));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(dbRole).isNotNull();
        assertThat(dbRole.getSort()).isEqualTo(maxSortBeforeSave + 1);
        assertThat(rolePermissions)
                .extracting(RolePermission::getPermissionId)
                .containsExactlyInAnyOrder(firstPermission.getId(), secondPermission.getId());
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void deleteShouldAssignNormalRoleToAffectedUsers() {
        Role normalUserRole = roleService.getOne(new QueryWrapper<Role>().eq("code", "NORMAL_USER"));
        if (normalUserRole == null) {
            normalUserRole = createBuiltInRole("NORMAL_USER");
        }
        Role customRole = createRole(TEST_PREFIX + "DELETE_TO_NORMAL", "待删除角色");
        User customUser = createUser(TEST_PREFIX + "delete_role_user", "删除角色测试用户");
        userRoleService.remove(new QueryWrapper<UserRole>().eq("user_id", customUser.getId()));
        assignRole(customUser, customRole);
        MockHttpServletRequest request = tokenRequest("delete-role-token", List.of("SUPER_ADMIN"));

        roleController.delete(customRole.getId(), request);
        List<UserRole> userRoles = userRoleService.list(new QueryWrapper<UserRole>().eq("user_id", customUser.getId()));

        assertThat(roleService.getById(customRole.getId())).isNull();
        assertThat(userRoles).extracting(UserRole::getRoleId).containsExactly(normalUserRole.getId());
    }

    @Test
    void normalUserRoleShouldNeverBeDeleted() {
        Role normalUserRole = roleService.getOne(new QueryWrapper<Role>().eq("code", "NORMAL_USER"));
        if (normalUserRole == null) {
            normalUserRole = createBuiltInRole("NORMAL_USER");
        }
        Long normalUserRoleId = normalUserRole.getId();
        MockHttpServletRequest request = tokenRequest("normal-role-delete-token", List.of("SUPER_ADMIN"));

        assertThatThrownBy(() -> roleController.delete(normalUserRoleId, request))
                .isInstanceOf(BusinessException.class);
    }

    private Role createBuiltInRole(String code) {
        Role role = new Role();
        role.setCode(code);
        role.setName("测试内置角色");
        role.setDescription("测试用内置角色");
        role.setBuiltIn(1);
        role.setSort(99);
        roleService.save(role);
        return role;
    }

    private MockHttpServletRequest tokenRequest(String token, List<String> roleCodes) {
        AuthTokenStore.put(token, normalUser.getId(), roleCodes, List.of("role:manage"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);
        return request;
    }
}
