<script setup lang="ts">
// 文件说明：这个页面负责小游戏入口和钢琴块玩法，支持上传黑块图片和查看排行榜。
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  DeleteOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp, PianoLeaderboardItem } from '@/types'
import { getLoginUser } from '@/utils/auth'
import Game2048 from '@/views/games/Game2048.vue'
import LinkMatchGame from '@/views/games/LinkMatchGame.vue'
import Match3Game from '@/views/games/Match3Game.vue'
import PuzzleGame from '@/views/games/PuzzleGame.vue'

interface TileRow {
  id: number
  targetLane: number
}

interface GameCard {
  key: GameKey
  title: string
  description: string
  tag: string
}

type GameKey = 'menu' | 'piano' | 'link' | '2048' | 'match3' | 'puzzle'
type GameStatus = 'ready' | 'countdown' | 'running' | 'over'
type FeedbackType = 'correct' | 'wrong'

const laneKeys = ['A', 'S', 'J', 'K']
const rowCount = 6
const totalSeconds = 60
const gameCards: GameCard[] = [
  {
    key: 'piano',
    title: '简易钢琴块',
    description: '按A,S,J,K进行控制，鼠标也可点击，1分钟内尽可能多的按对黑块，按错即结束',
    tag: '锻炼反应力',
  },
  {
    key: 'link',
    title: '连连看',
    description: '找出相同物种图片，路线转弯不超过两次即可消除，在最短的时间内消除所有的图片方块吧',
    tag: '休闲娱乐',
  },
  {
    key: '2048',
    title: '2048',
    description: '合并相同数字，尽量冲到更高分，当上下左右操作均无效时游戏结束',
    tag: '提高智力',
  },
  {
    key: 'match3',
    title: '消消乐',
    description: '交换相邻图片，形成三连及以上后消除并冲获得对应分数，在指定时间或指定步数内拿到尽可能多的分数',
    tag: '休闲娱乐',
  },
  {
    key: 'puzzle',
    title: '拼图游戏',
    description: '移动拼块还原海洋知识库图片，在尽可能少的步数和尽可能短的时间内完成。按H可查看原图片',
    tag: '休闲娱乐',
  },
]

const activeGame = ref<GameKey>('menu')
const rows = ref<TileRow[]>([])
const score = ref(0)
const totalPressCount = ref(0)
const wrongCount = ref(0)
const leftSeconds = ref(totalSeconds)
const status = ref<GameStatus>('ready')
const countdown = ref(3)
const combo = ref(0)
const maxCombo = ref(0)
const endReason = ref('')
const laneFeedback = ref<{ lane: number; type: FeedbackType }>()
const tileImage = ref('')
const fileInput = ref<HTMLInputElement>()
const bestScore = ref(0)
const leaderboard = ref<PianoLeaderboardItem[]>([])

let rowId = 0
let timerId = 0
let countdownTimerId = 0
let feedbackTimerId = 0

const currentRow = computed(() => rows.value[rows.value.length - 1])

const statusText = computed(() => {
  if (status.value === 'countdown') {
    return '准备中'
  }
  if (status.value === 'running') {
    return '进行中'
  }
  if (status.value === 'over') {
    return '已结束'
  }
  return '准备开始'
})

const accuracy = computed(() => {
  if (totalPressCount.value === 0) {
    return 100
  }
  return Math.round((score.value / totalPressCount.value) * 100)
})

const tileSkinStyle = computed(() => ({
  backgroundImage: tileImage.value ? `url("${tileImage.value}")` : '',
}))

const currentGameTitle = computed(() => {
  return gameCards.find((game) => game.key === activeGame.value)?.title || '小游戏'
})

const openGame = (gameKey: GameKey) => {
  activeGame.value = gameKey
  if (gameKey === 'piano') {
    void loadLeaderboard()
  }
}

const backToGameMenu = () => {
  clearTimer()
  resetGame()
  activeGame.value = 'menu'
}

