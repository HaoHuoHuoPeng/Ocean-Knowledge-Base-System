<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责评论页面的数据请求、交互逻辑和界面展示。
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckOutlined, DeleteOutlined, ReloadOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp, IdValue, PageResp, UserComment } from '@/types'
import { getLoginUser, isCurrentSuperAdmin } from '@/utils/auth'

const comments = ref<UserComment[]>([])
const loading = ref(false)
const keyword = ref('')
const reviewOnly = getLoginUser()?.roleCodes?.includes('CONTENT_REVIEWER') && !isCurrentSuperAdmin()
const status = ref<string | undefined>(reviewOnly ? 'pending' : undefined)
const selectedRowKeys = ref<IdValue[]>([])
const reviewOpen = ref(false)
const batchReviewOpen = ref(false)
const pageTitle = computed(() => (reviewOnly ? '评论审核' : '评论管理'))
const pagination = reactive({
  // 当前页码，传给后端分页查询
  current: 1,
  // 每页条数，传给后端限制返回数量
  pageSize: 10,
  // 评论总数，后端返回后用于分页器展示
  total: 0,
})

const batchReviewForm = reactive({
  // 批量审核后的目标状态，published 表示通过，rejected 表示驳回
  status: 'published' as 'published' | 'rejected',
  // 批量审核原因，用户会在通知里看到
  remark: '',
})

const reviewForm = reactive({
  // 当前单条审核的评论
  comment: undefined as UserComment | undefined,
  // 单条审核后的目标状态
  status: 'published' as 'published' | 'rejected',
  // 单条审核原因，必须由管理员填写
  remark: '',
})

const selectedComments = computed(() =>
  comments.value.filter((item) => selectedRowKeys.value.some((id) => String(id) === String(item.id))),
)
const selectedCommentText = computed(() =>
  selectedComments.value.map((item) => item.content).join('、'),
)

const canReview = (record: UserComment) => {
  return isCurrentSuperAdmin() || record.status === 'pending'
}

const rowSelection = reactive({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: IdValue[]) => {
    selectedRowKeys.value = keys
    rowSelection.selectedRowKeys = keys
  },
  getCheckboxProps: (record: UserComment) => ({
    disabled: !canReview(record),
  }),
})

// 分页查询评论列表
const loadData = async () => {
  loading.value = true
  try {
    const resp = await http.get<CommonResp<PageResp<UserComment>>>('/comment/admin/page', {
      params: {
        // 评论状态筛选
        status: reviewOnly ? 'pending' : status.value || undefined,
        // 评论内容或用户关键词，后端做模糊查询
        keyword: keyword.value.trim() || undefined,
        // 当前页码
        current: pagination.current,
        // 每页条数
        pageSize: pagination.pageSize,
      },
    })
    comments.value = resp.data.content?.records || []
    pagination.total = Number(resp.data.content?.total || 0)
    const currentPageIds = comments.value.map((item) => String(item.id))
    selectedRowKeys.value = selectedRowKeys.value.filter((id) => {
      const currentComment = comments.value.find((item) => String(item.id) === String(id))
      return currentPageIds.includes(String(id)) && !!currentComment && canReview(currentComment)
    })
    rowSelection.selectedRowKeys = selectedRowKeys.value
  } finally {
    loading.value = false
  }
}

// 审核评论
const openReview = (record: UserComment, nextStatus: 'published' | 'rejected') => {
  reviewForm.comment = record
  reviewForm.status = nextStatus
  reviewForm.remark = ''
  reviewOpen.value = true
}

const submitReview = async () => {
  if (!reviewForm.comment?.id) {
    return
  }
  if (!reviewForm.remark.trim()) {
    message.warning('请填写审核原因')
    return
  }
  const resp = await http.post<CommonResp<null>>('/comment/admin/review', {
    // 要审核的评论 id
    id: reviewForm.comment.id,
    // 审核后的评论状态
    status: reviewForm.status,
    // 审核原因
    remark: reviewForm.remark.trim(),
  })
  if (resp.data.success) {
    message.success('审核成功')
    reviewOpen.value = false
    await loadData()
  }
}

const openBatchReview = (nextStatus: 'published' | 'rejected') => {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要审核的评论')
    return
  }
  if (selectedComments.value.some((item) => !canReview(item))) {
    message.warning('选中的评论里存在已处理评论，请刷新后重新选择')
    return
  }
  batchReviewForm.status = nextStatus
  batchReviewForm.remark = ''
  batchReviewOpen.value = true
}

