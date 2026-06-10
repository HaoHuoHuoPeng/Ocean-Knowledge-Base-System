<script setup lang="ts">
// 文件说明：这个页面负责统计展示，包括总阅读量、总点赞量和最近 30 天趋势。
import { onMounted, ref } from 'vue'

import http from '@/api/http'
import type { CommonResp, Statistic, StatisticRecord } from '@/types'

const statistic = ref<Statistic>({
  viewCount: 0,
  voteCount: 0,
  todayViewCount: 0,
  todayVoteCount: 0,
})
const records = ref<StatisticRecord[]>([])

// 查询统计总览和最近 30 天趋势
const loadData = async () => {
  const [statResp, recordsResp] = await Promise.all([
    http.get<CommonResp<Statistic>>('/ebookSnapshot/getStatistic'),
    http.get<CommonResp<StatisticRecord[]>>('/ebookSnapshot/get30Statistic'),
  ])

  statistic.value = {
    viewCount: Number(statResp.data.content?.viewCount || 0),
    voteCount: Number(statResp.data.content?.voteCount || 0),
    todayViewCount: Number(statResp.data.content?.todayViewCount || 0),
    todayVoteCount: Number(statResp.data.content?.todayVoteCount || 0),
  }
  records.value = recordsResp.data.content || []
}

onMounted(loadData)
</script>

<template>
  <div class="page statistics-page">
    <div class="panel">
      <a-row :gutter="[16, 16]">
        <a-col :xs="12" :lg="6">
          <a-statistic title="总阅读量" :value="statistic.viewCount" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic title="总点赞量" :value="statistic.voteCount" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic title="今日阅读" :value="statistic.todayViewCount" />
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-statistic title="今日点赞" :value="statistic.todayVoteCount" />
        </a-col>
      </a-row>
    </div>

    <div class="panel table-panel">
      <a-typography-title :level="4">最近 30 天统计</a-typography-title>
      <a-table :data-source="records" :pagination="false" row-key="date" bordered>
        <a-table-column title="日期" data-index="date" />
        <a-table-column title="总阅读" data-index="viewCount" />
        <a-table-column title="总点赞" data-index="voteCount" />
        <a-table-column title="新增阅读" data-index="viewIncrease" />
        <a-table-column title="新增点赞" data-index="voteIncrease" />
      </a-table>
    </div>
  </div>
</template>

<style scoped>
.statistics-page {
  max-width: 1180px;
  margin: 0 auto;
}

.table-panel {
  margin-top: 16px;
}
</style>
