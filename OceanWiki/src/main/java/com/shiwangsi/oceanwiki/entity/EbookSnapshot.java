// 文件说明：这个实体类对应统计快照相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

// 电子书统计快照实体类，对应 ebook_snapshot 表
// 每天保存一份阅读数、点赞数快照，方便首页做统计
@Data
@TableName("ebook_snapshot")
public class EbookSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    // 快照主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 电子书 id
    @TableField("ebook_id")
    private Long ebookId;

    // 快照日期
    private LocalDate date;

    // 截止当天的总阅读数
    @TableField("view_count")
    private Integer viewCount;

    // 截止当天的总点赞数
    @TableField("vote_count")
    private Integer voteCount;

    // 当天新增阅读数
    @TableField("view_increase")
    private Integer viewIncrease;

    // 当天新增点赞数
    @TableField("vote_increase")
    private Integer voteIncrease;
}

