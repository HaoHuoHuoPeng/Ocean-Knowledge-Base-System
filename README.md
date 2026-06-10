# Ocean-Knowledge-Base-System

基于 Spring Boot、MyBatis-Plus、Vue3、TypeScript 和 Ant Design Vue 的海洋知识库系统，支持电子书管理、文档投稿审核、评论反馈、权限管理、阅读收藏和小游戏等功能。

## 项目结构

- `OceanWiki`：后端 Spring Boot 项目
- `OceanWiki-front`：前端 Vue3 + TypeScript 项目

## 本地运行说明

后端数据库配置使用环境变量读取：

- `DB_USERNAME`：MySQL 账号，默认 `root`
- `DB_PASSWORD`：MySQL 密码

初始化数据库脚本在 `OceanWiki/src/main/resources/db` 目录下。
