// 文件说明：这个实体类对应阅读历史相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 阅读历史实体类，对应 reading_history 表
// 每个用户每篇文档只保留一条最近阅读记录
@Data
@TableName("reading_history")
public class ReadingHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    // 阅读历史主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 阅读用户 id
    @TableField("user_id")
    private Long userId;

    // 电子书 id
    @TableField("ebook_id")
    private Long ebookId;

    // 文档 id
    @TableField("doc_id")
    private Long docId;

    // 最近阅读时间
    @TableField("read_time")
    private LocalDateTime readTime;

    // 阅读进度百分比，0 到 100，用来展示用户大概读到什么程度
    @TableField("progress")
    private Integer progress;

    // 电子书名称，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String ebookName;

    // 文档名称，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String docName;
}

