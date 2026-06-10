<script setup lang="ts">
// 文件说明：这个组件负责 2048 小游戏，处理数字合成、得分、本地最高分和用户排行榜。
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'

import { queryGameLeaderboard, queryMyGameBest, submitGameScore } from '@/api/gameScore'
import type { GameLeaderboardItem } from '@/types'
import { getLoginUser } from '@/utils/auth'

const GAME_CODE = 'game_2048'
const size = 4
const board = ref<number[][]>([])
const score = ref(0)
const bestScore = ref(Number(localStorage.getItem('ocean-2048-best') || 0))
const gameOver = ref(false)
const resultSubmitted = ref(false)
const leaderboard = ref<GameLeaderboardItem[]>([])
const myBest = ref<GameLeaderboardItem | null>(null)

const emptyCells = computed(() => {
  const cells: { row: number; col: number }[] = []
  board.value.forEach((rowItems, row) => {
    rowItems.forEach((value, col) => {
      if (value === 0) {
        cells.push({ row, col })
      }
    })
  })
  return cells
})

const maxTile = computed(() => {
  return Math.max(...board.value.flat(), 0)
})

const createEmptyBoard = () => {
  board.value = Array.from({ length: size }, () => Array.from({ length: size }, () => 0))
}

const addRandomTile = () => {
  const cells = emptyCells.value
  if (!cells.length) {
    return
  }
  const cell = cells[Math.floor(Math.random() * cells.length)]
  board.value[cell.row][cell.col] = Math.random() < 0.9 ? 2 : 4
}

const loadLeaderboard = async () => {
  if (!getLoginUser()) {
    leaderboard.value = []
    myBest.value = null
    return
  }

  const leaderboardResp = await queryGameLeaderboard(GAME_CODE)
  leaderboard.value = leaderboardResp.data.content || []

  const myBestResp = await queryMyGameBest(GAME_CODE)
  myBest.value = myBestResp.data.content || null
}

const submitResult = async (reason = '手动提交') => {
  if (!getLoginUser()) {
    message.warning('登录后才能保存排行榜成绩')
    return
  }
  if (score.value <= 0) {
    message.warning('当前还没有得分，先移动合成数字后再提交')
    return
  }
  if (resultSubmitted.value && reason !== '手动提交') {
    return
  }

  resultSubmitted.value = true
  const resp = await submitGameScore({
    gameCode: GAME_CODE,
    score: score.value,
    totalPressCount: emptyCells.value.length,
    wrongCount: 0,
    accuracy: 100,
    maxCombo: maxTile.value,
    reason,
  })

  if (resp.data.success) {
    await loadLeaderboard()
    message.success('2048 成绩已提交排行榜')
  }
}

const resetGame = () => {
  score.value = 0
  gameOver.value = false
  resultSubmitted.value = false
  createEmptyBoard()
  addRandomTile()
  addRandomTile()
}

const hasAvailableMove = () => {
  if (emptyCells.value.length) {
    return true
  }

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col < size; col += 1) {
      const value = board.value[row][col]
      if (row + 1 < size && board.value[row + 1][col] === value) {
        return true
      }
      if (col + 1 < size && board.value[row][col + 1] === value) {
        return true
      }
    }
  }

  return false
}

const mergeLine = (line: number[]) => {
  const values = line.filter(Boolean)
  const merged: number[] = []

  for (let i = 0; i < values.length; i += 1) {
    if (values[i] === values[i + 1]) {
      const newValue = values[i] * 2
      merged.push(newValue)
      score.value += newValue
      i += 1
    } else {
      merged.push(values[i])
    }
  }

  while (merged.length < size) {
    merged.push(0)
  }

  return merged
}

const boardText = () => JSON.stringify(board.value)

const createEmptyMatrix = () => {
  return Array.from({ length: size }, () => Array.from({ length: size }, () => 0))
}

