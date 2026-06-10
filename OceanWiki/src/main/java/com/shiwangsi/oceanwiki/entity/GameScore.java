// 文件说明：这个实体类对应小游戏成绩相关表数据，用来在后端代码里承载数据库字段。
package com.shiwangsi.oceanwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

// 小游戏成绩实体类，对应数据库中的 game_score 表
// 当前先用于钢琴块排行榜，后面如果 2048、连连看也要排行，可以继续用 gameCode 区分
@Data
@TableName("game_score")
public class GameScore implements Serializable {

    private static final long serialVersionUID = 1L;

    // 成绩主键 id，由 MyBatis-Plus 雪花算法生成
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    // 用户 id，用来把成绩和登录用户关联起来
    @TableField("user_id")
    private Long userId;

    // 游戏编码，例如 piano_tiles 表示钢琴块
    @TableField("game_code")
    private String gameCode;

    // 本局正确次数，排行榜主要按这个字段从高到低排序
    private Integer score;

    // 本局总按键次数
    @TableField("total_press_count")
    private Integer totalPressCount;

    // 本局错误次数
    @TableField("wrong_count")
    private Integer wrongCount;

    // 正确率，前端展示时直接显示百分比
    private Integer accuracy;

    // 最高连击数
    @TableField("max_combo")
    private Integer maxCombo;

    // 结束原因，例如“时间到”或“按错了”
    private String reason;

    // 成绩创建时间
    @TableField("create_time")
    private LocalDateTime createTime;
}

