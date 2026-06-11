// 文件说明：这个实体类对应文档相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

// 文档实体类，对应数据库中的 doc 表
// 一本电子书下面可以有多个文档，文档之间也可以通过 parent 组成目录树
@Data
@TableName("doc")
public class Doc implements Serializable {

    private static final long serialVersionUID = 1L;

    // 文档主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 所属电子书 id
    @TableField("ebook_id")
    private Long ebookId;

    // 父文档 id，0 表示顶级文档
    private Long parent;

    // 文档标题
    private String name;

    // 排序值，数字越小越靠前
    private Integer sort;

    // 阅读数
    @TableField("view_count")
    private Integer viewCount;

    // 点赞数
    @TableField("vote_count")
    private Integer voteCount;

    // 发布状态：draft 草稿，pending 待审核，published 已发布，rejected 已驳回，offline 已下架
    private String status;

    // 审核或下架原因，方便内容管理员知道为什么没有发布
    @TableField("review_remark")
    private String reviewRemark;

    // 创建人 id，普通用户投稿时用来记录是谁提交的
    @TableField("create_user_id")
    private Long createUserId;

    // 投稿人姓名，只给前端展示使用，不保存到 doc 表
    @TableField(exist = false)
    private String createUserName;

    // 正文内容不在 doc 表里，而是在 content 表里；这个字段只给前后端传参使用
    @TableField(exist = false)
    private String content;

    // 前端文档管理树形表格使用的子文档列表，不保存到 doc 表
    @TableField(exist = false)
    private List<Doc> children;
}

