<script setup lang="ts">
// 文件说明：这个组件负责消消乐小游戏，处理图片选择、上传图片、计时模式、记步模式、交换消除和排行榜。
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import { queryGameLeaderboard, queryMyGameBest, submitGameScore, type GameCode } from '@/api/gameScore'
import type { GameLeaderboardItem } from '@/types'
import { getLoginUser } from '@/utils/auth'
import {
  createUploadedGameAssets,
  defaultGameAssets,
  findGameAsset,
  type GameAsset,
} from '@/utils/gameAssets'

interface Point {
  row: number
  col: number
}

type DifficultyKey = 'easy' | 'normal' | 'hard'
type PlayMode = 'time' | 'step'

interface DifficultyConfig {
  label: string
  size: number
}

const GAME_CODE_MAP: Record<PlayMode, Record<DifficultyKey, GameCode>> = {
  time: {
    easy: 'match3_time_easy',
    normal: 'match3_time_normal',
    hard: 'match3_time_hard',
  },
  step: {
    easy: 'match3_step_easy',
    normal: 'match3_step_normal',
    hard: 'match3_step_hard',
  },
}
const TIME_MODE_SECONDS = 120
const STEP_MODE_LIMIT = 20
const difficultyOptions: { label: string; value: DifficultyKey }[] = [
  { label: '入门', value: 'easy' },
  { label: '普通', value: 'normal' },
  { label: '困难', value: 'hard' },
]
const modeOptions: { label: string; value: PlayMode }[] = [
  { label: '计时模式', value: 'time' },
  { label: '记步模式', value: 'step' },
]
const difficultyMap: Record<DifficultyKey, DifficultyConfig> = {
  easy: { label: '入门', size: 8 },
  normal: { label: '普通', size: 9 },
  hard: { label: '困难', size: 10 },
}

const difficulty = ref<DifficultyKey>('easy')
const playMode = ref<PlayMode>('time')
const uploadedAssets = ref<GameAsset[]>([])
const selectedAssetIds = ref(defaultGameAssets.map((asset) => asset.id))
const imageCount = ref(5)
const uploadInput = ref<HTMLInputElement>()
const board = ref<string[][]>([])
const selected = ref<Point>()
const score = ref(0)
const moves = ref(0)
const timeLeaderboard = ref<GameLeaderboardItem[]>([])
const stepLeaderboard = ref<GameLeaderboardItem[]>([])
const timeMyBest = ref<GameLeaderboardItem | null>(null)
const stepMyBest = ref<GameLeaderboardItem | null>(null)
const resultSubmitted = ref(false)
const leftSeconds = ref(TIME_MODE_SECONDS)
const gameOver = ref(false)
const gameStarted = ref(false)
const endReason = ref('')

let timerId = 0

const currentDifficulty = computed(() => difficultyMap[difficulty.value])
const size = computed(() => currentDifficulty.value.size)
const allAssets = computed(() => [...defaultGameAssets, ...uploadedAssets.value])
const activeAssets = computed(() => {
  return selectedAssetIds.value
    .map((id) => allAssets.value.find((asset) => asset.id === id))
    .filter(Boolean) as GameAsset[]
})
const boardStyle = computed(() => ({
  gridTemplateColumns: `repeat(${size.value}, 1fr)`,
}))
const leftSteps = computed(() => Math.max(STEP_MODE_LIMIT - moves.value, 0))
const modeText = computed(() => (playMode.value === 'time' ? '计时模式' : '记步模式'))
const currentGameCode = computed(() => GAME_CODE_MAP[playMode.value][difficulty.value])
const currentLeaderboard = computed(() => (playMode.value === 'time' ? timeLeaderboard.value : stepLeaderboard.value))
const currentMyBest = computed(() => (playMode.value === 'time' ? timeMyBest.value : stepMyBest.value))
const anotherLeaderboard = computed(() => (playMode.value === 'time' ? stepLeaderboard.value : timeLeaderboard.value))
const anotherLeaderboardTitle = computed(() => (playMode.value === 'time' ? '记步模式排行榜' : '计时模式排行榜'))
const getGameCode = (mode: PlayMode, difficultyKey: DifficultyKey) => GAME_CODE_MAP[mode][difficultyKey]

// 校验开局图片数量
// 消消乐至少要 3 张图，且用户指定几张就必须刚好勾选几张
const validateAssets = () => {
  if (imageCount.value < 3) {
    message.error('消消乐至少需要指定 3 张图片')
    return false
  }
  if (allAssets.value.length < imageCount.value) {
    message.error(`当前只有 ${allAssets.value.length} 张可用图片，不够选择 ${imageCount.value} 张`)
    return false
  }
  if (selectedAssetIds.value.length !== imageCount.value) {
    message.error(`请刚好勾选 ${imageCount.value} 张图片，当前勾选了 ${selectedAssetIds.value.length} 张`)
    return false
  }
  return true
}

