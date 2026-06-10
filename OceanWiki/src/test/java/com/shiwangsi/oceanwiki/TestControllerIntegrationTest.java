// 文件说明：这个测试类负责验证 /test 测试接口，确保开发调试接口能正常返回。
package com.shiwangsi.oceanwiki;

import com.shiwangsi.oceanwiki.controller.TestController;
import com.shiwangsi.oceanwiki.rep.DemoReq;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 测试接口集成测试
// 这些接口只做健康检查和基础数据检查，不修改业务数据
class TestControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestController testController;

    @Test
    void healthShouldReturnUpStatus() {
        CommonResp<Map<String, Object>> resp = testController.health();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent()).containsEntry("status", "UP");
        assertThat(resp.getContent()).containsKey("time");
    }

    @Test
    void databaseCountShouldReturnBasicTableCounts() {
        CommonResp<Map<String, Long>> resp = testController.databaseCount();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent().get("userCount")).isGreaterThanOrEqualTo(2);
        assertThat(resp.getContent().get("ebookCount")).isGreaterThanOrEqualTo(1);
        assertThat(resp.getContent().get("docCount")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void sayHelloShouldEchoRequestBody() {
        DemoReq req = new DemoReq();
        req.setName("测试请求");
        req.setPhone("13800138000");
        req.setPassword("123456");

        CommonResp<DemoReq> resp = testController.sayHello(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent().getName()).isEqualTo("测试请求");
        assertThat(resp.getContent().getPhone()).isEqualTo("13800138000");
    }
}
