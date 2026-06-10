// 文件说明：这个实体类对应反馈相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户反馈实体类，对应 user_feedback 表
// 普通用户可以提交建议或纠错，管理员在后台处理
@Data
@TableName("user_feedback")
public class UserFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    // 反馈主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 提交反馈的用户 id
    @TableField("user_id")
    private Long userId;

    // 反馈目标类型：ebook 表示电子书，doc 表示文档，可以为空
    @TableField("target_type")
    private String targetType;

    // 反馈目标 id，可以为空
    @TableField("target_id")
    private Long targetId;

    // 反馈类型：suggestion 建议，correction 纠错
    private String type;

    // 反馈标题
    private String title;

    // 反馈内容
    private String content;

    // 状态：open 待处理，handled 已处理，rejected 已驳回
    private String status;

    // 提交时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 处理时间
    @TableField("handle_time")
    private LocalDateTime handleTime;

    // 处理人 id
    @TableField("handle_user_id")
    private Long handleUserId;

    // 处理备注
    @TableField("handle_remark")
    private String handleRemark;

    // 提交用户姓名，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String userName;
}

