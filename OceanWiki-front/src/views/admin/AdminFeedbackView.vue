<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责反馈页面的数据请求、交互逻辑和界面展示。
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckOutlined, MessageOutlined, ReloadOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp, FeedbackReply, IdValue, PageResp, UserFeedback } from '@/types'
import { getLoginUser, isCurrentSuperAdmin } from '@/utils/auth'

const feedbackList = ref<UserFeedback[]>([])
const replies = ref<FeedbackReply[]>([])
const loading = ref(false)
const keyword = ref('')
const reviewOnly = getLoginUser()?.roleCodes?.includes('CONTENT_REVIEWER') && !isCurrentSuperAdmin()
const status = ref<string | undefined>(reviewOnly ? 'open' : undefined)
const handleOpen = ref(false)
const batchHandleOpen = ref(false)
const replyOpen = ref(false)
const currentFeedback = ref<UserFeedback>()
const replyContent = ref('')
const selectedRowKeys = ref<IdValue[]>([])
const pageTitle = computed(() => (reviewOnly ? '反馈审核' : '反馈管理'))
const pagination = reactive({
  // 当前页码，传给后端分页查询
  current: 1,
  // 每页条数，传给后端限制返回数量
  pageSize: 10,
  // 反馈总数，后端返回后用于分页器展示
  total: 0,
})

const handleForm = reactive({
  // 要处理的反馈 id
  id: undefined as IdValue | undefined,
  // 处理后的反馈状态，handled 表示已处理，rejected 表示驳回
  status: 'handled' as 'handled' | 'rejected',
  // 处理备注，说明处理结果或驳回原因
  remark: '',
})

const batchHandleForm = reactive({
  // 批量处理后的反馈状态
  status: 'handled' as 'handled' | 'rejected',
  // 批量处理备注
  remark: '',
})

const selectedFeedbacks = computed(() =>
  feedbackList.value.filter((item) => selectedRowKeys.value.some((id) => String(id) === String(item.id))),
)
const selectedFeedbackTitles = computed(() => selectedFeedbacks.value.map((item) => item.title).join('、'))

const canHandle = (record: UserFeedback) => {
  return isCurrentSuperAdmin() || record.status === 'open'
}

const rowSelection = reactive({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: IdValue[]) => {
    selectedRowKeys.value = keys
    rowSelection.selectedRowKeys = keys
  },
  getCheckboxProps: (record: UserFeedback) => ({
    disabled: !canHandle(record),
  }),
})

// 分页查询反馈列表
const loadData = async () => {
  loading.value = true
  try {
    const resp = await http.get<CommonResp<PageResp<UserFeedback>>>('/feedback/admin/page', {
      params: {
        // 反馈状态筛选
        status: reviewOnly ? 'open' : status.value || undefined,
        // 反馈标题或内容关键词，后端做模糊查询
        keyword: keyword.value.trim() || undefined,
        // 当前页码
        current: pagination.current,
        // 每页条数
        pageSize: pagination.pageSize,
      },
    })
    feedbackList.value = resp.data.content?.records || []
    pagination.total = Number(resp.data.content?.total || 0)
    const currentPageIds = feedbackList.value.map((item) => String(item.id))
    selectedRowKeys.value = selectedRowKeys.value.filter((id) => {
      const currentFeedback = feedbackList.value.find((item) => String(item.id) === String(id))
      return currentPageIds.includes(String(id)) && !!currentFeedback && canHandle(currentFeedback)
    })
    rowSelection.selectedRowKeys = selectedRowKeys.value
  } finally {
    loading.value = false
  }
}

// 打开处理反馈弹窗
const openHandle = (record: UserFeedback, nextStatus: 'handled' | 'rejected') => {
  handleForm.id = record.id
  handleForm.status = nextStatus
  handleForm.remark = ''
  handleOpen.value = true
}

// 提交处理结果
const submitHandle = async () => {
  if (!handleForm.remark.trim()) {
    message.warning('请填写处理原因')
    return
  }
  handleForm.remark = handleForm.remark.trim()
  const resp = await http.post<CommonResp<null>>('/feedback/admin/handle', handleForm)
  if (resp.data.success) {
    message.success('处理成功')
    handleOpen.value = false
    await loadData()
    if (currentFeedback.value?.id === handleForm.id) {
      await loadReplies(handleForm.id)
    }
  }
}

const openBatchHandle = (nextStatus: 'handled' | 'rejected') => {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要处理的反馈')
    return
  }
  if (selectedFeedbacks.value.some((item) => !canHandle(item))) {
    message.warning('选中的反馈里存在已处理反馈，请刷新后重新选择')
    return
  }
  batchHandleForm.status = nextStatus
  batchHandleForm.remark = ''
  batchHandleOpen.value = true
}

const submitBatchHandle = async () => {
  if (!batchHandleForm.remark.trim()) {
    message.warning('请填写处理原因')
    return
  }
  const resp = await http.post<CommonResp<null>>('/feedback/admin/batchHandle', {
    // 批量处理的反馈 id 列表
    ids: selectedRowKeys.value,
    // 批量处理后的目标状态
    status: batchHandleForm.status,
    // 批量处理原因
    remark: batchHandleForm.remark.trim(),
  })
  if (resp.data.success) {
    message.success('批量处理成功')
    batchHandleOpen.value = false
    selectedRowKeys.value = []
    rowSelection.selectedRowKeys = []
    await loadData()
  }
}

// 打开反馈回复对话
const openReply = async (record: UserFeedback) => {
  currentFeedback.value = record
  replyContent.value = ''
  replyOpen.value = true
  await loadReplies(record.id)
}