const loadLeaderboard = async () => {
  if (!getLoginUser()) {
    leaderboard.value = []
    bestScore.value = 0
    return
  }

  const leaderboardResp = await http.get<CommonResp<PianoLeaderboardItem[]>>('/game/piano/leaderboard')
  leaderboard.value = leaderboardResp.data.content || []

  const myBestResp = await http.get<CommonResp<PianoLeaderboardItem | null>>('/game/piano/myBest')
  bestScore.value = myBestResp.data.content?.score || 0
}

const createRow = (): TileRow => {
  return {
    id: rowId++,
    targetLane: Math.floor(Math.random() * laneKeys.length),
  }
}

const resetRows = () => {
  rows.value = Array.from({ length: rowCount }, () => createRow())
}

const clearTimer = () => {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = 0
  }
  if (countdownTimerId) {
    window.clearInterval(countdownTimerId)
    countdownTimerId = 0
  }
}

const showLaneFeedback = (lane: number, type: FeedbackType) => {
  laneFeedback.value = { lane, type }
  if (feedbackTimerId) {
    window.clearTimeout(feedbackTimerId)
  }
  feedbackTimerId = window.setTimeout(() => {
    laneFeedback.value = undefined
  }, 180)
}

const saveResult = async (reason: string) => {
  if (!getLoginUser()) {
    message.warning('登录后才能保存排行榜成绩')
    return
  }

  const result = {
    // 本局正确按键次数，钢琴块排行榜按这个作为主要分数
    score: score.value,
    // 本局总按键次数
    totalPressCount: totalPressCount.value,
    // 本局错误次数，钢琴块按错会直接结束
    wrongCount: wrongCount.value,
    // 本局准确率
    accuracy: accuracy.value,
    // 本局最大连击数
    maxCombo: maxCombo.value,
    // 本局结束原因，比如时间到、按错
    reason,
  }

  const resp = await http.post<CommonResp<null>>('/game/piano/submit', result)
  if (resp.data.success) {
    await loadLeaderboard()
    message.success('成绩已提交到排行榜')
  }
}

const finishGame = (reason = '时间到') => {
  clearTimer()
  status.value = 'over'
  endReason.value = reason
  void saveResult(reason)
  if (score.value > bestScore.value) {
    bestScore.value = score.value
  }
}

const startRunningTimer = () => {
  timerId = window.setInterval(() => {
    leftSeconds.value -= 1
    if (leftSeconds.value <= 0) {
      leftSeconds.value = 0
      finishGame('时间到')
    }
  }, 1000)
}

const startGame = () => {
  clearTimer()
  rowId = 0
  score.value = 0
  totalPressCount.value = 0
  wrongCount.value = 0
  combo.value = 0
  maxCombo.value = 0
  leftSeconds.value = totalSeconds
  countdown.value = 3
  endReason.value = ''
  laneFeedback.value = undefined
  status.value = 'countdown'
  resetRows()

  countdownTimerId = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimerId)
      countdownTimerId = 0
      status.value = 'running'
      startRunningTimer()
    }
  }, 1000)
}

const resetGame = () => {
  clearTimer()
  rowId = 0
  score.value = 0
  totalPressCount.value = 0
  wrongCount.value = 0
  combo.value = 0
  maxCombo.value = 0
  leftSeconds.value = totalSeconds
  countdown.value = 3
  endReason.value = ''
  laneFeedback.value = undefined
  status.value = 'ready'
  resetRows()
}

const advanceOneRow = () => {
  rows.value = [createRow(), ...rows.value.slice(0, rowCount - 1)]
}

const pressLane = (laneIndex: number) => {
  if (status.value !== 'running' || !currentRow.value) {
    return
  }

  totalPressCount.value += 1

  if (laneIndex === currentRow.value.targetLane) {
    showLaneFeedback(laneIndex, 'correct')
    score.value += 1
    combo.value += 1
    maxCombo.value = Math.max(maxCombo.value, combo.value)
    advanceOneRow()
    return
  }

  showLaneFeedback(laneIndex, 'wrong')
  wrongCount.value += 1
  combo.value = 0
  finishGame('按错了')
}

