<script setup lang="ts">
// 文件说明：这个组件负责连连看小游戏，处理图片选择、上传图片、计时、配对消除和排行榜。
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

interface SearchNode extends Point {
  dir: number
  turns: number
}

type DifficultyKey = 'easy' | 'normal' | 'hard'

interface DifficultyConfig {
  label: string
  rowCount: number
  colCount: number
}

const GAME_CODE_MAP: Record<DifficultyKey, GameCode> = {
  easy: 'link_match_easy',
  normal: 'link_match_normal',
  hard: 'link_match_hard',
}
const difficultyOptions: { label: string; value: DifficultyKey }[] = [
  { label: '入门', value: 'easy' },
  { label: '普通', value: 'normal' },
  { label: '困难', value: 'hard' },
]
const difficultyMap: Record<DifficultyKey, DifficultyConfig> = {
  easy: { label: '入门', rowCount: 6, colCount: 6 },
  normal: { label: '普通', rowCount: 6, colCount: 8 },
  hard: { label: '困难', rowCount: 8, colCount: 8 },
}
const directions = [
  { row: -1, col: 0 },
  { row: 1, col: 0 },
  { row: 0, col: -1 },
  { row: 0, col: 1 },
]

const difficulty = ref<DifficultyKey>('easy')
const uploadedAssets = ref<GameAsset[]>([])
const selectedAssetIds = ref(defaultGameAssets.map((asset) => asset.id))
const imageCount = ref(5)
const uploadInput = ref<HTMLInputElement>()
const board = ref<(string | null)[][]>([])
const selected = ref<Point>()
const score = ref(0)
const remain = ref(0)
const elapsedSeconds = ref(0)
const gameStarted = ref(false)
const leaderboard = ref<GameLeaderboardItem[]>([])
const myBest = ref<GameLeaderboardItem | null>(null)
const resultSubmitted = ref(false)

let timerId = 0

const currentDifficulty = computed(() => difficultyMap[difficulty.value])
const currentGameCode = computed(() => GAME_CODE_MAP[difficulty.value])
const rowCount = computed(() => currentDifficulty.value.rowCount)
const colCount = computed(() => currentDifficulty.value.colCount)
const pairCount = computed(() => (rowCount.value * colCount.value) / 2)
const allAssets = computed(() => [...defaultGameAssets, ...uploadedAssets.value])
const activeAssets = computed(() => {
  return selectedAssetIds.value
    .map((id) => allAssets.value.find((asset) => asset.id === id))
    .filter(Boolean) as GameAsset[]
})
const boardStyle = computed(() => ({
  gridTemplateColumns: `repeat(${colCount.value}, 1fr)`,
}))
const assetOptions = computed(() =>
  allAssets.value.map((asset) => ({
    label: asset.name,
    value: asset.id,
  })),
)

// 校验开局图片数量
// 用户指定几张，就必须刚好勾选几张；资源不够时直接给错误提示
const validateAssets = () => {
  if (allAssets.value.length < imageCount.value) {
    message.error(`当前只有 ${allAssets.value.length} 张可用图片，不够选择 ${imageCount.value} 张`)
    return false
  }
  if (selectedAssetIds.value.length !== imageCount.value) {
    message.error(`请刚好勾选 ${imageCount.value} 张图片，当前勾选了 ${selectedAssetIds.value.length} 张`)
    return false
  }
  if (imageCount.value > pairCount.value) {
    message.error(`当前难度最多使用 ${pairCount.value} 张图片，请减少指定数量或提高难度`)
    return false
  }
  return true
}

