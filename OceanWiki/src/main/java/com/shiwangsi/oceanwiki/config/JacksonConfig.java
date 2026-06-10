// 文件说明：这个配置类负责处理后端 JSON 序列化，避免雪花算法生成的 Long 类型 ID 在前端丢失精度。
package com.shiwangsi.oceanwiki.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Jackson 配置
// 雪花算法生成的 ID 通常是 19 位数字，已经超过 JavaScript 安全整数范围
// 所以后端返回 JSON 时，把 Long 转成字符串，前端就不会出现 ID 精度丢失
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