const move = (direction: 'left' | 'right' | 'up' | 'down') => {
  if (gameOver.value) {
    return
  }

  const before = boardText()

  if (direction === 'left' || direction === 'right') {
    board.value = board.value.map((row) => {
      const source = direction === 'left' ? row : [...row].reverse()
      const merged = mergeLine(source)
      return direction === 'left' ? merged : merged.reverse()
    })
  } else {
    const nextBoard = createEmptyMatrix()
    for (let col = 0; col < size; col += 1) {
      const column = board.value.map((row) => row[col])
      const source = direction === 'up' ? column : [...column].reverse()
      const merged = mergeLine(source)
      const result = direction === 'up' ? merged : merged.reverse()
      for (let row = 0; row < size; row += 1) {
        nextBoard[row][col] = result[row]
      }
    }
    board.value = nextBoard
  }

  if (boardText() !== before) {
    addRandomTile()
    bestScore.value = Math.max(bestScore.value, score.value)
    localStorage.setItem('ocean-2048-best', String(bestScore.value))
    gameOver.value = !hasAvailableMove()
    if (gameOver.value) {
      void submitResult('无可移动步')
    }
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  const map: Record<string, 'left' | 'right' | 'up' | 'down'> = {
    ArrowLeft: 'left',
    ArrowRight: 'right',
    ArrowUp: 'up',
    ArrowDown: 'down',
  }
  const direction = map[event.key]
  if (!direction) {
    return
  }
  event.preventDefault()
  move(direction)
}

onMounted(() => {
  resetGame()
  void loadLeaderboard()
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="game-card">
    <div class="game-toolbar">
      <div>
        <a-typography-title :level="4">2048</a-typography-title>
        <div class="muted">使用方向键移动，相同数字合并，分数越高排名越高。</div>
      </div>
      <a-space wrap>
        <a-statistic title="得分" :value="score" />
        <a-statistic title="最高" :value="bestScore" />
        <a-statistic title="最大数字" :value="maxTile" />
        <a-button type="primary" @click="resetGame">重开</a-button>
        <a-button @click="submitResult('手动提交')">提交成绩</a-button>
      </a-space>
    </div>

    <a-alert
      class="game-guide"
      type="info"
      show-icon
      message="玩法说明"
      description="使用键盘方向键移动全部数字方块，相同数字相撞会合并成更大的数字。每次有效移动后会生成新数字，直到没有可移动位置时游戏结束。"
    />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <div class="board-2048">
          <div v-for="(cell, index) in board.flat()" :key="index" class="tile-2048" :class="`value-${cell}`">
            {{ cell || '' }}
          </div>
        </div>
        <a-alert v-if="gameOver" class="game-over-alert" type="warning" show-icon message="没有可移动步了，游戏结束。" />
      </a-col>

      <a-col :xs="24" :lg="8">
        <div class="leaderboard-panel">
          <a-typography-title :level="4">得分排行榜</a-typography-title>
          <div v-if="leaderboard.length" class="leaderboard">
            <div v-for="item in leaderboard" :key="item.userId" class="leaderboard-row">
              <span class="rank">#{{ item.rank }}</span>
              <span class="score">{{ item.userName }}：{{ item.score }} 分</span>
              <span class="combo">最大 {{ item.maxCombo }}</span>
            </div>
          </div>
          <a-empty v-else description="暂无成绩" />
          <a-divider />
          <a-statistic title="我的最高分" :value="myBest?.score || 0" />

          <a-divider />

          <a-typography-title :level="4">规则</a-typography-title>
          <ul class="rule-list">
            <li>使用上、下、左、右方向键移动棋盘上的全部数字。</li>
            <li>两个相同数字碰到一起会合并，合并后的数字会计入得分。</li>
            <li>每次有效移动后，棋盘空位会随机生成 2 或 4。</li>
            <li>棋盘填满且上下左右都无法合并时游戏结束，可以手动提交成绩。</li>
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

.board-2048 {
  display: grid;
  width: min(440px, 100%);
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 10px;
  background: #94a3b8;
  border-radius: 8px;
}

.tile-2048 {
  display: flex;
  aspect-ratio: 1;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  border-radius: 6px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.game-over-alert {
  width: min(440px, 100%);
  margin-top: 12px;
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
  grid-template-columns: 44px 1fr 78px;
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

.value-2,
.value-4 {
  background: #f8fafc;
}

.value-8,
.value-16,
.value-32,
.value-64 {
  background: #bfdbfe;
}

.value-128,
.value-256,
.value-512 {
  background: #86efac;
}

.value-1024,
.value-2048 {
  background: #fde68a;
}
</style>