const handleKeydown = (event: KeyboardEvent) => {
  const key = event.key.toUpperCase()
  const laneIndex = laneKeys.indexOf(key)
  if (laneIndex === -1) {
    return
  }

  event.preventDefault()
  pressLane(laneIndex)
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleTileImageUpload = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    input.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    tileImage.value = String(reader.result || '')
    message.success('黑块图片已替换')
  }
  reader.readAsDataURL(file)
  input.value = ''
}

const clearTileImage = () => {
  tileImage.value = ''
  message.success('已恢复默认黑块')
}

onMounted(() => {
  resetRows()
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  clearTimer()
  if (feedbackTimerId) {
    window.clearTimeout(feedbackTimerId)
  }
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="page piano-page">
    <div v-if="activeGame === 'menu'" class="game-menu">
      <div class="toolbar">
        <div>
          <a-typography-title :level="3">小游戏</a-typography-title>
          <div class="muted">点击下方小游戏即可游玩</div>
        </div>
      </div>

      <div class="game-choice-grid">
        <button
          v-for="game in gameCards"
          :key="game.key"
          class="game-choice-card"
          type="button"
          @click="openGame(game.key)"
        >
          <span class="game-choice-tag">{{ game.tag }}</span>
          <strong>{{ game.title }}</strong>
          <span>{{ game.description }}</span>
        </button>
      </div>
    </div>

    <template v-else>
      <div class="game-detail-nav">
        <a-button @click="backToGameMenu">返回小游戏选择</a-button>
        <strong>{{ currentGameTitle }}</strong>
      </div>

      <template v-if="activeGame === 'piano'">
        <div class="toolbar">
      <div>
        <a-typography-title :level="3">小游戏：简易钢琴块</a-typography-title>
        <div class="muted">使用键盘 A、S、J、K 控制四条道，1 分钟内尽可能多按正确，按错将会终止游戏。</div>
      </div>

      <div class="toolbar-left">
        <a-button type="primary" @click="startGame">
          <PlayCircleOutlined />
          {{ status === 'running' || status === 'countdown' ? '重新开始' : '开始游戏' }}
        </a-button>
        <a-button @click="resetGame">
          <ReloadOutlined />
          重置
        </a-button>
        <a-button @click="triggerUpload">
          <UploadOutlined />
          上传黑块图片
        </a-button>
        <a-button :disabled="!tileImage" @click="clearTileImage">
          <DeleteOutlined />
          还原黑块
        </a-button>
        <input
          ref="fileInput"
          class="hidden-file"
          type="file"
          accept="image/*"
          @change="handleTileImageUpload"
        />
      </div>
    </div>

        <a-alert
      class="game-guide"
      type="info"
      show-icon
      message="玩法说明"
      description="底部高亮行是当前要按的目标层。黑块在哪条道，就按对应键：A、S、J、K。按对后立刻推进下一层；按错会直接结束。倒计时 60 秒结束后，看正确次数。"
    />

        <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <div class="panel game-panel">
          <div class="game-board">
            <div
              v-for="(row, rowIndex) in rows"
              :key="row.id"
              class="tile-row"
              :class="{ 'target-row': rowIndex === rows.length - 1 }"
            >
              <button
                v-for="(_keyName, laneIndex) in laneKeys"
                :key="laneIndex"
                class="lane-cell"
                :class="{
                  'feedback-correct': laneFeedback?.lane === laneIndex && laneFeedback.type === 'correct',
                  'feedback-wrong': laneFeedback?.lane === laneIndex && laneFeedback.type === 'wrong',
                }"
                type="button"
                :aria-label="`第 ${laneIndex + 1} 列，按 ${laneKeys[laneIndex]}`"
                @click="pressLane(laneIndex)"
              >
                <span
                  v-if="row.targetLane === laneIndex"
                  class="piano-tile"
                  :class="{ 'custom-tile': tileImage }"
                  :style="tileSkinStyle"
                />
              </button>
            </div>

            <div v-if="status !== 'running'" class="game-mask">
              <div class="mask-title">{{ statusText }}</div>
              <div v-if="status === 'countdown'" class="countdown-number">{{ countdown }}</div>
              <div v-if="endReason" class="mask-reason">{{ endReason }}</div>
              <div v-if="status === 'over'" class="result-grid">
                <div>
                  <strong>{{ score }}</strong>
                  <span>正确次数</span>
                </div>
                <div>
                  <strong>{{ accuracy }}%</strong>
                  <span>正确率</span>
                </div>
                <div>
                  <strong>{{ maxCombo }}</strong>
                  <span>最高 Combo</span>
                </div>
                <div>
                  <strong>{{ bestScore }}</strong>
                  <span>历史最高</span>
                </div>
              </div>
              <div v-else class="mask-score">正确次数：{{ score }}，最高纪录：{{ bestScore }}</div>
              <a-button v-if="status !== 'countdown'" type="primary" @click="startGame">
                <PlayCircleOutlined />
                {{ status === 'over' ? '再来一局' : '开始游戏' }}
              </a-button>
            </div>
          </div>

          <div class="lane-labels">
            <div v-for="keyName in laneKeys" :key="keyName" class="lane-label">
              按 {{ keyName }}
            </div>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :lg="8">
        <div class="panel side-panel">
          <a-row :gutter="[12, 12]">
            <a-col :span="12">
              <a-statistic title="剩余时间" :value="leftSeconds" suffix="秒" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="正确次数" :value="score" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="按键次数" :value="totalPressCount" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="错误次数" :value="wrongCount" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="正确率" :value="accuracy" suffix="%" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="最高纪录" :value="bestScore" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="当前 Combo" :value="combo" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="最高 Combo" :value="maxCombo" />
            </a-col>
          </a-row>

          <a-divider />

          <a-typography-title :level="4">用户排行榜</a-typography-title>
          <div v-if="leaderboard.length" class="leaderboard">
            <div v-for="item in leaderboard" :key="item.userId" class="leaderboard-row">
              <span class="rank">#{{ item.rank }}</span>
              <span class="score">{{ item.userName }}：{{ item.score }} 次</span>
              <span class="combo">Combo {{ item.maxCombo }}</span>
            </div>
          </div>
          <a-empty v-else description="暂无成绩" />

          <a-divider />

          <a-typography-title :level="4">黑块预览</a-typography-title>
          <div
            class="tile-preview"
            :class="{ 'custom-tile': tileImage }"
            :style="tileSkinStyle"
          >
            <span v-if="!tileImage">默认黑块</span>
          </div>

          <a-divider />

          <a-typography-title :level="4">规则</a-typography-title>
          <ul class="rule-list">
            <li>四条道从左到右分别对应 A、S、J、K。</li>
            <li>只看底部高亮行，黑块在哪条道就按哪个键。</li>
            <li>按对后画面立刻推进一层，由你自己控制速度。</li>
            <li>按错会直接结束，60 秒内正确次数就是本局成绩。</li>
          </ul>
        </div>
      </a-col>
    </a-row>
      </template>

      <div v-else class="panel selected-game-panel">
        <LinkMatchGame v-if="activeGame === 'link'" />
        <Game2048 v-else-if="activeGame === '2048'" />
        <Match3Game v-else-if="activeGame === 'match3'" />
        <PuzzleGame v-else-if="activeGame === 'puzzle'" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.piano-page {
  max-width: 1320px;
  margin: 0 auto;
}

.hidden-file {
  display: none;
}

.game-guide {
  margin-bottom: 16px;
}

.game-panel {
  padding: 14px;
}

.game-menu {
  display: grid;
  gap: 16px;
}

.game-choice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 14px;
}

