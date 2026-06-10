// 文件说明：这个文件负责MybatisPlusCodeGenerator对应模块的代码逻辑。
package com.shiwangsi.oceanwiki.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

/*
 * MyBatis-Plus 代码逆向生成器。
 * 它的作用是根据数据库表，自动生成 entity、mapper、service、controller 和 mapper XML。
 *
 * 注意：
 * 1. 这个类只是在需要生成代码时手动运行，不是项目启动类。
 * 2. 如果数据库表结构改了，可以重新运行这个 main 方法生成代码。
 * 3. 当前配置不会覆盖同名文件，避免把你已经写过的注释和业务代码冲掉。
 */
public class MybatisPlusCodeGenerator {

    public static void main(String[] args) {
        // 当前项目根目录，例如 C:\Users\Asus\Desktop\YueQian\Code\OceanWiki
        String projectPath = "C:/Users/Asus/Desktop/YueQian/Code/OceanWiki";

        // 数据库连接信息，要和 application.properties 里的配置保持一致
        // 密码从环境变量读取，避免把本机真实密码上传到 GitHub
        String url = "jdbc:mysql://localhost:3306/oceanwiki?characterEncoding=UTF8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("请先配置环境变量 DB_PASSWORD，再运行代码生成器");
        }

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder
                            // 生成代码里的作者名，可以改成你自己的名字
                            .author("shiwangsi")
                            // 生成完成后不要自动打开文件夹
                            .disableOpenDir()
                            // Java 文件输出位置
                            .outputDir(projectPath + "/src/main/java");
                })
                .packageConfig(builder -> {
                    builder
                            // 生成代码的父包名
                            .parent("com.shiwangsi.oceanwiki")
                            // mapper XML 文件输出到 resources/mapper 目录
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    builder
                            // 这里写要生成代码的数据库表名
                            .addInclude("user", "category", "ebook", "doc", "content", "ebook_snapshot")
                            // 生成实体类
                            .entityBuilder()
                            // 使用数据库自增主键
                            .enableTableFieldAnnotation()
                            // 生成 mapper 接口和 mapper XML
                            .mapperBuilder()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            // 生成 service 和 serviceImpl
                            .serviceBuilder()
                            // 生成 REST 风格 controller
                            .controllerBuilder()
                            .enableRestStyle();
                })
                // 使用 velocity 模板引擎，pom.xml 里已经引入 velocity-engine-core
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
