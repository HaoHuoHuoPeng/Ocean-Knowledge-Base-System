// 文件说明：这个 Controller 负责小游戏成绩接口，前端请求会先进入这里再调用业务层处理。
package com.shiwangsi.oceanwiki.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiwangsi.oceanwiki.config.auth.AuthTokenStore;
import com.shiwangsi.oceanwiki.entity.GameScore;
import com.shiwangsi.oceanwiki.entity.User;
import com.shiwangsi.oceanwiki.resp.CommonResp;
import com.shiwangsi.oceanwiki.resp.GameLeaderboardResp;
import com.shiwangsi.oceanwiki.service.IGameScoreService;
import com.shiwangsi.oceanwiki.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// 小游戏成绩接口
// 钢琴块、连连看、2048、消消乐都可以把成绩保存到 game_score 表
@Tag(name = "小游戏成绩接口")
@RestController
@RequestMapping("/game")
public class PianoGameController {

    private static final String PIANO_GAME_CODE = "piano_tiles";
    private static final Set<String> LINK_MATCH_GAME_CODES = Set.of(
            "link_match_easy",
            "link_match_normal",
            "link_match_hard"
    );
    private static final Set<String> ALLOW_GAME_CODES = Set.of(
            PIANO_GAME_CODE,
            "link_match_easy",
            "link_match_normal",
            "link_match_hard",
            "game_2048",
            "match3_time_easy",
            "match3_time_normal",
            "match3_time_hard",
            "match3_step_easy",
            "match3_step_normal",
            "match3_step_hard",
            "puzzle_easy",
            "puzzle_normal"
    );
    private static final int LEADERBOARD_LIMIT = 10;

    private final IGameScoreService gameScoreService;
    private final IUserService userService;

    public PianoGameController(IGameScoreService gameScoreService, IUserService userService) {
        this.gameScoreService = gameScoreService;
        this.userService = userService;
    }

    // 保存当前登录用户的一局钢琴块成绩
    // 这个旧地址继续保留，避免前端钢琴块已有请求失效
    @Operation(summary = "提交钢琴块成绩")
    @PostMapping("/piano/submit")
    public CommonResp<Object> submit(@RequestBody GameScore gameScore, HttpServletRequest request) {
        return saveGameScore(gameScore, request, PIANO_GAME_CODE, true);
    }

    // 保存任意小游戏成绩
    // 前端通过 gameCode 区分是哪一个小游戏，排行榜也会按 gameCode 分开查询
    @Operation(summary = "提交小游戏成绩")
    @PostMapping("/score/submit")
    public CommonResp<Object> submitGameScore(@RequestBody GameScore gameScore, HttpServletRequest request) {
        if (!StringUtils.hasText(gameScore.getGameCode())) {
            return CommonResp.fail("缺少游戏编码");
        }
        if (!ALLOW_GAME_CODES.contains(gameScore.getGameCode())) {
            return CommonResp.fail("不支持的游戏编码");
        }
        return saveGameScore(gameScore, request, gameScore.getGameCode(), false);
    }

    // 查询钢琴块排行榜
    // 每个用户只展示自己成绩最好的一局，避免同一个用户刷屏
    @Operation(summary = "查询钢琴块排行榜")
    @GetMapping("/piano/leaderboard")
    public CommonResp<List<GameLeaderboardResp>> leaderboard() {
        return leaderboardByGameCode(PIANO_GAME_CODE);
    }

    // 查询任意小游戏排行榜
    @Operation(summary = "查询小游戏排行榜")
    @GetMapping("/score/leaderboard")
    public CommonResp<List<GameLeaderboardResp>> gameLeaderboard(@RequestParam String gameCode) {
        if (!ALLOW_GAME_CODES.contains(gameCode)) {
            return CommonResp.fail("不支持的游戏编码");
        }
        return leaderboardByGameCode(gameCode);
    }

    // 查询当前登录用户的个人最高钢琴块成绩
    @Operation(summary = "查询我的钢琴块最高成绩")
    @GetMapping("/piano/myBest")
    public CommonResp<GameLeaderboardResp> myBest(HttpServletRequest request) {
        return myBestByGameCode(PIANO_GAME_CODE, request);
    }

    // 查询当前登录用户在指定小游戏里的最好成绩
    @Operation(summary = "查询我的小游戏最好成绩")
    @GetMapping("/score/myBest")
    public CommonResp<GameLeaderboardResp> gameMyBest(@RequestParam String gameCode, HttpServletRequest request) {
        if (!ALLOW_GAME_CODES.contains(gameCode)) {
            return CommonResp.fail("不支持的游戏编码");
        }
        return myBestByGameCode(gameCode, request);
    }

