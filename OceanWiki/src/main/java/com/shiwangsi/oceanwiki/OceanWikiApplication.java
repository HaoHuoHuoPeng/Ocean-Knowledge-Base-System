// 文件说明：这是后端启动入口，用来启动 Spring Boot 项目并扫描 MyBatis-Plus Mapper。
package com.shiwangsi.oceanwiki;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

// 扫描 mapper 包，MyBatis-Plus 才能找到 XxxMapper 接口
@MapperScan("com.shiwangsi.oceanwiki.mapper")
// 扫描 Servlet 组件，这里主要是为了让 XssFilter 过滤器生效
@ServletComponentScan
// Spring Boot 启动入口
@SpringBootApplication
public class OceanWikiApplication {

    private static final Logger LOG = LoggerFactory.getLogger(OceanWikiApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OceanWikiApplication.class);
        ConfigurableEnvironment env = app.run(args).getEnvironment();

        // 这几行日志是给你启动后快速找到访问地址用的
        LOG.info("项目启动成功");
        LOG.info("后端地址：http://127.0.0.1:{}", env.getProperty("server.port"));
        LOG.info("Knife4j 地址：http://127.0.0.1:{}/doc.html", env.getProperty("server.port"));
    }
}
