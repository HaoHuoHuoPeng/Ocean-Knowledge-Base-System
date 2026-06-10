// 文件说明：这个文件负责封装小游戏成绩接口，组件只管调用方法，不直接拼接口地址。
import http from '@/api/http'
import type { CommonResp, GameLeaderboardItem } from '@/types'

// 小游戏编码
// 后端会按这个字段区分不同小游戏的排行榜
export type GameCode =
  | 'piano_tiles'
  | 'link_match_easy'
  | 'link_match_normal'
  | 'link_match_hard'
  | 'game_2048'
  | 'match3_time_easy'
  | 'match3_time_normal'
  | 'match3_time_hard'
  | 'match3_step_easy'
  | 'match3_step_normal'
  | 'match3_step_hard'
  | 'puzzle_easy'
  | 'puzzle_normal'

// 提交成绩时需要传给后端的数据
export interface GameScoreSubmit {
  // 小游戏编码，后端用它区分是哪一个游戏、哪一个难度、哪一种模式
  gameCode: GameCode
  // 本局得分，排行榜主要按这个字段排序
  score: number
  // 本局总操作次数，比如钢琴块按键次数、拼图移动次数、消消乐步数
  totalPressCount?: number
  // 本局错误次数，比如钢琴块按错次数
  wrongCount?: number
  // 准确率，通常是正确次数除以总次数后换算成百分比
  accuracy?: number
  // 最大连击数，适合钢琴块和消消乐这类连续操作游戏
  maxCombo?: number
  // 本局结束原因，比如完成、超时、按错、主动结束
  reason?: string
}

// 提交一局小游戏成绩
export const submitGameScore = (data: GameScoreSubmit) => {
  return http.post<CommonResp<null>>('/game/score/submit', data)
}

// 查询某个小游戏的排行榜
export const queryGameLeaderboard = (gameCode: GameCode) => {
  return http.get<CommonResp<GameLeaderboardItem[]>>('/game/score/leaderboard', {
    // gameCode 通过地址参数传给后端，用来查询指定游戏排行榜
    params: { gameCode },
  })
}

// 查询当前登录用户在某个小游戏里的最好成绩
export const queryMyGameBest = (gameCode: GameCode) => {
  return http.get<CommonResp<GameLeaderboardItem | null>>('/game/score/myBest', {
    // gameCode 通过地址参数传给后端，用来查询当前用户在指定游戏里的最好成绩
    params: { gameCode },
  })
}
