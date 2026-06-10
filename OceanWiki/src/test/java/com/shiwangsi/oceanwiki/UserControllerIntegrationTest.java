// 文件说明：这个文件负责用户对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.entity.OperationLog;
import com.shiwangsi.oceanwiki.entity.Role;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.entity.UserRole;
import com.shiwangsi.oceanwiki.exception.BusinessException;
import com.shiwangsi.oceanwiki.rep.UpdateProfileReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.utils.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 用户接口测试
// 主要验证登录、注册、默认角色这些系统入口逻辑
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private com.shiwangsi.oceanwiki.controller.UserController userController;

    @Test
    void loginShouldReturnTokenAndPermissions() {
        User loginReq = new User();
        loginReq.setLoginName(normalUser.getLoginName());
        loginReq.setPassword("123456");

        CommonResp<Map<String, Object>> resp = userController.login(loginReq);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent()).containsKeys("id", "loginName", "name", "token", "permissions");
        assertThat(resp.getContent().get("token")).asString().isNotBlank();
    }

    @Test
    void loginWithWrongPasswordShouldFail() {
        User loginReq = new User();
        loginReq.setLoginName(normalUser.getLoginName());
        loginReq.setPassword("wrong-password");

        assertThatThrownBy(() -> userController.login(loginReq))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerShouldCreateUserAndAssignNormalRoleWhenRoleExists() {
        // 当前项目注册默认分配 NORMAL_USER
        // 数据库里如果已经有这个角色就直接复用，没有才临时创建
        Role normalUserRole = roleService.getOne(new QueryWrapper<Role>().eq("code", "NORMAL_USER"));
        if (normalUserRole == null) {
            normalUserRole = new Role();
            normalUserRole.setCode("NORMAL_USER");
            normalUserRole.setName("普通用户");
            normalUserRole.setDescription("测试普通用户角色");
            normalUserRole.setBuiltIn(1);
            normalUserRole.setSort(3);
            roleService.save(normalUserRole);
        }

        User registerReq = new User();
        registerReq.setLoginName(TEST_PREFIX + "register");
        registerReq.setName("测试注册用户");
        registerReq.setPassword("123456");

        CommonResp<Object> resp = userController.register(registerReq);
        User dbUser = userService.getOne(new QueryWrapper<User>().eq("login_name", registerReq.getLoginName()));
        List<UserRole> userRoles = userRoleService.list(new QueryWrapper<UserRole>().eq("user_id", dbUser.getId()));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(dbUser).isNotNull();
        assertThat(userRoles).extracting(UserRole::getRoleId).contains(normalUserRole.getId());
    }

    @Test
    void saveShouldCreateUserAndAssignSubmittedRoles() {
        Role customRole = createRole(TEST_PREFIX + "USER_FORM_ROLE", "表单新增用户角色");
        MockHttpServletRequest request = adminRequest(TEST_PREFIX + "save_user_admin_token");

        User saveReq = new User();
        saveReq.setLoginName(TEST_PREFIX + "form_user");
        saveReq.setName("表单新增用户");
        saveReq.setPassword("123456");
        saveReq.setRoleIds(List.of(customRole.getId()));

        CommonResp<Object> resp = userController.save(saveReq, request);
        User dbUser = userService.getOne(new QueryWrapper<User>().eq("login_name", saveReq.getLoginName()));
        List<UserRole> userRoles = userRoleService.list(new QueryWrapper<UserRole>().eq("user_id", dbUser.getId()));
        long logCount = operationLogService.count(new QueryWrapper<OperationLog>()
                .eq("module", "用户管理")
                .eq("action", "新增用户")
                .like("content", saveReq.getLoginName()));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(dbUser).isNotNull();
        assertThat(userRoles).extracting(UserRole::getRoleId).containsExactly(customRole.getId());
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    void saveShouldRejectMultipleRoles() {
        Role firstRole = createRole(TEST_PREFIX + "USER_ROLE_FIRST", "用户角色一");
        Role secondRole = createRole(TEST_PREFIX + "USER_ROLE_SECOND", "用户角色二");

        User saveReq = new User();
        saveReq.setLoginName(TEST_PREFIX + "multi_role_user");
        saveReq.setName("多角色用户");
        saveReq.setPassword("123456");
        saveReq.setRoleIds(List.of(firstRole.getId(), secondRole.getId()));
        MockHttpServletRequest request = adminRequest(TEST_PREFIX + "multi_role_admin_token");

        assertThatThrownBy(() -> userController.save(saveReq, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void saveShouldRejectSuperAdminRole() {
        Role superAdminRole = roleService.getOne(new QueryWrapper<Role>().eq("code", "SUPER_ADMIN"));
        if (superAdminRole == null) {
            superAdminRole = createRole("SUPER_ADMIN", "超级管理员");
        }

        User saveReq = new User();
        saveReq.setLoginName(TEST_PREFIX + "super_admin_assign");
        saveReq.setName("错误超级管理员");
        saveReq.setPassword("123456");
        saveReq.setRoleIds(List.of(superAdminRole.getId()));
        MockHttpServletRequest request = adminRequest(TEST_PREFIX + "super_admin_assign_token");

        assertThatThrownBy(() -> userController.save(saveReq, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateProfileShouldUpdateCurrentUserInfoAndPassword() {
        String token = TEST_PREFIX + "profile_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:read"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);

        UpdateProfileReq req = new UpdateProfileReq();
        req.setLoginName(TEST_PREFIX + "profile_new_account");
        req.setName("新的姓名");
        req.setCurrentPassword("123456");
        req.setNewPassword("654321");

        CommonResp<Map<String, Object>> resp = userController.updateProfile(req, request);
        User dbUser = userService.getById(normalUser.getId());

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent().get("loginName")).isEqualTo(req.getLoginName());
        assertThat(resp.getContent().get("name")).isEqualTo(req.getName());
        assertThat(dbUser.getLoginName()).isEqualTo(req.getLoginName());
        assertThat(dbUser.getName()).isEqualTo(req.getName());
        assertThat(PasswordUtil.matches("654321", dbUser.getPassword())).isTrue();

        AuthTokenStore.remove(token);
    }

    @Test
    void updateProfileShouldRejectWrongCurrentPasswordWhenChangingPassword() {
        String token = TEST_PREFIX + "profile_wrong_password_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:read"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);

        UpdateProfileReq req = new UpdateProfileReq();
        req.setLoginName(normalUser.getLoginName());
        req.setName(normalUser.getName());
        req.setCurrentPassword("wrong-password");
        req.setNewPassword("654321");

        assertThatThrownBy(() -> userController.updateProfile(req, request))
                .isInstanceOf(BusinessException.class);

        AuthTokenStore.remove(token);
    }

    @Test
    void deleteShouldRejectCurrentLoginUser() {
        String token = TEST_PREFIX + "delete_self_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("SUPER_ADMIN"), List.of("user:manage"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);

        assertThatThrownBy(() -> userController.delete(normalUser.getId(), request))
                .isInstanceOf(BusinessException.class);

        AuthTokenStore.remove(token);
    }

    private MockHttpServletRequest adminRequest(String token) {
        AuthTokenStore.put(token, anotherUser.getId(), List.of("SUPER_ADMIN"), List.of("user:manage"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);
        return request;
    }
}
