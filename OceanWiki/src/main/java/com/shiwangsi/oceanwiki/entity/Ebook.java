// 文件说明：这个实体类对应电子书相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 电子书实体类，对应数据库中的 ebook 表
// 这里的一本电子书，可以理解成一种海洋生物的资料卡片
@Data
@TableName("ebook")
public class Ebook implements Serializable {

    private static final long serialVersionUID = 1L;

    // 电子书主键 id，由 MyBatis-Plus 雪花算法生成
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 电子书名称，例如“虎鲸”
    private String name;

    // 当前所属分类 id，可以是任意层级分类
    @TableField("category_id")
    private Long categoryId;

    // 一级分类 id，旧字段继续保留，兼容之前已经存在的数据
    @TableField("category1_id")
    private Long category1Id;

    // 二级分类 id，旧字段继续保留，兼容之前已经存在的数据
    @TableField("category2_id")
    private Long category2Id;

    // 电子书简介，首页卡片上会显示这段文字
    private String description;

    // 封面图片地址，用户这次提供的 OSS 图片地址会保存到这里
    private String cover;

    // 发布状态：draft 草稿，pending 待审核，published 已发布，rejected 已驳回，offline 已下架
    private String status;

    // 下架原因，电子书被下架时记录，方便后台追踪原因
    @TableField("offline_reason")
    private String offlineReason;

    // 审核备注，审核驳回或审核通过时记录审核人说明
    @TableField("review_remark")
    private String reviewRemark;

    // 文档数量，根据 doc 表自动统计后写回
    @TableField("doc_count")
    private Integer docCount;

    // 阅读数，根据 doc 表阅读数汇总后写回
    @TableField("view_count")
    private Integer viewCount;

    // 点赞数，根据 doc 表点赞数汇总后写回
    @TableField("vote_count")
    private Integer voteCount;

    // 推荐指数，不是数据库字段，由后端推荐算法临时计算后返回给前端
    @TableField(exist = false)
    private Double recommendScore;
}

