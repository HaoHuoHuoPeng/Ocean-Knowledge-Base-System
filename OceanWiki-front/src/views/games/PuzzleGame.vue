<script setup lang="ts">
// 文件说明：这个组件负责拼图小游戏，支持选择默认物种图、上传图片、移动拼块和排行榜。
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import { queryGameLeaderboard, queryMyGameBest, submitGameScore, type GameCode } from '@/api/gameScore'
import type { GameLeaderboardItem } from '@/types'
import { getLoginUser } from '@/utils/auth'
import { createUploadedGameAssets, defaultGameAssets, findGameAsset, type GameAsset } from '@/utils/gameAssets'

type DifficultyKey = 'easy' | 'normal'

interface Tile {
  id: number
  correctIndex: number
}

const GAME_CODE_MAP: Record<DifficultyKey, GameCode> = {
  easy: 'puzzle_easy',
  normal: 'puzzle_normal',
}
const difficultyOptions: { label: string; value: DifficultyKey }[] = [
  { label: '入门 3x3', value: 'easy' },
  { label: '普通 4x4', value: 'normal' },
]
const sizeMap: Record<DifficultyKey, number> = {
  easy: 3,
  normal: 4,
}

const difficulty = ref<DifficultyKey>('easy')
const uploadedAssets = ref<GameAsset[]>([])
const selectedAssetId = ref(defaultGameAssets[0].id)
const uploadInput = ref<HTMLInputElement>()
const tiles = ref<Tile[]>([])
const emptyIndex = ref(0)
const moves = ref(0)
const elapsedSeconds = ref(0)
const gameOver = ref(false)
const gameStarted = ref(false)
const leaderboard = ref<GameLeaderboardItem[]>([])
const myBest = ref<GameLeaderboardItem | null>(null)
const resultSubmitted = ref(false)
const isPreviewing = ref(false)

let timerId = 0
const PREVIEW_KEY = 'h'

const size = computed(() => sizeMap[difficulty.value])
const currentGameCode = computed(() => GAME_CODE_MAP[difficulty.value])
const allAssets = computed(() => [...defaultGameAssets, ...uploadedAssets.value])
const currentAsset = computed(() => findGameAsset(allAssets.value, selectedAssetId.value) || defaultGameAssets[0])
const boardStyle = computed(() => ({
  gridTemplateColumns: `repeat(${size.value}, 1fr)`,
}))
const previewStyle = computed(() => ({
  backgroundImage: `url("${currentAsset.value.url}")`,
}))

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

const isSolved = () => {
  return tiles.value.every((tile, index) => tile.id === 0 || tile.correctIndex === index)
}

const canMove = (index: number) => {
  const row = Math.floor(index / size.value)
  const col = index % size.value
  const emptyRow = Math.floor(emptyIndex.value / size.value)
  const emptyCol = emptyIndex.value % size.value
  return Math.abs(row - emptyRow) + Math.abs(col - emptyCol) === 1
}

const swapWithEmpty = (index: number, countMove: boolean) => {
  const target = tiles.value[index]
  tiles.value[index] = tiles.value[emptyIndex.value]
  tiles.value[emptyIndex.value] = target
  emptyIndex.value = index
  if (countMove) {
    moves.value += 1
  }
}

const shuffleBoard = () => {
  for (let i = 0; i < size.value * size.value * 30; i += 1) {
    const neighbors = tiles.value
      .map((_tile, index) => index)
      .filter((index) => canMove(index))
    const next = neighbors[Math.floor(Math.random() * neighbors.length)]
    swapWithEmpty(next, false)
  }
  if (isSolved()) {
    shuffleBoard()
  }
}

const resetGame = () => {
  stopTimer()
  gameStarted.value = false
  const total = size.value * size.value
  tiles.value = Array.from({ length: total }, (_item, index) => ({
    id: index === total - 1 ? 0 : index + 1,
    correctIndex: index,
  }))
  emptyIndex.value = total - 1
  moves.value = 0
  elapsedSeconds.value = 0
  gameOver.value = false
  resultSubmitted.value = false
  shuffleBoard()
}