.game-choice-card {
  display: grid;
  min-height: 160px;
  gap: 10px;
  align-content: start;
  padding: 18px;
  cursor: pointer;
  text-align: left;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.game-choice-card:hover {
  border-color: #1677ff;
  box-shadow: 0 8px 22px rgb(15 23 42 / 10%);
  transform: translateY(-2px);
}

.game-choice-card strong {
  color: #0f172a;
  font-size: 20px;
}

.game-choice-card span:last-child {
  color: #475569;
  line-height: 1.7;
}

.game-choice-tag {
  width: fit-content;
  padding: 3px 8px;
  color: #0958d9;
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-radius: 6px;
  font-size: 12px;
}

.game-detail-nav {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.game-detail-nav strong {
  color: #0f172a;
  font-size: 18px;
}

.selected-game-panel {
  padding: 16px;
}

.lane-labels {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  width: 100%;
  max-width: 720px;
  margin: 8px auto 0;
  overflow: hidden;
  border: 1px solid #cfd6e3;
  border-radius: 8px;
  background: #fff;
}

.lane-label {
  padding: 10px 0;
  color: #0f172a;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  border-right: 1px solid #e2e8f0;
}

.lane-label:last-child {
  border-right: 0;
}

.game-board {
  position: relative;
  overflow: hidden;
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  background: #f8fafc;
  border: 1px solid #cfd6e3;
  border-radius: 8px;
}

.tile-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  height: 78px;
  border-bottom: 1px solid #dbe3ef;
}

.tile-row:last-child {
  border-bottom: 0;
}

.target-row {
  background: #eff6ff;
  box-shadow: inset 0 0 0 2px #1677ff;
}

.lane-cell {
  position: relative;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-right: 1px solid #e2e8f0;
}

.lane-cell:last-child {
  border-right: 0;
}

.lane-cell:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: -2px;
}

