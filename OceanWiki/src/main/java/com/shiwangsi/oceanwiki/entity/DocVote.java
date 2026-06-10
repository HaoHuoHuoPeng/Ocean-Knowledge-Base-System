// 文件说明：这个实体类对应文档点赞相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 文档点赞记录实体类，对应数据库中的 doc_vote 表
// 一条记录表示某个用户已经给某篇文档点过赞
@Data
@TableName("doc_vote")
public class DocVote implements Serializable {

    private static final long serialVersionUID = 1L;

    // 点赞记录主键 id，由 MyBatis-Plus 雪花算法生成
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 文档 id，对应 doc 表
    @TableField("doc_id")
    private Long docId;

    // 用户 id，对应 user 表
    @TableField("user_id")
    private Long userId;

    // 点赞时间
    @TableField("create_time")
    private LocalDateTime createTime;
}

