// 文件说明：这个实体类对应评论相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户评论实体类，对应 user_comment 表
// 评论发布时会检查敏感词，命中后进入待审核状态
@Data
@TableName("user_comment")
public class UserComment implements Serializable {

    private static final long serialVersionUID = 1L;

    // 评论主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 发布评论的用户 id
    @TableField("user_id")
    private Long userId;

    // 评论目标类型：ebook 表示电子书，doc 表示文档
    @TableField("target_type")
    private String targetType;

    // 评论目标 id
    @TableField("target_id")
    private Long targetId;

    // 父评论 id，0 表示一级评论
    @TableField("parent_id")
    private Long parentId;

    // 评论内容
    private String content;

    // 状态：published 已发布，pending 待审核，rejected 已驳回，deleted 已删除
    private String status;

    // 命中的敏感词，多个词用逗号连接
    @TableField("sensitive_hit")
    private String sensitiveHit;

    // 评论创建时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 审核时间
    @TableField("review_time")
    private LocalDateTime reviewTime;

    // 审核人 id
    @TableField("review_user_id")
    private Long reviewUserId;

    // 审核备注
    @TableField("review_remark")
    private String reviewRemark;

    // 评论用户姓名，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String userName;

    // 评论目标名称，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String targetName;

    // 子评论列表，只给前端展示评论回复使用，不是表字段
    @TableField(exist = false)
    private java.util.List<UserComment> children;
}