.feedback-correct {
  background: #dcfce7;
  box-shadow: inset 0 0 0 3px #22c55e;
}

.feedback-wrong {
  background: #fee2e2;
  box-shadow: inset 0 0 0 3px #ef4444;
}

.piano-tile {
  display: block;
  width: 100%;
  height: 100%;
  background-color: #09090b;
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  border: 1px solid #020617;
  box-shadow: inset 0 -8px 16px rgb(255 255 255 / 8%);
}

.custom-tile {
  background-color: #111827;
}

.game-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  background: rgb(248 250 252 / 88%);
  text-align: center;
}

.mask-title {
  color: #111827;
  font-size: 26px;
  font-weight: 700;
}

.countdown-number {
  color: #1677ff;
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
}

.mask-score {
  color: #475569;
  font-size: 16px;
}

.mask-reason {
  color: #dc2626;
  font-size: 18px;
  font-weight: 700;
}

.result-grid {
  display: grid;
  width: min(420px, 90%);
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.result-grid div {
  padding: 10px 6px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.result-grid strong,
.result-grid span {
  display: block;
}

.result-grid strong {
  color: #111827;
  font-size: 20px;
}

.result-grid span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.side-panel {
  min-height: 100%;
}

.leaderboard {
  display: grid;
  gap: 8px;
}

.leaderboard-row {
  display: grid;
  grid-template-columns: 44px 1fr 90px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.rank {
  color: #1677ff;
  font-weight: 700;
}

.score {
  color: #111827;
  font-weight: 700;
}

.combo {
  color: #64748b;
  text-align: right;
  font-size: 12px;
}

.tile-preview {
  display: flex;
  width: 120px;
  height: 120px;
  align-items: center;
  justify-content: center;
  color: #fff;
  background-color: #09090b;
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  border-radius: 8px;
  font-size: 13px;
}

.rule-list {
  padding-left: 20px;
  margin: 0;
  line-height: 1.9;
  color: #475569;
}

@media (max-width: 760px) {
  .tile-row {
    height: 64px;
  }

  .result-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .tile-preview {
    width: 100%;
  }
}
</style>