const startGame = () => {
  resetGame()
  gameStarted.value = true
  startTimer()
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

const submitResult = async () => {
  if (resultSubmitted.value || !getLoginUser()) {
    return
  }
  resultSubmitted.value = true
  /*入门3×3基础分为10000，普通4×4基础分为20000*/
  const baseScore = size.value === 3 ? 10000 : 20000
  /*基础分基础上每走一步扣20分，每过一秒扣5分，最低为1分，计为最终成绩*/
  const finalScore = Math.max(baseScore - moves.value * 20 - elapsedSeconds.value * 5, 1)
  const resp = await submitGameScore({
    gameCode: currentGameCode.value,
    score: finalScore,
    totalPressCount: moves.value,
    wrongCount: 0,
    accuracy: 100,
    maxCombo: elapsedSeconds.value,
    reason: `${size.value}x${size.value}，${currentAsset.value.name}`,
  })
  if (resp.data.success) {
    await loadLeaderboard()
    message.success('拼图成绩已提交排行榜')
  }
}

const finishGame = () => {
  stopTimer()
  gameStarted.value = false
  gameOver.value = true
  message.success('拼图完成')
  void submitResult()
}

const moveTile = (index: number) => {
  if (!gameStarted.value) {
    message.warning('请先点击开始游戏')
    return
  }
  if (gameOver.value || !canMove(index)) {
    return
  }
  swapWithEmpty(index, true)
  if (isSolved()) {
    finishGame()
  }
}

const isTypingTarget = (target: EventTarget | null) => {
  const element = target as HTMLElement | null
  return ['INPUT', 'TEXTAREA', 'SELECT'].includes(element?.tagName || '')
}

const handleKeydown = (event: KeyboardEvent) => {
  if (isTypingTarget(event.target)) {
    return
  }
  if (event.key.toLowerCase() === PREVIEW_KEY) {
    if (!gameStarted.value || gameOver.value) {
      return
    }
    event.preventDefault()
    isPreviewing.value = true
  }
}

const handleKeyup = (event: KeyboardEvent) => {
  if (event.key.toLowerCase() === PREVIEW_KEY) {
    event.preventDefault()
    isPreviewing.value = false
  }
}

const tileStyle = (tile: Tile) => {
  if (tile.id === 0) {
    return {}
  }
  const row = Math.floor(tile.correctIndex / size.value)
  const col = tile.correctIndex % size.value
  return {
    backgroundImage: `url("${currentAsset.value.url}")`,
    backgroundSize: `${size.value * 100}% ${size.value * 100}%`,
    backgroundPosition: `${(col / (size.value - 1)) * 100}% ${(row / (size.value - 1)) * 100}%`,
  }
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
  selectedAssetId.value = assets[0].id
  message.success('拼图图片已上传')
  input.value = ''
  resetGame()
}

watch(difficulty, () => {
  resetGame()
  void loadLeaderboard()
})

onMounted(() => {
  resetGame()
  void loadLeaderboard()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('keyup', handleKeyup)
})

onBeforeUnmount(() => {
  stopTimer()
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('keyup', handleKeyup)
})
</script>

<template>
  <div class="game-card">
    <div class="game-toolbar">
      <div>
        <a-typography-title :level="4">拼图游戏</a-typography-title>
        <div class="muted">点击空格旁边的拼块移动，按住 H 查看完整图片，松开 H 恢复当前进度。</div>
      </div>
      <a-space wrap>
        <a-select v-model:value="difficulty" class="difficulty-select" :options="difficultyOptions" @change="resetGame" />
        <a-select v-model:value="selectedAssetId" class="image-select" @change="resetGame">
          <a-select-option v-for="asset in allAssets" :key="asset.id" :value="asset.id">
            {{ asset.name }}
          </a-select-option>
        </a-select>
        <a-button @click="triggerUpload">
          <UploadOutlined />
          上传图片
        </a-button>
        <input ref="uploadInput" class="hidden-file" type="file" accept="image/*" @change="handleUpload" />
        <a-statistic title="步数" :value="moves" />
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
      description="选择难度和图片后开始拼图。点击空白格旁边的拼块移动，逐步还原完整图片；按住 H 可以临时查看原图，松开后继续当前进度。"
    />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <div class="puzzle-board" :style="boardStyle">
          <button
            v-for="(tile, index) in tiles"
            :key="`${tile.id}-${index}`"
            class="puzzle-tile"
            :class="{ empty: tile.id === 0 }"
            type="button"
            :style="tileStyle(tile)"
            @click="moveTile(index)"
          >
            <span v-if="tile.id !== 0">{{ tile.id }}</span>
          </button>
          <div v-if="isPreviewing && !gameOver" class="original-preview" :style="previewStyle">
            <div class="preview-tip">松开 H 继续拼图</div>
          </div>
          <div v-if="gameOver" class="pass-mask">
            <div class="result-title">恭喜完成</div>
            <div class="result-desc">用时 {{ elapsedSeconds }} 秒，移动 {{ moves }} 步</div>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :lg="8">
        <div class="leaderboard-panel">
          <a-typography-title :level="4">{{ size }}x{{ size }} 排行榜</a-typography-title>
          <div v-if="leaderboard.length" class="leaderboard">
            <div v-for="item in leaderboard" :key="item.userId" class="leaderboard-row">
              <span class="rank">#{{ item.rank }}</span>
              <span class="score">{{ item.userName }}：{{ item.score }} 分</span>
              <span class="combo">{{ item.totalPressCount }} 步</span>
            </div>
          </div>
          <a-empty v-else description="暂无成绩" />
          <a-divider />
          <a-statistic title="我的最高分" :value="myBest?.score || 0" />

          <a-divider />

          <a-typography-title :level="4">规则</a-typography-title>
          <ul class="rule-list">
            <li>先选择 3x3 或 4x4 难度，也可以上传本地图片作为拼图素材。</li>
            <li>只能移动空白格上下左右相邻的拼块，每移动一次都会记录步数。</li>
            <li>按住 H 可以查看完整原图，松开 H 会恢复到当前拼图进度。</li>
            <li>还原完整图片后通关，系统会根据用时和步数计算排行榜得分。</li>
            <li>入门3×3的基础分是10000，普通4×4的基础分是20000</li>
            <li>在基础分的基础上每走一步扣20分，每过一秒扣5分，最低不低于1分</li>
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

.difficulty-select {
  width: 118px;
}

.image-select {
  width: 140px;
}

.hidden-file {
  display: none;
}

.puzzle-board {
  position: relative;
  display: grid;
  width: min(560px, 100%);
  gap: 4px;
  padding: 6px;
  background: #cbd5e1;
  border-radius: 8px;
}

.puzzle-tile {
  display: flex;
  aspect-ratio: 1;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 6px;
  overflow: hidden;
  color: #fff;
  cursor: pointer;
  background-color: #0f172a;
  background-repeat: no-repeat;
  border: 0;
  border-radius: 6px;
  font-weight: 800;
  text-shadow: 0 1px 3px rgb(0 0 0 / 60%);
}

.puzzle-tile.empty {
  cursor: default;
  background: #f8fafc;
}

.puzzle-tile.empty span {
  display: none;
}

.original-preview {
  position: absolute;
  inset: 6px;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  pointer-events: none;
  background-color: #0f172a;
  background-position: center;
  background-size: cover;
  border-radius: 6px;
  box-shadow: inset 0 0 0 3px rgb(22 119 255 / 55%);
}

.preview-tip {
  width: 100%;
  padding: 10px;
  color: #fff;
  text-align: center;
  background: rgb(15 23 42 / 72%);
  font-size: 14px;
  font-weight: 700;
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
