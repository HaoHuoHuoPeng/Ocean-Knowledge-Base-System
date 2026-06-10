// 文件说明：这个返回对象类用来封装PianoLeaderboardResp接口返回给前端的数据。
package com.shiwangsi.oceanwiki.resp;

import lombok.Data;

import java.time.LocalDateTime;

// 钢琴块排行榜返回对象
// 前端只需要展示这些字段，不需要知道 game_score 表里的所有细节
@Data
public class PianoLeaderboardResp {

    // 排名，从 1 开始
    private Integer rank;

    // 用户 id
    private Long userId;

    // 登录名，方便区分同名用户
    private String loginName;

    // 用户昵称或真实姓名
    private String userName;

    // 正确次数
    private Integer score;

    // 总按键次数
    private Integer totalPressCount;

    // 错误次数
    private Integer wrongCount;

    // 正确率
    private Integer accuracy;

    // 最高连击
    private Integer maxCombo;

    // 结束原因
    private String reason;

    // 成绩创建时间
    private LocalDateTime createTime;
}
