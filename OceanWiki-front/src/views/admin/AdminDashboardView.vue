<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责后台看板展示，超级管理员可以快速看到系统管理数据和运营趋势。
import { computed, onMounted, reactive, ref } from 'vue'
import {
  BookOutlined,
  CommentOutlined,
  DownloadOutlined,
  MessageOutlined,
  ReadOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'

import http from '@/api/http'
import type { AdminDashboard, ChartItem, CommonResp, OperationLog, PageResp, StatisticRecord } from '@/types'

const loading = ref(false)
const logLoading = ref(false)
const logs = ref<OperationLog[]>([])
const logKeyword = ref('')
const logPagination = reactive({
  // 操作日志当前页码
  current: 1,
  // 操作日志每页条数
  pageSize: 8,
  // 操作日志总数
  total: 0,
})
const dashboard = ref<AdminDashboard>({
  // 电子书总数
  ebookCount: 0,
  // 已发布电子书数量
  publishedEbookCount: 0,
  // 待审核电子书数量
  pendingEbookCount: 0,
  // 待审核文档数量
  pendingDocCount: 0,
  // 文档总数
  docCount: 0,
  // 用户总数
  userCount: 0,
  // 待审核评论数量
  pendingCommentCount: 0,
  // 未处理反馈数量
  openFeedbackCount: 0,
  // 今日阅读数量
  todayReadCount: 0,
  // 阅读和点赞趋势图数据
  trend: [],
  // 待处理事项统计
  todoStats: [],
  // 电子书状态统计
  ebookStatusStats: [],
  // 分类阅读排行
  categoryRank: [],
})

const trendPoints = computed(() => {
  const records = dashboard.value.trend || []
  return {
    view: buildPolyline(records, 'viewIncrease'),
    vote: buildPolyline(records, 'voteIncrease'),
  }
})

const trendLabels = computed(() => {
  const records = dashboard.value.trend || []
  if (!records.length) {
    return []
  }
  const first = records[0]?.date || ''
  const middle = records[Math.floor(records.length / 2)]?.date || ''
  const last = records[records.length - 1]?.date || ''
  return [first, middle, last].filter(Boolean)
})

const maxTodoValue = computed(() => getMaxValue(dashboard.value.todoStats || []))
const maxCategoryValue = computed(() => getMaxValue(dashboard.value.categoryRank || []))
const totalStatusValue = computed(() => (dashboard.value.ebookStatusStats || []).reduce((sum, item) => sum + Number(item.value || 0), 0))

const statusRingStyle = computed(() => {
  const list = dashboard.value.ebookStatusStats || []
  const colors = ['#64748b', '#faad14', '#52c41a', '#f5222d']
  let current = 0
  const segments = list.map((item, index) => {
    const percent = totalStatusValue.value ? (Number(item.value || 0) / totalStatusValue.value) * 100 : 0
    const start = current
    current += percent
    return `${colors[index % colors.length]} ${start}% ${current}%`
  })
  return {
    background: `conic-gradient(${segments.length ? segments.join(', ') : '#e5e7eb 0% 100%'})`,
  }
})

// 查询后台看板数据
const loadData = async () => {
  loading.value = true
  try {
    const resp = await http.get<CommonResp<AdminDashboard>>('/dashboard/overview')
    dashboard.value = {
      ...dashboard.value,
      ...(resp.data.content || {}),
    }
  } finally {
    loading.value = false
  }
}

// 分页查询操作日志
// 这里放在看板底部，超级管理员打开看板时就能顺手看到最近的关键操作
const loadLogs = async () => {
  logLoading.value = true
  try {
    const resp = await http.get<CommonResp<PageResp<OperationLog>>>('/operationLog/page', {
      params: {
        // 日志关键词，后端按模块、动作或内容做模糊查询
        keyword: logKeyword.value.trim() || undefined,
        // 当前页码
        current: logPagination.current,
        // 每页条数
        pageSize: logPagination.pageSize,
      },
    })
    logs.value = resp.data.content?.records || []
    logPagination.total = Number(resp.data.content?.total || 0)
  } finally {
    logLoading.value = false
  }
}

const handleLogTableChange = (page: { current?: number; pageSize?: number }) => {
  logPagination.current = page.current || 1
  logPagination.pageSize = page.pageSize || 8
  loadLogs()
}

const searchLogs = () => {
  logPagination.current = 1
  loadLogs()
}

const exportLogs = async () => {
  // 不能用 window.open 导出，因为新窗口请求不会带 Authorization 请求头，后端会判断为未登录
  const resp = await http.get('/operationLog/export', {
    params: {
      // 导出时使用同一个搜索关键词，保持看板日志列表和导出结果一致
      keyword: logKeyword.value.trim() || undefined,
    },
    responseType: 'blob',
  })
  downloadCsv(resp.data, '操作日志.csv')
}

const downloadCsv = (data: BlobPart, fileName: string) => {
  // 用 Blob 主动下载，可以复用 axios 里的 token 请求头，也能避免浏览器直接打开 CSV
  const blob = new Blob([data], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const refreshAll = async () => {
  await Promise.all([loadData(), loadLogs()])
}

const buildPolyline = (records: StatisticRecord[], key: 'viewIncrease' | 'voteIncrease') => {
  if (!records.length) {
    return ''
  }
  const width = 720
  const height = 220
  const padding = 18
  const maxValue = Math.max(...records.map((item) => Number(item[key] || 0)), 1)
  return records
    .map((item, index) => {
      const x = padding + (index / Math.max(records.length - 1, 1)) * (width - padding * 2)
      const y = height - padding - (Number(item[key] || 0) / maxValue) * (height - padding * 2)
      return `${x},${y}`
    })
    .join(' ')
}

const getMaxValue = (list: ChartItem[]) => {
  return Math.max(...list.map((item) => Number(item.value || 0)), 1)
}

const percentWidth = (value: number, maxValue: number) => {
  return `${Math.max(4, (Number(value || 0) / maxValue) * 100)}%`
}

onMounted(refreshAll)
</script>

<template>
  <div class="page dashboard-page">
    <div class="toolbar">
      <a-typography-title :level="3">后台看板</a-typography-title>
      <a-button @click="refreshAll">
        <ReloadOutlined />
        刷新
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <BookOutlined />
            <a-statistic title="电子书总数" :value="dashboard.ebookCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <ReadOutlined />
            <a-statistic title="已发布电子书" :value="dashboard.publishedEbookCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <CommentOutlined />
            <a-statistic title="待审核评论" :value="dashboard.pendingCommentCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <MessageOutlined />
            <a-statistic title="待处理反馈" :value="dashboard.openFeedbackCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <TeamOutlined />
            <a-statistic title="用户总数" :value="dashboard.userCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <ReadOutlined />
            <a-statistic title="文档总数" :value="dashboard.docCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <BookOutlined />
            <a-statistic title="待审核电子书" :value="dashboard.pendingEbookCount" />
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div class="panel stat-panel">
            <ReadOutlined />
            <a-statistic title="今日阅读用户记录" :value="dashboard.todayReadCount" />
          </div>
        </a-col>
      </a-row>

      <div class="panel chart-panel trend-panel">
        <div class="panel-title">
          <a-typography-title :level="4">最近 30 天阅读/点赞趋势</a-typography-title>
          <a-space>
            <a-tag color="blue">新增阅读</a-tag>
            <a-tag color="green">新增点赞</a-tag>
          </a-space>
        </div>
        <svg class="trend-chart" viewBox="0 0 720 220" preserveAspectRatio="none">
          <line x1="18" y1="202" x2="702" y2="202" class="axis-line" />
          <line x1="18" y1="18" x2="18" y2="202" class="axis-line" />
          <polyline v-if="trendPoints.view" :points="trendPoints.view" class="view-line" />
          <polyline v-if="trendPoints.vote" :points="trendPoints.vote" class="vote-line" />
        </svg>
        <div class="trend-labels">
          <span v-for="label in trendLabels" :key="label">{{ label }}</span>
        </div>
      </div>

      <a-row :gutter="[16, 16]" class="bottom-charts">
        <a-col :xs="24" :lg="8">
          <div class="panel chart-panel">
            <a-typography-title :level="4">待处理事项</a-typography-title>
            <div v-for="item in dashboard.todoStats" :key="item.name" class="bar-row">
              <div class="bar-meta">
                <span>{{ item.name }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div class="bar-track">
                <div class="bar-fill todo-fill" :style="{ width: percentWidth(item.value, maxTodoValue) }"></div>
              </div>
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="8">
          <div class="panel chart-panel">
            <a-typography-title :level="4">分类电子书数量排行</a-typography-title>
            <div v-if="dashboard.categoryRank?.length">
              <div v-for="item in dashboard.categoryRank" :key="item.name" class="bar-row">
                <div class="bar-meta">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
                <div class="bar-track">
                  <div class="bar-fill category-fill" :style="{ width: percentWidth(item.value, maxCategoryValue) }"></div>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无分类数据" />
          </div>
        </a-col>

        <a-col :xs="24" :lg="8">
          <div class="panel chart-panel">
            <a-typography-title :level="4">电子书状态分布</a-typography-title>
            <div class="status-chart">
              <div class="status-ring" :style="statusRingStyle">
                <div class="status-ring-center">
                  <strong>{{ totalStatusValue }}</strong>
                  <span>总数</span>
                </div>
              </div>
              <div class="status-legend">
                <div v-for="item in dashboard.ebookStatusStats" :key="item.name" class="legend-row">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>

      <div class="panel log-panel">
        <div class="panel-title log-title">
          <div>
            <a-typography-title :level="4">操作日志</a-typography-title>
<!--            <div class="muted">记录后台关键操作，方便超级管理员排查是谁在什么时间改了什么。</div>-->
          </div>
          <a-space wrap>
            <a-input-search
              v-model:value="logKeyword"
              allow-clear
              placeholder="按模块、动作或内容模糊搜索"
              style="width: 280px"
              @search="searchLogs"
            >
              <template #enterButton>
                <a-button>
                  <SearchOutlined />
                  搜索
                </a-button>
              </template>
            </a-input-search>
            <a-button @click="loadLogs">
              <ReloadOutlined />
              刷新日志
            </a-button>
            <a-button @click="exportLogs">
              <DownloadOutlined />
              导出
            </a-button>
          </a-space>
        </div>

        <a-table
          :data-source="logs"
          :loading="logLoading"
          row-key="id"
          bordered
          size="small"
          :pagination="logPagination"
          @change="handleLogTableChange"
        >
          <a-table-column title="用户ID" data-index="userId" width="100" />
          <a-table-column title="模块" data-index="module" width="130" />
          <a-table-column title="动作" data-index="action" width="130" />
          <a-table-column title="内容" data-index="content" />
          <a-table-column title="修改前" data-index="beforeData" />
          <a-table-column title="修改后" data-index="afterData" />
          <a-table-column title="操作时间" data-index="createTime" width="190" />
        </a-table>
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.dashboard-page {
  max-width: 1280px;
  margin: 0 auto;
}

.stat-panel {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stat-panel :deep(.anticon) {
  color: #1677ff;
  font-size: 26px;
}

.chart-panel {
  margin-top: 16px;
}

.log-panel {
  margin-top: 16px;
}

.panel-title {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.log-title {
  align-items: flex-start;
  margin-bottom: 14px;
}

.trend-chart {
  width: 100%;
  height: 280px;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.axis-line {
  stroke: #cbd5e1;
  stroke-width: 1;
}

.view-line,
.vote-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.view-line {
  stroke: #1677ff;
}

.vote-line {
  stroke: #16a34a;
}

.trend-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.bottom-charts {
  margin-top: 0;
}

.bar-row {
  margin-top: 14px;
}

.bar-meta {
  display: flex;
  justify-content: space-between;
  color: #334155;
  font-size: 13px;
}

.bar-track {
  height: 10px;
  margin-top: 6px;
  overflow: hidden;
  background: #e5e7eb;
  border-radius: 999px;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
}

.todo-fill {
  background: #faad14;
}

.category-fill {
  background: #1677ff;
}

.status-chart {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 20px;
  align-items: center;
  min-height: 220px;
}

.status-ring {
  display: flex;
  width: 150px;
  height: 150px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.status-ring-center {
  display: flex;
  width: 86px;
  height: 86px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 50%;
}

.status-ring-center strong {
  color: #0f172a;
  font-size: 24px;
}

.status-ring-center span {
  color: #64748b;
  font-size: 12px;
}

.legend-row {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
  color: #334155;
  border-bottom: 1px solid #e5e7eb;
}
</style>