const randomSymbol = () => {
  return activeAssets.value[Math.floor(Math.random() * activeAssets.value.length)].id
}

const loadLeaderboard = async () => {
  if (!getLoginUser()) {
    timeLeaderboard.value = []
    stepLeaderboard.value = []
    timeMyBest.value = null
    stepMyBest.value = null
    return
  }

  const [timeLeaderboardResp, stepLeaderboardResp, timeMyBestResp, stepMyBestResp] = await Promise.all([
    queryGameLeaderboard(getGameCode('time', difficulty.value)),
    queryGameLeaderboard(getGameCode('step', difficulty.value)),
    queryMyGameBest(getGameCode('time', difficulty.value)),
    queryMyGameBest(getGameCode('step', difficulty.value)),
  ])

  timeLeaderboard.value = timeLeaderboardResp.data.content || []
  stepLeaderboard.value = stepLeaderboardResp.data.content || []
  timeMyBest.value = timeMyBestResp.data.content || null
  stepMyBest.value = stepMyBestResp.data.content || null
}

const stopTimer = () => {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = 0
  }
}

const startTimer = () => {
  stopTimer()
  if (playMode.value !== 'time') {
    return
  }

  timerId = window.setInterval(() => {
    leftSeconds.value -= 1
    if (leftSeconds.value <= 0) {
      leftSeconds.value = 0
      finishGame('时间到')
    }
  }, 1000)
}

const submitResult = async () => {
  if (resultSubmitted.value || !getLoginUser()) {
    return
  }

  resultSubmitted.value = true
  const resp = await submitGameScore({
    gameCode: currentGameCode.value,
    score: score.value,
    totalPressCount: moves.value,
    wrongCount: 0,
    accuracy: 100,
    maxCombo: imageCount.value,
    reason: `${modeText.value}，${currentDifficulty.value.label}难度`,
  })

  if (resp.data.success) {
    await loadLeaderboard()
    message.success('消消乐成绩已提交排行榜')
  }
}

const finishGame = (reason: string) => {
  if (gameOver.value) {
    return
  }
  stopTimer()
  gameOver.value = true
  gameStarted.value = false
  endReason.value = reason
  message.success(`消消乐结束：${reason}`)
  void submitResult()
}

const resetGame = () => {
  stopTimer()
  gameStarted.value = false
  selected.value = undefined
  board.value = []
  if (!validateAssets()) {
    return
  }

  score.value = 0
  moves.value = 0
  leftSeconds.value = TIME_MODE_SECONDS
  selected.value = undefined
  resultSubmitted.value = false
  gameOver.value = false
  endReason.value = ''
  board.value = Array.from({ length: size.value }, () =>
    Array.from({ length: size.value }, () => randomSymbol()),
  )
  resolveMatches()
  score.value = 0
}

const startGame = () => {
  resetGame()
  if (!board.value.length) {
    return
  }
  gameStarted.value = true
  startTimer()
}

const isSamePoint = (a?: Point, b?: Point) => {
  return !!a && !!b && a.row === b.row && a.col === b.col
}

const isNeighbor = (a: Point, b: Point) => {
  return Math.abs(a.row - b.row) + Math.abs(a.col - b.col) === 1
}

const swap = (a: Point, b: Point) => {
  const temp = board.value[a.row][a.col]
  board.value[a.row][a.col] = board.value[b.row][b.col]
  board.value[b.row][b.col] = temp
}

const findMatches = () => {
  const matched = new Set<string>()

  for (let row = 0; row < size.value; row += 1) {
    let start = 0
    for (let col = 1; col <= size.value; col += 1) {
      if (col < size.value && board.value[row][col] === board.value[row][start]) {
        continue
      }
      if (col - start >= 3) {
        for (let i = start; i < col; i += 1) {
          matched.add(`${row}-${i}`)
        }
      }
      start = col
    }
  }

  for (let col = 0; col < size.value; col += 1) {
    let start = 0
    for (let row = 1; row <= size.value; row += 1) {
      if (row < size.value && board.value[row][col] === board.value[start][col]) {
        continue
      }
      if (row - start >= 3) {
        for (let i = start; i < row; i += 1) {
          matched.add(`${i}-${col}`)
        }
      }
      start = row
    }
  }

  return matched
}

