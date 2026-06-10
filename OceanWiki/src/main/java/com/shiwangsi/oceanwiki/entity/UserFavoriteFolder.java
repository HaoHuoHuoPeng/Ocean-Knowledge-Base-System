// 文件说明：这个实体类对应用户收藏分组表，用来保存每个用户自己创建的收藏分组。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 用户收藏分组实体类
// 分组单独成表后，即使分组里暂时没有收藏内容，也能在页面下拉框里显示出来
@Data
@TableName("user_favorite_folder")
public class UserFavoriteFolder implements Serializable {

    private static final long serialVersionUID = 1L;

    // 分组主键 id，使用雪花算法生成
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 分组所属用户 id
    @TableField("user_id")
    private Long userId;

    // 分组名称，例如“默认分组”“虎鲸资料”“课堂重点”
    @TableField("name")
    private String name;

    // 分组创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
}
