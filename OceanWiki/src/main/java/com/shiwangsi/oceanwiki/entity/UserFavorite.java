// 文件说明：这个实体类对应收藏相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户收藏实体类，对应 user_favorite 表
// 用户可以收藏电子书，也可以收藏具体文档
@Data
@TableName("user_favorite")
public class UserFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    // 收藏主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 收藏用户 id
    @TableField("user_id")
    private Long userId;

    // 收藏类型，ebook 表示电子书，doc 表示文档
    @TableField("target_type")
    private String targetType;

    // 收藏目标 id
    @TableField("target_id")
    private Long targetId;

    // 收藏时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 收藏分组名称，例如“课堂重点”“鲸类资料”
    @TableField("folder_name")
    private String folderName;

    // 收藏目标名称，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String targetName;

    // 收藏目标所属电子书 id，只给前端跳转阅读页使用，不是表字段
    @TableField(exist = false)
    private Long ebookId;
}