const fillBoard = (matched: Set<string>) => {
  for (let col = 0; col < size.value; col += 1) {
    const rest: string[] = []
    for (let row = size.value - 1; row >= 0; row -= 1) {
      if (!matched.has(`${row}-${col}`)) {
        rest.push(board.value[row][col])
      }
    }
    for (let row = size.value - 1; row >= 0; row -= 1) {
      board.value[row][col] = rest[size.value - 1 - row] || randomSymbol()
    }
  }
}

const resolveMatches = () => {
  let totalRemoved = 0
  let matched = findMatches()
  while (matched.size) {
    totalRemoved += matched.size
    fillBoard(matched)
    matched = findMatches()
  }
  if (totalRemoved) {
    score.value += totalRemoved
  }
  return totalRemoved
}

const selectCell = (row: number, col: number) => {
  if (!gameStarted.value) {
    message.warning('请先点击开始游戏')
    return
  }
  if (gameOver.value) {
    return
  }

  const current = { row, col }
  if (!selected.value) {
    selected.value = current
    return
  }

  if (isSamePoint(selected.value, current)) {
    selected.value = undefined
    return
  }

  if (!isNeighbor(selected.value, current)) {
    selected.value = current
    return
  }

  const first = selected.value
  swap(first, current)
  const removed = resolveMatches()
  moves.value += 1
  selected.value = undefined

  if (!removed) {
    swap(first, current)
    message.warning('这一步没有形成三连')
    return
  }

  if (playMode.value === 'step' && moves.value >= STEP_MODE_LIMIT) {
    finishGame('步数用完')
  }
}

const getImage = (id: string) => {
  return findGameAsset(allAssets.value, id)?.url || ''
}

const getName = (id: string) => {
  return findGameAsset(allAssets.value, id)?.name || ''
}

const triggerUpload = () => {
  uploadInput.value?.click()
}

const handleUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) {
    return
  }

  const assets = await createUploadedGameAssets(input.files, uploadedAssets.value.length)
  if (!assets.length) {
    message.warning('请选择图片文件')
    input.value = ''
    return
  }

  uploadedAssets.value.push(...assets)
  selectedAssetIds.value = [...selectedAssetIds.value, ...assets.map((asset) => asset.id)].slice(0, imageCount.value)
  message.success(`已添加 ${assets.length} 张图片`)
  input.value = ''
}

watch(difficulty, () => {
  resetGame()
  void loadLeaderboard()
})

watch(playMode, () => {
  resetGame()
})

watch(imageCount, (count) => {
  selectedAssetIds.value = selectedAssetIds.value.slice(0, count)
})

onMounted(() => {
  resetGame()
  void loadLeaderboard()
})

onBeforeUnmount(() => {
  stopTimer()
})
</script>

