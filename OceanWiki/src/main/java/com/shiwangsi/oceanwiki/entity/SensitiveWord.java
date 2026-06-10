// 文件说明：这个实体类对应敏感词相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 敏感词实体类，对应 sensitive_word 表
// 管理员可以在后台维护这些词，评论发布时会用启用中的敏感词做检查
@Data
@TableName("sensitive_word")
public class SensitiveWord implements Serializable {

    private static final long serialVersionUID = 1L;

    // 敏感词主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 具体敏感词
    private String word;

    // 是否启用：1 启用，0 停用
    private Integer enabled;

    // 备注说明，方便管理员知道为什么加这个词
    private String remark;

    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
}

