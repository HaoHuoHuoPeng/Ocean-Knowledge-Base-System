// 文件说明：这个测试类负责验证 XSS 请求包装类不会破坏文档正文的富文本格式。
package com.shiwangsi.oceanwiki;

import com.shiwangsi.oceanwiki.config.xss.XssHttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// XSS 过滤测试
// 重点验证文档正文和普通文本字段的过滤规则是分开的
class XssHttpServletRequestWrapperTest {

    @Test
    void docContentShouldKeepParagraphHtmlAndCleanDangerousScript() throws Exception {
        // 文档正文是富文本，段落、缩进、加粗这些 HTML 结构必须保留
        String json = """
                {
                  "name": "orca history",
                  "content": "<p style=\\"text-indent: 2em;\\">first paragraph</p><p>second paragraph</p><img src=\\"x\\" onerror=\\"alert(1)\\"><script>alert(2)</script><a href=\\"javascript:alert(3)\\">link</a>"
                }
                """;
        XssHttpServletRequestWrapper wrapper = wrapper("/doc/save", json);

        String filtered = new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(filtered).contains("<p style=\\\"text-indent: 2em;\\\">first paragraph</p>");
        assertThat(filtered).contains("<p>second paragraph</p>");
        assertThat(filtered).doesNotContain("<script>");
        assertThat(filtered).doesNotContain("onerror");
        assertThat(filtered).doesNotContain("javascript:");
    }

    @Test
    void commentContentShouldUseStrictTextFilter() throws Exception {
        // 评论不是富文本，不能把用户提交的 HTML 标签原样保存
        String json = """
                {
                  "docId": 1,
                  "content": "<img src=x onerror=alert(1)>normal comment"
                }
                """;
        XssHttpServletRequestWrapper wrapper = wrapper("/comment/submit", json);

        String filtered = new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(filtered).doesNotContain("onerror");
        assertThat(filtered).contains("normal comment");
    }

    private XssHttpServletRequestWrapper wrapper(String uri, String json) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        return new XssHttpServletRequestWrapper(request);
    }
}
