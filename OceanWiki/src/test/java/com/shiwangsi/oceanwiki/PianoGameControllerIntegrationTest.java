// 文件说明：这个文件负责钢琴块小游戏对应模块的代码逻辑。
package com.shiwangsi.oceanwiki;

import com.shiwangsi.oceanwiki.controller.PianoGameController;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.GameLeaderboardResp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 钢琴块小游戏测试
// 主要验证排行榜不会被同一个用户的多条成绩刷屏
class PianoGameControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PianoGameController pianoGameController;

    @Test
    void leaderboardShouldUseBestScoreOfEachUser() {
        // 同一个用户提交两条成绩，排行榜应该只显示这个用户最高的一条
        createGameScore(normalUser.getId(), 10, 100, 10);
        createGameScore(normalUser.getId(), 35, 100, 20);

        // 另一个用户也有成绩，用来确认排行榜仍然能展示多个用户
        createGameScore(anotherUser.getId(), 20, 100, 15);

        CommonResp<List<GameLeaderboardResp>> resp = pianoGameController.leaderboard();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContent()).hasSizeGreaterThanOrEqualTo(2);
        GameLeaderboardResp normalUserBest = resp.getContent().stream()
                .filter(item -> item.getUserId().equals(normalUser.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(normalUserBest.getScore()).isEqualTo(35);
        assertThat(resp.getContent().stream()
                .filter(item -> item.getUserId().equals(normalUser.getId()))
                .count()).isEqualTo(1);
    }
}