const submitBatchReview = async () => {
  if (!batchReviewForm.remark.trim()) {
    message.warning('请填写审核原因')
    return
  }
  const resp = await http.post<CommonResp<null>>('/comment/admin/batchReview', {
    // 批量审核的评论 id 列表
    ids: selectedRowKeys.value,
    // 批量审核后的目标状态
    status: batchReviewForm.status,
    // 批量审核原因
    remark: batchReviewForm.remark.trim(),
  })
  if (resp.data.success) {
    message.success('批量审核成功')
    batchReviewOpen.value = false
    selectedRowKeys.value = []
    rowSelection.selectedRowKeys = []
    await loadData()
  }
}

const remove = (record: UserComment) => {
  if (!record.id) {
    return
  }
  Modal.confirm({
    title: '确认删除这条评论吗？',
    content: '删除后这条评论不会继续展示给用户，但后台会保留操作日志。',
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/comment/delete/${record.id}`)
      if (resp.data.success) {
        message.success('评论已删除')
        await loadData()
      }
    },
  })
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
  if (value === 'published') return '已发布'
  if (value === 'pending') return '待审核'
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
        <a-select v-if="!reviewOnly" v-model:value="status" allow-clear placeholder="评论状态" style="width: 140px" @change="search">
          <a-select-option value="pending">待审核</a-select-option>
          <a-select-option value="published">已发布</a-select-option>
          <a-select-option value="rejected">已驳回</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按评论内容模糊搜索"
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
        <a-button :disabled="!selectedRowKeys.length" type="primary" @click="openBatchReview('published')">
          <CheckOutlined />
          批量通过
        </a-button>
        <a-button :disabled="!selectedRowKeys.length" danger @click="openBatchReview('rejected')">
          <StopOutlined />
          批量驳回
        </a-button>
      </div>
    </div>

    <div class="panel">
      <div class="batch-tip">已选择 {{ selectedRowKeys.length }} 条评论</div>
      <a-table
        :data-source="comments"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        bordered
        :pagination="pagination"
        @change="handleTableChange"
      >
        <a-table-column title="评论人" data-index="userName" width="120" />
        <a-table-column title="评论对象" data-index="targetName" width="180" />
        <a-table-column title="内容" data-index="content" />
        <a-table-column title="状态" width="100">
          <template #default="{ record }">
            <a-tag>{{ statusText(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="敏感词" data-index="sensitiveHit" width="140" />
        <a-table-column title="审核备注" data-index="reviewRemark" width="180" />
        <a-table-column title="操作" width="260">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" type="primary" :disabled="!canReview(record)" @click="openReview(record, 'published')">
                <CheckOutlined />
                通过
              </a-button>
              <a-button size="small" danger :disabled="!canReview(record)" @click="openReview(record, 'rejected')">
                <StopOutlined />
                驳回
              </a-button>
              <a-button size="small" danger @click="remove(record)">
                <DeleteOutlined />
                删除
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <a-modal
      v-model:open="reviewOpen"
      :title="reviewForm.status === 'published' ? '通过评论' : '驳回评论'"
      width="560px"
      @ok="submitReview"
    >
      <a-alert
        type="info"
        show-icon
        message="请填写本次审核原因，系统会把原因写入评论审核备注并通知评论作者。"
      />
      <div class="batch-comments">
        评论内容：{{ reviewForm.comment?.content || '无' }}
      </div>
      <a-form layout="vertical">
        <a-form-item label="审核原因" required>
          <a-textarea
            v-model:value="reviewForm.remark"
            :rows="4"
            maxlength="500"
            show-count
            placeholder="请填写通过或驳回的原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="batchReviewOpen"
      :title="batchReviewForm.status === 'published' ? '批量通过评论' : '批量驳回评论'"
      width="620px"
      @ok="submitBatchReview"
    >
      <a-alert
        type="warning"
        show-icon
        :message="`将批量审核 ${selectedRowKeys.length} 条评论，系统会给每条评论写入审核结果并通知评论作者。`"
      />
      <div class="batch-comments">
        选中评论：{{ selectedCommentText || '无' }}
      </div>
      <a-form layout="vertical">
        <a-form-item label="审核原因" required>
          <a-textarea
            v-model:value="batchReviewForm.remark"
            :rows="4"
            maxlength="500"
            show-count
            placeholder="请填写批量审核原因，用户会在通知里看到这段内容。"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.batch-tip {
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
}

.batch-comments {
  margin: 12px 0;
  color: #334155;
  line-height: 1.7;
  word-break: break-all;
}
</style>