const shuffle = <T,>(items: T[]) => {
  const result = [...items]
  for (let i = result.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

const stopTimer = () => {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = 0
  }
}

const startTimer = () => {
  stopTimer()
  elapsedSeconds.value = 0
  timerId = window.setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

const loadLeaderboard = async () => {
  if (!getLoginUser()) {
    leaderboard.value = []
    myBest.value = null
    return
  }

  const leaderboardResp = await queryGameLeaderboard(currentGameCode.value)
  leaderboard.value = leaderboardResp.data.content || []

  const myBestResp = await queryMyGameBest(currentGameCode.value)
  myBest.value = myBestResp.data.content || null
}

const resetGame = () => {
  stopTimer()
  gameStarted.value = false
  selected.value = undefined
  board.value = []
  remain.value = 0
  elapsedSeconds.value = 0
  if (!validateAssets()) {
    return
  }

  const values: string[] = []
  for (let i = 0; i < pairCount.value; i += 1) {
    const value = activeAssets.value[i % activeAssets.value.length].id
    values.push(value, value)
  }

  const shuffled = shuffle(values)
  board.value = Array.from({ length: rowCount.value }, (_emptyRow, row) =>
    Array.from({ length: colCount.value }, (_cell, col) => shuffled[row * colCount.value + col]),
  )
  selected.value = undefined
  score.value = 0
  remain.value = rowCount.value * colCount.value
  resultSubmitted.value = false
}

const startGame = () => {
  resetGame()
  if (!remain.value) {
    return
  }
  gameStarted.value = true
  startTimer()
}

const submitResult = async () => {
  if (resultSubmitted.value || !getLoginUser()) {
    return
  }

  resultSubmitted.value = true
  const resp = await submitGameScore({
    gameCode: currentGameCode.value,
    score: elapsedSeconds.value,
    totalPressCount: imageCount.value,
    wrongCount: 0,
    accuracy: 100,
    maxCombo: pairCount.value,
    reason: `${currentDifficulty.value.label}难度`,
  })

  if (resp.data.success) {
    await loadLeaderboard()
    message.success('连连看成绩已提交排行榜')
  }
}

const finishGame = () => {
  stopTimer()
  gameStarted.value = false
  message.success('连连看通关')
  void submitResult()
}

const isSamePoint = (a?: Point, b?: Point) => {
  return !!a && !!b && a.row === b.row && a.col === b.col
}

const isBlocked = (row: number, col: number, start: Point, end: Point) => {
  if (row < 0 || row > rowCount.value + 1 || col < 0 || col > colCount.value + 1) {
    return true
  }
  if (row === start.row && col === start.col) {
    return false
  }
  if (row === end.row && col === end.col) {
    return false
  }
  if (row === 0 || row === rowCount.value + 1 || col === 0 || col === colCount.value + 1) {
    return false
  }
  return board.value[row - 1][col - 1] !== null
}

const canConnect = (first: Point, second: Point) => {
  const start = { row: first.row + 1, col: first.col + 1 }
  const end = { row: second.row + 1, col: second.col + 1 }
  const queue: SearchNode[] = [{ ...start, dir: -1, turns: 0 }]
  const visited = new Set<string>()

  while (queue.length) {
    const current = queue.shift()!
    const key = `${current.row}-${current.col}-${current.dir}-${current.turns}`
    if (visited.has(key)) {
      continue
    }
    visited.add(key)

    if (current.row === end.row && current.col === end.col) {
      return true
    }

    directions.forEach((direction, dirIndex) => {
      const nextTurns = current.dir === -1 || current.dir === dirIndex ? current.turns : current.turns + 1
      if (nextTurns > 2) {
        return
      }

      const next = {
        row: current.row + direction.row,
        col: current.col + direction.col,
        dir: dirIndex,
        turns: nextTurns,
      }

      if (!isBlocked(next.row, next.col, start, end)) {
        queue.push(next)
      }
    })
  }

  return false
}

const selectCell = (row: number, col: number) => {
  if (!gameStarted.value) {
    message.warning('请先点击开始游戏')
    return
  }
  const value = board.value[row][col]
  if (!value || remain.value === 0) {
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

  const firstValue = board.value[selected.value.row][selected.value.col]
  if (firstValue === value && canConnect(selected.value, current)) {
    board.value[selected.value.row][selected.value.col] = null
    board.value[row][col] = null
    remain.value -= 2
    score.value += 10
    selected.value = undefined
    if (remain.value === 0) {
      finishGame()
    }
    return
  }

  message.warning('这两个不能连接')
  selected.value = current
}

const getImage = (id: string | null) => {
  return findGameAsset(allAssets.value, id)?.url || ''
}

const getName = (id: string | null) => {
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
        <a-typography-title :level="4">连连看</a-typography-title>
        <div class="muted">选中两个相同图块，路径转弯不超过两次就能消除，通关后按用时排名。</div>
      </div>
      <a-space wrap>
        <span class="difficulty-label">难度</span>
        <a-select v-model:value="difficulty" class="difficulty-select" :options="difficultyOptions" />
        <a-statistic title="得分" :value="score" />
        <a-statistic title="剩余" :value="remain" />
        <a-statistic title="用时" :value="elapsedSeconds" suffix="秒" />
        <a-button type="primary" @click="startGame">开始游戏</a-button>
        <a-button @click="resetGame">重置</a-button>
      </a-space>
    </div>

    <a-alert
      class="game-guide"
      type="info"
      show-icon
      message="玩法说明"
      description="先选择难度和指定数量的图片，再点击两个相同图块。两图之间路径转弯不超过两次就能消除，全部消除后按通关用时进入排行榜。"
    />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <div class="asset-panel">
          <div class="asset-toolbar">
            <a-space wrap>
              <span>指定图片数量</span>
              <a-input-number v-model:value="imageCount" :min="1" :max="pairCount" />
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

        <div class="link-board" :style="boardStyle">
          <button
            v-for="(cell, index) in board.flat()"
            :key="index"
            class="link-cell"
            :class="{ selected: isSamePoint(selected, { row: Math.floor(index / colCount), col: index % colCount }) }"
            type="button"
            :title="getName(cell)"
            @click="selectCell(Math.floor(index / colCount), index % colCount)"
          >
            <img v-if="cell" class="species-image" :src="getImage(cell)" :alt="getName(cell)" />
          </button>
          <div v-if="remain === 0" class="pass-mask">恭喜通过</div>
        </div>
      </a-col>

      <a-col :xs="24" :lg="8">
        <div class="leaderboard-panel">
          <a-typography-title :level="4">{{ currentDifficulty.label }}难度排行榜</a-typography-title>
          <a-alert class="rank-tip" type="info" show-icon message="连连看按当前难度的通关用时从少到多排名。" />
          <div v-if="leaderboard.length" class="leaderboard">
            <div v-for="item in leaderboard" :key="item.userId" class="leaderboard-row">
              <span class="rank">#{{ item.rank }}</span>
              <span class="score">{{ item.userName }}：{{ item.score }} 秒</span>
              <span class="combo">{{ item.reason }}</span>
            </div>
          </div>
          <a-empty v-else description="暂无成绩" />
          <a-divider />
          <a-statistic title="我的最好用时" :value="myBest?.score || 0" suffix="秒" />

          <a-divider />

          <a-typography-title :level="4">规则</a-typography-title>
          <ul class="rule-list">
            <li>开局前必须刚好勾选指定数量的图片，图片不够会提示错误。</li>
            <li>点击两个相同图片，如果连接路径最多只转弯两次，就可以消除。</li>
            <li>点错不会结束游戏，会把后点的图块作为新的选中目标。</li>
            <li>全部图块消除后通关，排行榜按通关用时从少到多排序。</li>
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

.link-board {
  position: relative;
  display: grid;
  width: min(560px, 100%);
  gap: 8px;
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

.link-cell {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1;
  padding: 0;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.link-cell.selected {
  z-index: 1;
  background: #e6f4ff;
  border-color: #d90928;
  box-shadow:
    0 0 0 3px rgb(22 119 255 / 28%),
    inset 0 0 0 4px #0958d9,
    0 8px 18px rgb(22 119 255 / 22%);
  transform: scale(1.035);
}

.link-cell.selected::after {
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
  align-items: center;
  justify-content: center;
  background: rgb(248 250 252 / 86%);
  color: #16a34a;
  font-size: 34px;
  font-weight: 800;
}

.leaderboard-panel {
  padding: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.rank-tip {
  margin-bottom: 10px;
}

.leaderboard {
  display: grid;
  gap: 8px;
}

.leaderboard-row {
  display: grid;
  grid-template-columns: 44px 1fr 88px;
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

@media (max-width: 760px) {
  .link-board {
    gap: 6px;
  }
}
</style>
