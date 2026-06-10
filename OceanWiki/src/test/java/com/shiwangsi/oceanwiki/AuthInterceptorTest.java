// 文件说明：这个文件负责登录和权限对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.shiwangsi.oceanwiki.config.auth.AuthInterceptor;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 权限拦截器测试
// 主要验证后台接口不是只靠前端菜单隐藏，而是后端也会检查权限
class AuthInterceptorTest extends BaseIntegrationTest {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Test
    void manageApiShouldRejectRequestWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ebook/save");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        assertThat(pass).isFalse();
        assertThat(response.getContentAsString()).contains("请先登录");
    }

    @Test
    void manageApiShouldPassWhenTokenHasPermission() throws Exception {
        String token = TEST_PREFIX + "token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:manage"));

        HttpServletRequest request = new MockHttpServletRequest("POST", "/ebook/save");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        HttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isTrue();
    }

    @Test
    void statisticApiShouldRejectUserWithoutStatisticPermission() throws Exception {
        String token = TEST_PREFIX + "statistic_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        HttpServletRequest request = new MockHttpServletRequest("GET", "/ebookSnapshot/getStatistic");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isFalse();
        assertThat(response.getContentAsString()).contains("没有操作权限");
    }

    @Test
    void dashboardApiShouldOnlyAllowSuperAdmin() throws Exception {
        String token = TEST_PREFIX + "dashboard_content_admin_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("CONTENT_ADMIN"), List.of("statistics:view"));

        HttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard/overview");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isFalse();
        assertThat(response.getContentAsString()).contains("后台看板仅超级管理员可访问");
    }

    @Test
    void dashboardApiShouldAllowSuperAdmin() throws Exception {
        String token = TEST_PREFIX + "dashboard_super_admin_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("SUPER_ADMIN"), List.of());

        HttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard/overview");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isTrue();
    }

    @Test
    void commentAdminApiShouldRequireCommentManagePermission() throws Exception {
        String token = TEST_PREFIX + "comment_permission_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        HttpServletRequest request = new MockHttpServletRequest("GET", "/comment/admin/list");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isFalse();
        assertThat(response.getContentAsString()).contains("没有操作权限");
    }

    @Test
    void feedbackAdminApiShouldRequireFeedbackManagePermission() throws Exception {
        String token = TEST_PREFIX + "feedback_permission_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        HttpServletRequest request = new MockHttpServletRequest("GET", "/feedback/admin/list");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isFalse();
        assertThat(response.getContentAsString()).contains("没有操作权限");
    }

    @Test
    void operationLogApiShouldRequireOperationLogPermission() throws Exception {
        String token = TEST_PREFIX + "operation_log_permission_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        HttpServletRequest request = new MockHttpServletRequest("GET", "/operationLog/list");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isFalse();
        assertThat(response.getContentAsString()).contains("没有操作权限");
    }

    @Test
    void profileApiShouldNotRequireUserManagePermission() throws Exception {
        String token = TEST_PREFIX + "profile_permission_token";
        AuthTokenStore.put(token, normalUser.getId(), List.of("NORMAL_USER"), List.of("ebook:view"));

        HttpServletRequest request = new MockHttpServletRequest("GET", "/user/profile");
        ((MockHttpServletRequest) request).addHeader("Authorization", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = authInterceptor.preHandle(request, response, new Object());

        AuthTokenStore.remove(token);
        assertThat(pass).isTrue();
    }
}
