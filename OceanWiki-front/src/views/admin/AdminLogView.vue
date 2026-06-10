<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责AdminLog页面的数据请求、交互逻辑和界面展示。
import { onMounted, reactive, ref } from 'vue'
import { DownloadOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'

import http from '@/api/http'
import type { CommonResp, OperationLog, PageResp } from '@/types'

const logs = ref<OperationLog[]>([])
const keyword = ref('')
const loading = ref(false)
const pagination = reactive({
  // 当前页码，传给后端分页查询
  current: 1,
  // 每页条数，传给后端限制返回数量
  pageSize: 10,
  // 日志总数，后端返回后用于分页器展示
  total: 0,
})

// 分页查询操作日志
const loadData = async () => {
  loading.value = true
  try {
    const resp = await http.get<CommonResp<PageResp<OperationLog>>>('/operationLog/page', {
      params: {
        // 日志关键词，后端按模块、动作或内容做模糊查询
        keyword: keyword.value.trim() || undefined,
        // 当前页码
        current: pagination.current,
        // 每页条数
        pageSize: pagination.pageSize,
      },
    })
    logs.value = resp.data.content?.records || []
    pagination.total = Number(resp.data.content?.total || 0)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (page: { current?: number; pageSize?: number }) => {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  loadData()
}

const search = () => {
  pagination.current = 1
  loadData()
}

const exportData = () => {
  const params = new URLSearchParams()
  if (keyword.value.trim()) {
    // 导出时使用同一个搜索关键词，保持列表和导出结果一致
    params.set('keyword', keyword.value.trim())
  }
  window.open(`/operationLog/export?${params.toString()}`, '_blank')
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-typography-title :level="3">操作日志</a-typography-title>
      <div class="toolbar-left">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按模块、动作或内容模糊搜索"
          style="width: 280px"
          @search="search"
        >
          <template #enterButton>
            <a-button>
              <SearchOutlined />
              搜索
            </a-button>
          </template>
        </a-input-search>
        <a-button @click="loadData">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button @click="exportData">
          <DownloadOutlined />
          导出
        </a-button>
      </div>
    </div>

    <div class="panel">
      <a-table
        :data-source="logs"
        :loading="loading"
        row-key="id"
        bordered
        :pagination="pagination"
        @change="handleTableChange"
      >
        <a-table-column title="用户ID" data-index="userId" width="100" />
        <a-table-column title="模块" data-index="module" width="140" />
        <a-table-column title="动作" data-index="action" width="140" />
        <a-table-column title="内容" data-index="content" />
        <a-table-column title="修改前" data-index="beforeData" />
        <a-table-column title="修改后" data-index="afterData" />
        <a-table-column title="操作时间" data-index="createTime" width="190" />
      </a-table>
    </div>
  </div>
</template>