    private CommonResp<Object> saveGameScore(
            GameScore gameScore,
            HttpServletRequest request,
            String gameCode,
            boolean calcPianoAccuracy
    ) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return CommonResp.fail("请先登录后再提交成绩");
        }

        int score = safeNumber(gameScore.getScore());
        int totalPressCount = safeNumber(gameScore.getTotalPressCount());

        gameScore.setId(null);
        gameScore.setUserId(userId);
        gameScore.setGameCode(gameCode);
        gameScore.setScore(score);
        gameScore.setTotalPressCount(totalPressCount);
        gameScore.setWrongCount(safeNumber(gameScore.getWrongCount()));
        gameScore.setAccuracy(calcPianoAccuracy ? calcAccuracy(score, totalPressCount) : safeAccuracy(gameScore.getAccuracy()));
        gameScore.setMaxCombo(safeNumber(gameScore.getMaxCombo()));
        gameScore.setReason(StringUtils.hasText(gameScore.getReason()) ? gameScore.getReason() : "未知");
        gameScore.setCreateTime(LocalDateTime.now());
        gameScoreService.save(gameScore);

        return CommonResp.ok("成绩已保存", null);
    }

    private CommonResp<List<GameLeaderboardResp>> leaderboardByGameCode(String gameCode) {
        List<GameScore> scores = gameScoreService.list(bestScoreWrapper(gameCode));
        List<GameLeaderboardResp> leaderboard = buildLeaderboard(scores, LEADERBOARD_LIMIT);
        return CommonResp.ok(leaderboard);
    }

    private CommonResp<GameLeaderboardResp> myBestByGameCode(String gameCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return CommonResp.fail("请先登录");
        }

        List<GameScore> scores = gameScoreService.list(bestScoreWrapper(gameCode).eq("user_id", userId));
        List<GameLeaderboardResp> leaderboard = buildLeaderboard(scores, 1);
        return CommonResp.ok(leaderboard.isEmpty() ? null : leaderboard.get(0));
    }

    private QueryWrapper<GameScore> bestScoreWrapper(String gameCode) {
        QueryWrapper<GameScore> wrapper = new QueryWrapper<>();
        wrapper.eq("game_code", gameCode);

        // 连连看用 score 保存通关秒数，秒数越少排名越高
        if (LINK_MATCH_GAME_CODES.contains(gameCode)) {
            wrapper.gt("score", 0)
                    .orderByAsc("score")
                    .orderByAsc("create_time")
                    .last("LIMIT 300");
            return wrapper;
        }

        // 其他小游戏都按分数越高排名越高
        wrapper.orderByDesc("score")
                .orderByDesc("accuracy")
                .orderByDesc("max_combo")
                .orderByAsc("create_time")
                .last("LIMIT 300");
        return wrapper;
    }

    private List<GameLeaderboardResp> buildLeaderboard(List<GameScore> scores, int limit) {
        if (scores.isEmpty()) {
            return List.of();
        }

        Map<Long, GameScore> bestScoreMap = new LinkedHashMap<>();
        for (GameScore score : scores) {
            bestScoreMap.putIfAbsent(score.getUserId(), score);
            if (bestScoreMap.size() >= limit) {
                break;
            }
        }

        Map<Long, User> userMap = userService.listByIds(bestScoreMap.keySet())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<GameLeaderboardResp> result = new ArrayList<>();
        int rank = 1;
        for (GameScore score : bestScoreMap.values()) {
            User user = userMap.get(score.getUserId());
            GameLeaderboardResp item = new GameLeaderboardResp();
            item.setRank(rank++);
            item.setUserId(score.getUserId());
            item.setLoginName(user == null ? "" : user.getLoginName());
            item.setUserName(user == null ? "用户" + score.getUserId() : user.getName());
            item.setScore(score.getScore());
            item.setTotalPressCount(score.getTotalPressCount());
            item.setWrongCount(score.getWrongCount());
            item.setAccuracy(score.getAccuracy());
            item.setMaxCombo(score.getMaxCombo());
            item.setReason(score.getReason());
            item.setCreateTime(score.getCreateTime());
            result.add(item);
        }
        return result;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        AuthTokenStore.LoginUserInfo loginUserInfo = AuthTokenStore.get(token);
        return loginUserInfo == null ? null : loginUserInfo.userId();
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private int safeAccuracy(Integer value) {
        if (value == null) {
            return 100;
        }
        return Math.max(0, Math.min(value, 100));
    }

    private int calcAccuracy(int score, int totalPressCount) {
        if (totalPressCount <= 0) {
            return 100;
        }
        return Math.round(score * 100F / totalPressCount);
    }
}