<template>
  <div class="game-card">
    <div class="game-toolbar">
      <div>
        <a-typography-title :level="4">消消乐</a-typography-title>
        <div class="muted">交换相邻图块，横向或纵向三个以上相同即可消除。计时模式限时 2 分钟，记步模式限 20 步。</div>
      </div>
      <a-space wrap>
        <a-segmented v-model:value="playMode" :options="modeOptions" />
        <span class="difficulty-label">难度</span>
        <a-select v-model:value="difficulty" class="difficulty-select" :options="difficultyOptions" />
        <a-statistic title="得分" :value="score" />
        <a-statistic title="步数" :value="moves" />
        <a-statistic v-if="playMode === 'time'" title="剩余时间" :value="leftSeconds" suffix="秒" />
        <a-statistic v-else title="剩余步数" :value="leftSteps" />
        <a-button type="primary" @click="startGame">开始游戏</a-button>
        <a-button @click="resetGame">重置</a-button>
      </a-space>
    </div>

    <a-alert
      class="game-guide"
      type="info"
      show-icon
      message="玩法说明"
      description="选择计时模式或记步模式后，交换相邻图片。横向或纵向凑出三个及以上相同图片即可消除，分数越高排名越靠前。"
    />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <div class="asset-panel">
          <div class="asset-toolbar">
            <a-space wrap>
              <span>指定图片数量</span>
              <a-input-number v-model:value="imageCount" :min="3" :max="allAssets.length" />
              <a-button @click="triggerUpload">
                <UploadOutlined />
                上传图片
              </a-button>
              <input ref="uploadInput" class="hidden-file" type="file" accept="image/*" multiple @change="handleUpload" />
            </a-space>
            <span class="muted">必须刚好勾选 {{ imageCount }} 张，当前 {{ selectedAssetIds.length }} 张。</span>
          </div>

          <a-checkbox-group v-model:value="selectedAssetIds" class="asset-grid">
            <label v-for="asset in allAssets" :key="asset.id" class="asset-option">
              <a-checkbox :value="asset.id" />
              <img :src="asset.url" :alt="asset.name" />
              <span>{{ asset.name }}</span>
            </label>
          </a-checkbox-group>
        </div>

        <div class="match-board" :style="boardStyle">
          <button
            v-for="(cell, index) in board.flat()"
            :key="index"
            class="match-cell"
            :class="{ selected: isSamePoint(selected, { row: Math.floor(index / size), col: index % size }) }"
            type="button"
            :title="getName(cell)"
            @click="selectCell(Math.floor(index / size), index % size)"
          >
            <img class="species-image" :src="getImage(cell)" :alt="getName(cell)" />
          </button>
          <div v-if="gameOver" class="pass-mask">
            <div class="result-title">本局结束</div>
            <div class="result-desc">{{ endReason }}，得分 {{ score }}，步数 {{ moves }}</div>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :lg="8">
        <div class="leaderboard-panel">
          <a-typography-title :level="4">{{ currentDifficulty.label }}难度{{ modeText }}排行榜</a-typography-title>
          <div v-if="currentLeaderboard.length" class="leaderboard">
            <div v-for="item in currentLeaderboard" :key="item.userId" class="leaderboard-row">
              <span class="rank">#{{ item.rank }}</span>
              <span class="score">{{ item.userName }}：{{ item.score }} 分</span>
              <span class="combo">{{ item.totalPressCount }} 步</span>
            </div>
          </div>
          <a-empty v-else description="暂无成绩" />
          <a-divider />
          <a-statistic title="我的当前模式最高分" :value="currentMyBest?.score || 0" />

          <a-divider />

          <a-typography-title :level="5">{{ currentDifficulty.label }}难度{{ anotherLeaderboardTitle }}</a-typography-title>
          <div v-if="anotherLeaderboard.length" class="leaderboard compact">
            <div v-for="item in anotherLeaderboard" :key="item.userId" class="leaderboard-row">
              <span class="rank">#{{ item.rank }}</span>
              <span class="score">{{ item.userName }}：{{ item.score }} 分</span>
              <span class="combo">{{ item.totalPressCount }} 步</span>
            </div>
          </div>
          <a-empty v-else description="暂无成绩" />

          <a-divider />

          <a-typography-title :level="4">规则</a-typography-title>
          <ul class="rule-list">
            <li>开局前选择模式、难度和图片数量，图片需要刚好勾选指定数量。</li>
            <li>点击两个相邻图块进行交换，只有形成三连及以上才算有效消除。</li>
            <li>计时模式限时 2 分钟，记步模式限制 20 步，两种模式分别排名。</li>
            <li>消除的图片越多得分越高，本局结束后会提交当前模式排行榜。</li>
            <li>每消除一个图片记一分，交换后没形成三连及以上不加分</li>
          </ul>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.game-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.game-guide {
  margin-bottom: 16px;
}

.asset-panel {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.asset-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
}

.asset-option {
  display: grid;
  grid-template-columns: 20px 42px 1fr;
  gap: 8px;
  align-items: center;
  padding: 8px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.asset-option img {
  width: 42px;
  height: 42px;
  object-fit: cover;
  border-radius: 6px;
}

.match-board {
  position: relative;
  display: grid;
  width: min(560px, 100%);
  gap: 6px;
}

.hidden-file {
  display: none;
}

.difficulty-label {
  color: #475569;
  font-size: 13px;
}

.difficulty-select {
  width: 96px;
}

.match-cell {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1;
  padding: 0;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.match-cell.selected {
  z-index: 1;
  background: #e6f4ff;
  border-color: #d90921;
  box-shadow:
    0 0 0 3px rgb(22 119 255 / 28%),
    inset 0 0 0 4px #0958d9,
    0 8px 18px rgb(22 119 255 / 22%);
  transform: scale(1.035);
}

.match-cell.selected::after {
  position: absolute;
  inset: 6px;
  border: 2px solid #fff;
  border-radius: 5px;
  content: "";
  pointer-events: none;
}

.species-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pass-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  background: rgb(248 250 252 / 86%);
  color: #16a34a;
  text-align: center;
}

.result-title {
  font-size: 34px;
  font-weight: 800;
}

.result-desc {
  color: #334155;
  font-size: 15px;
  font-weight: 700;
}

.leaderboard-panel {
  padding: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.leaderboard {
  display: grid;
  gap: 8px;
}

.leaderboard-row {
  display: grid;
  grid-template-columns: 44px 1fr 70px;
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

.rule-list {
  padding-left: 20px;
  margin: 0;
  color: #475569;
  line-height: 1.9;
}
</style>
