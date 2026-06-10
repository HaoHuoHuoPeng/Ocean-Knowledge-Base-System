// 文件说明：这个请求包装类负责清理参数和请求体里的危险脚本，同时保留文档正文需要的富文本格式。
package com.shiwangsi.oceanwiki.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/*
 * XSS 请求包装类。
 * 普通字段使用严格过滤，避免用户把脚本内容提交到系统里。
 * 文档正文是 wangEditor 生成的富文本 HTML，不能按普通文本过滤，否则 <p>、<br>、table、img 等标签会丢失。
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?is)<script\\b[^>]*>.*?</script>");
    private static final Pattern EVENT_PATTERN = Pattern.compile("(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("(?i)(href|src)\\s*=\\s*([\"'])\\s*javascript:[^\"']*\\2");

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return filter(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                values[i] = filter(values[i]);
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        Map<String, String[]> result = new LinkedHashMap<>();
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                String[] values = parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    values[i] = filter(values[i]);
                }
                result.put(key, values);
            }
        }
        return result;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return filter(value);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in = super.getInputStream();
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader buffer = new BufferedReader(reader);
        StringBuilder body = new StringBuilder();

        String line = buffer.readLine();
        while (line != null) {
            body.append(line);
            line = buffer.readLine();
        }

        buffer.close();
        reader.close();
        in.close();

        String requestBody = body.toString();
        if (StrUtil.isBlank(requestBody)) {
            return buildInputStream(requestBody);
        }

        Map<String, Object> map;
        try {
            map = JSONUtil.parseObj(requestBody);
        } catch (Exception e) {
            return buildInputStream(requestBody);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : map.keySet()) {
            Object val = map.get(key);
            if (val instanceof String) {
                String text = val.toString();
                result.put(key, shouldKeepRichText(key) ? filterRichText(text) : filter(text));
            } else {
                result.put(key, val);
            }
        }
        return buildInputStream(JSONUtil.toJsonStr(result));
    }

    private boolean shouldKeepRichText(String key) {
        String uri = getRequestURI();
        return "content".equals(key) && ("/doc/save".equals(uri) || "/doc/submit".equals(uri));
    }

    private String filter(String value) {
        if (!StrUtil.hasEmpty(value)) {
            return HtmlUtil.filter(value);
        }
        return value;
    }

    private String filterRichText(String value) {
        if (StrUtil.hasEmpty(value)) {
            return value;
        }
        String cleaned = SCRIPT_PATTERN.matcher(value).replaceAll("");
        cleaned = EVENT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = JAVASCRIPT_PATTERN.matcher(cleaned).replaceAll("$1=\"#\"");
        return cleaned;
    }

    private ServletInputStream buildInputStream(String value) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }
}
