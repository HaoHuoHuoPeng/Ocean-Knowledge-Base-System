// 文件说明：这个实体类对应通知相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户通知实体类，对应 user_notice 表
// 评论审核、反馈处理等结果可以通过通知告诉用户
@Data
@TableName("user_notice")
public class UserNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    // 通知主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 接收用户 id
    @TableField("user_id")
    private Long userId;

    // 通知标题
    private String title;

    // 通知内容
    private String content;

    // 是否已读：0 未读，1 已读
    @TableField("read_flag")
    private Integer readFlag;

    // 通知创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
}