// 查询回复列表
const loadReplies = async (feedbackId?: IdValue) => {
  if (!feedbackId) {
    replies.value = []
    return
  }
  const resp = await http.get<CommonResp<FeedbackReply[]>>(`/feedback/reply/list/${feedbackId}`)
  replies.value = resp.data.content || []
}

// 发送管理员回复
const submitReply = async () => {
  if (!currentFeedback.value?.id) {
    return
  }
  if (!replyContent.value.trim()) {
    message.warning('请填写回复内容')
    return
  }
  const resp = await http.post<CommonResp<null>>('/feedback/admin/reply', {
    // 要回复的反馈 id
    feedbackId: currentFeedback.value.id,
    // 管理员回复内容
    content: replyContent.value.trim(),
  })
  if (resp.data.success) {
    message.success('回复成功')
    replyContent.value = ''
    await loadReplies(currentFeedback.value.id)
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

const statusText = (value?: string) => {
  if (value === 'open') return '待处理'
  if (value === 'handled') return '已处理'
  if (value === 'rejected') return '已驳回'
  return value || '-'
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-typography-title :level="3">{{ pageTitle }}</a-typography-title>
      <div class="toolbar-left">
        <a-select v-if="!reviewOnly" v-model:value="status" allow-clear placeholder="反馈状态" style="width: 140px" @change="search">
          <a-select-option value="open">待处理</a-select-option>
          <a-select-option value="handled">已处理</a-select-option>
          <a-select-option value="rejected">已驳回</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按标题或内容模糊搜索"
          style="width: 260px"
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
        <a-button :disabled="!selectedRowKeys.length" type="primary" @click="openBatchHandle('handled')">
          <CheckOutlined />
          批量已处理
        </a-button>
        <a-button :disabled="!selectedRowKeys.length" danger @click="openBatchHandle('rejected')">
          <StopOutlined />
          批量驳回
        </a-button>
      </div>
    </div>

    <div class="panel">
      <div class="batch-tip">已选择 {{ selectedRowKeys.length }} 条反馈</div>
      <a-table
        :data-source="feedbackList"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        bordered
        :pagination="pagination"
        @change="handleTableChange"
      >
        <a-table-column title="提交人" data-index="userName" width="120" />
        <a-table-column title="标题" data-index="title" width="180" />
        <a-table-column title="内容" data-index="content" />
        <a-table-column title="状态" width="100">
          <template #default="{ record }">
            <a-tag>{{ statusText(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="处理备注" data-index="handleRemark" width="180" />
        <a-table-column title="操作" width="280">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" @click="openReply(record)">
                <MessageOutlined />
                回复
              </a-button>
              <a-button size="small" type="primary" :disabled="!canHandle(record)" @click="openHandle(record, 'handled')">
                <CheckOutlined />
                已处理
              </a-button>
              <a-button size="small" danger :disabled="!canHandle(record)" @click="openHandle(record, 'rejected')">
                <StopOutlined />
                驳回
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <a-modal
      v-model:open="handleOpen"
      :title="handleForm.status === 'handled' ? '确认标记为已处理' : '确认驳回反馈'"
      width="560px"
      @ok="submitHandle"
    >
      <a-form layout="vertical">
        <a-form-item label="处理原因" required>
          <a-textarea
            v-model:value="handleForm.remark"
            :rows="4"
            maxlength="500"
            show-count
            placeholder="请填写处理或驳回的原因，用户会在通知和反馈对话里看到这段内容。"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="batchHandleOpen"
      :title="batchHandleForm.status === 'handled' ? '批量标记为已处理' : '批量驳回反馈'"
      width="620px"
      @ok="submitBatchHandle"
    >
      <a-alert
        type="warning"
        show-icon
        :message="`将批量处理 ${selectedRowKeys.length} 条反馈，系统会给每条反馈写入处理结果并通知对应用户。`"
      />
      <div class="batch-feedbacks">
        选中反馈：{{ selectedFeedbackTitles || '无' }}
      </div>
      <a-form layout="vertical">
        <a-form-item label="处理原因" required>
          <a-textarea
            v-model:value="batchHandleForm.remark"
            :rows="4"
            maxlength="500"
            show-count
            placeholder="请填写批量处理原因，用户会在通知和反馈对话里看到这段内容。"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="replyOpen" title="反馈回复对话" width="720px" @ok="submitReply">
      <a-typography-title :level="5">{{ currentFeedback?.title }}</a-typography-title>
      <div class="reply-list">
        <a-empty v-if="!replies.length" description="暂无回复" />
        <div
          v-for="item in replies"
          :key="item.id"
          class="reply-item"
          :class="{ admin: item.replyType === 'admin' }"
        >
          <div class="reply-meta">
            <strong>{{ item.userName || '用户' }}</strong>
            <a-tag :color="item.replyType === 'admin' ? 'blue' : 'default'">
              {{ item.replyType === 'admin' ? '管理员' : '用户' }}
            </a-tag>
            <span>{{ item.createTime }}</span>
          </div>
          <div class="reply-content">{{ item.content }}</div>
        </div>
      </div>
      <a-textarea
        v-model:value="replyContent"
        :rows="4"
        maxlength="1000"
        show-count
        placeholder="填写管理员回复内容"
      />
    </a-modal>
  </div>
</template>

<style scoped>
.batch-tip {
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
}

.batch-feedbacks {
  margin: 12px 0;
  color: #334155;
  line-height: 1.7;
  word-break: break-all;
}

.reply-list {
  display: grid;
  max-height: 360px;
  gap: 10px;
  padding-right: 4px;
  margin-bottom: 14px;
  overflow-y: auto;
}

.reply-item {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.reply-item.admin {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.reply-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
  color: #64748b;
}

.reply-content {
  color: #111827;
  line-height: 1.8;
}
</style>
