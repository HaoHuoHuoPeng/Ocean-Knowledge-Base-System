// 文件说明：这个实体类对应分类相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 分类实体类，对应数据库中的 category 表
// parent 字段用来表达树形结构：parent=0 表示一级分类，parent=某个 id 表示它的子分类
@Data
@TableName("category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    // 分类主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 父分类 id，0 表示没有父分类
    private Long parent;

    // 分类名称，例如“海洋动物”“鲸豚类”
    private String name;

    // 排序值，数字越小越靠前
    private Integer sort;
}

