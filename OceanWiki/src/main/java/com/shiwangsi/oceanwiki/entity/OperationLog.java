// 文件说明：这个实体类对应操作日志相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 操作日志实体类，对应 operation_log 表
// 后台关键操作可以记录到这里，方便后续追踪是谁操作的
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    // 日志主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 操作用户 id
    @TableField("user_id")
    private Long userId;

    // 操作模块
    private String module;

    // 操作动作
    private String action;

    // 操作内容
    private String content;

    // 操作前的关键数据，用来追踪管理员到底改了什么
    @TableField("before_data")
    private String beforeData;

    // 操作后的关键数据，用来和 beforeData 对比
    @TableField("after_data")
    private String afterData;

    // 操作时间
    @TableField("create_time")
    private LocalDateTime createTime;
}

