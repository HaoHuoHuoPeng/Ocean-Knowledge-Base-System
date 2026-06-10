// 文件说明：这个实体类对应文档版本记录表，用来保存文档每次修改前的历史内容。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 文档版本实体类
// 管理员改文档前，系统会先把旧标题、旧正文保存到这里，后续可以查看或回滚
@Data
@TableName("doc_version")
public class DocVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    // 版本主键 id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 对应的文档 id
    @TableField("doc_id")
    private Long docId;

    // 版本号，从 1 开始递增
    @TableField("version_no")
    private Integer versionNo;

    // 修改前的文档标题
    private String name;

    // 修改前的文档状态
    private String status;

    // 修改前的正文内容
    private String content;

    // 创建这个版本的用户 id
    @TableField("create_user_id")
    private Long createUserId;

    // 版本创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
}

