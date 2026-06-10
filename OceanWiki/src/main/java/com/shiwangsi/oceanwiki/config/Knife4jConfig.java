// 文件说明：这个配置类负责 Knife4j 接口文档，用来设置 /doc.html 页面的标题和说明。
package com.shiwangsi.oceanwiki.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Knife4j 配置类
// Spring Boot 3.x 使用 OpenAPI3 写法，启动后访问 http://127.0.0.1:8881/doc.html
@Configuration
@EnableKnife4j
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 这里配置接口文档首页显示的标题、版本和说明
        return new OpenAPI()
                .info(new Info()
                        .title("海洋知识库系统接口文档")
                        .version("1.0")
                        .description("OceanWiki 后端接口文档"));
    }
}
