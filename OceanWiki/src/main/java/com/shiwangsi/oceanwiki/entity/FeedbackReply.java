// 文件说明：这个实体类对应反馈回复相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 反馈回复实体类，对应 feedback_reply 表
// 管理员可以围绕一条反馈多次回复，用户在自己的反馈里可以看到处理对话
@Data
@TableName("feedback_reply")
public class FeedbackReply implements Serializable {

    private static final long serialVersionUID = 1L;

    // 回复主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 所属反馈 id
    @TableField("feedback_id")
    private Long feedbackId;

    // 回复人 id
    @TableField("user_id")
    private Long userId;

    // 回复人类型：admin 管理员，user 用户
    @TableField("reply_type")
    private String replyType;

    // 回复内容
    private String content;

    // 回复时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 回复人姓名，只给前端展示使用，不是表字段
    @TableField(exist = false)
    private String userName;
}

