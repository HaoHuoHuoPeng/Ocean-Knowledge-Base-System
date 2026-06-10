// 文件说明：这个返回对象类用来封装小游戏排行榜接口返回给前端的数据。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

import java.time.LocalDateTime;

// 小游戏排行榜返回对象
// 前端只需要展示这些字段，不需要知道 game_score 表里的所有细节
@Data
public class GameLeaderboardResp {

    // 排名，从 1 开始
    private Integer rank;

    // 用户 id
    private Long userId;

    // 账号，方便区分同名用户
    private String loginName;

    // 用户昵称或真实姓名
    private String userName;

    // 成绩分数
    // 钢琴块、2048、消消乐表示分数越高越好；连连看这里表示通关用时秒数，越少越好
    private Integer score;

    // 扩展计数
    // 钢琴块表示总按键次数；消消乐表示步数；连连看表示使用图片数量
    private Integer totalPressCount;

    // 错误次数
    private Integer wrongCount;

    // 正确率或完成度
    private Integer accuracy;

    // 最高连击或最大合成数字
    private Integer maxCombo;

    // 成绩备注，例如难度、结束原因
    private String reason;

    // 成绩创建时间
    private LocalDateTime createTime;
}
