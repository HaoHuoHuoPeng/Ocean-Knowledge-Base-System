// 文件说明：这个实体类对应正文内容相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 文档内容实体类，对应数据库中的 content 表
// content.id 和 doc.id 保持一致，这样可以通过文档 id 直接找到正文
@Data
@TableName("content")
public class Content implements Serializable {

    private static final long serialVersionUID = 1L;

    // 内容 id，不自增，保存时直接使用 doc.id
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    // 文档正文，可以保存普通文本，也可以保存 HTML
    private String content;
}

