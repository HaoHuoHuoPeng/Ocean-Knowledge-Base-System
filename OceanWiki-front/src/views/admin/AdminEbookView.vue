<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责电子书管理页面的数据请求、条件分页查询、交互逻辑和界面展示。
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
  StopOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import type { Category, CommonResp, Ebook, IdValue, ListByPageResp } from '@/types'
import { hasPermission } from '@/utils/auth'
import { arrayToTree, findNameById } from '@/utils/tree'

const ebooks = ref<Ebook[]>([])
const categories = ref<Category[]>([])
const categoryTree = ref<Category[]>([])
const loading = ref(false)
const modalOpen = ref(false)
const workflowModalOpen = ref(false)
const coverUploading = ref(false)
const coverInputType = ref<'url' | 'upload'>('url')
const keyword = ref('')
const searchCategoryIds = ref<IdValue[]>([])
const status = ref<string>()
const workflowAction = ref<'approve' | 'reject' | 'offline'>('approve')
const workflowTarget = ref<Ebook | null>(null)
const workflowRemark = ref('')
const canManageEbook = hasPermission('ebook:manage')
const canReviewEbook = hasPermission('ebook:review')
const reviewOnly = canReviewEbook && !canManageEbook
const pageTitle = computed(() => (reviewOnly ? '电子书审核' : '海洋生物电子书管理'))
const pagination = reactive({
  // 当前页码，传给后端做分页查询
  current: 1,
  // 每页条数，传给后端限制返回数量
  pageSize: 10,
  // 数据总数，后端返回后用于分页器展示
  total: 0,
})

const ebook = ref<Ebook>({
  // 电子书名称
  name: '',
  // 最终所属分类 id，多级分类时取最后一级
  categoryId: undefined,
  // 一级分类 id，兼容旧字段
  category1Id: undefined,
  // 二级分类 id，兼容旧字段
  category2Id: undefined,
  // 电子书简介
  description: '',
  // 封面地址，可以手动填 URL，也可以上传后由后端返回
  cover: '',
  // 电子书状态，新增默认草稿
  status: 'draft',
  // 下架原因，状态为已下架时填写
  offlineReason: '',
})

// 级联选择器用数组保存完整分类路径，最后一个值就是电子书实际所属分类
const categoryIds = ref<IdValue[]>([])

const columns = [
  { title: '名称', dataIndex: 'name' },
  { title: '所属分类', dataIndex: 'categoryId' },
  { title: '文档数', dataIndex: 'docCount' },
  { title: '阅读数', dataIndex: 'viewCount' },
  { title: '点赞数', dataIndex: 'voteCount' },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作', dataIndex: 'action', width: 360 },
]

const cascaderOptions = computed(() => categoryTree.value)
const workflowTitle = computed(() => {
  if (workflowAction.value === 'approve') return '审核通过'
  if (workflowAction.value === 'reject') return '审核驳回'
  return '下架电子书'
})

const loadCategories = async () => {
  const resp = await http.get<CommonResp<Category[]>>('/category/all')
  categories.value = resp.data.content || []
  categoryTree.value = arrayToTree(categories.value)
}

// 按条件分页查询电子书
// 当前支持名称模糊查询、任意层级分类、状态组合查询
const loadEbooks = async () => {
  loading.value = true
  try {
    const resp = await http.get<CommonResp<ListByPageResp<Ebook>>>('/ebook/listByPage', {
      params: {
        // 电子书名称关键词，后端做模糊查询
        name: keyword.value.trim() || undefined,
        // 分类 id，后端会按当前分类和子分类过滤
        categoryId: getLastCategoryId(searchCategoryIds.value),
        // 电子书状态
        status: reviewOnly ? 'pending' : status.value || undefined,
        // 当前页码
        page: pagination.current,
        // 每页条数
        size: pagination.pageSize,
      },
    })
    ebooks.value = resp.data.content?.list || []
    pagination.total = Number(resp.data.content?.total || 0)
  } finally {
    loading.value = false
  }
}

const resetSearch = async () => {
  keyword.value = ''
  searchCategoryIds.value = []
  status.value = reviewOnly ? 'pending' : undefined
  pagination.current = 1
  await loadEbooks()
}

const search = () => {
  pagination.current = 1
  loadEbooks()
}

// 分类筛选可以选任意层级；清空分类时要立即恢复查询全部分类
const handleSearchCategoryChange = (value?: IdValue[]) => {
  searchCategoryIds.value = Array.isArray(value) ? value : []
  search()
}

const handleTableChange = (page: { current?: number; pageSize?: number }) => {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  loadEbooks()
}

const add = () => {
  ebook.value = {
    name: '',
    categoryId: undefined,
    category1Id: undefined,
    category2Id: undefined,
    description: '',
    cover: '',
    status: 'draft',
    offlineReason: '',
  }
  categoryIds.value = []
  coverInputType.value = 'url'
  modalOpen.value = true
}

const edit = (record: Ebook) => {
  // 展开赋值，避免弹窗里直接改表格原对象
  ebook.value = { ...record }
  categoryIds.value = findCategoryPath(record.categoryId || record.category2Id || record.category1Id)
  coverInputType.value = 'url'
  modalOpen.value = true
}

const canSubmitReview = (record: Ebook) => {
  return canManageEbook && ['draft', 'rejected', 'offline'].includes(record.status || 'draft')
}

const canOffline = (record: Ebook) => {
  return canManageEbook && record.status === 'published'
}

const canAudit = (record: Ebook) => {
  return canReviewEbook && record.status === 'pending'
}

const save = async () => {
  // 任意层级分类保存最后一级，后端会自动兼容旧的 category1Id/category2Id 字段
  ebook.value.categoryId = getLastCategoryId(categoryIds.value)
  ebook.value.category1Id = categoryIds.value[0]
  ebook.value.category2Id = categoryIds.value[1]

  const resp = await http.post<CommonResp<null>>('/ebook/save', ebook.value)
  if (resp.data.success) {
    message.success('保存成功')
    modalOpen.value = false
    await loadEbooks()
  }
}

const getLastCategoryId = (ids?: IdValue[] | null) => {
  return ids?.length ? ids[ids.length - 1] : undefined
}

const findCategoryPath = (categoryId?: IdValue): IdValue[] => {
  if (!categoryId) {
    return []
  }
  const categoryMap = new Map(categories.value.map((item) => [String(item.id), item]))
  const path: IdValue[] = []
  let currentId: IdValue | undefined = categoryId
  while (currentId && categoryMap.has(String(currentId))) {
    const current = categoryMap.get(String(currentId))
    path.unshift(currentId)
    currentId = current?.parent === 0 || String(current?.parent) === '0' ? undefined : current?.parent
  }
  return path
}

const statusText = (value?: string) => {
  if (value === 'draft') return '草稿'
  if (value === 'pending') return '待审核'
  if (value === 'published') return '已发布'
  if (value === 'rejected') return '已驳回'
  if (value === 'offline') return '已下架'
  return value || '草稿'
}

const statusColor = (value?: string) => {
  if (value === 'published') return 'green'
  if (value === 'pending') return 'orange'
  if (value === 'rejected') return 'red'
  if (value === 'offline') return 'red'
  return 'default'
}

const submitReview = (record: Ebook) => {
  Modal.confirm({
    title: `确认提交「${record.name}」审核吗？`,
    content: '提交后状态会变成“待审核”，需要内容审核员或超级管理员审核通过后才会对普通用户展示。',
    onOk: async () => {
      const resp = await http.post<CommonResp<null>>(`/ebook/submitReview/${record.id}`)
      if (resp.data.success) {
        message.success(resp.data.message || '提交审核成功')
        await loadEbooks()
      }
    },
  })
}

const openWorkflowModal = (record: Ebook, action: 'approve' | 'reject' | 'offline') => {
  workflowTarget.value = record
  workflowAction.value = action
  workflowRemark.value = ''
  workflowModalOpen.value = true
}

const submitWorkflow = async () => {
  if (!workflowTarget.value?.id) {
    return
  }
  if ((workflowAction.value === 'reject' || workflowAction.value === 'offline') && !workflowRemark.value.trim()) {
    message.warning(workflowAction.value === 'reject' ? '请填写驳回原因' : '请填写下架原因')
    return
  }

  const payload = {
    id: workflowTarget.value.id,
    status: workflowAction.value === 'reject' ? 'rejected' : 'published',
    remark: workflowRemark.value.trim(),
  }
  const url = workflowAction.value === 'offline' ? '/ebook/offline' : '/ebook/review'
  const resp = await http.post<CommonResp<null>>(url, payload)
  if (resp.data.success) {
    message.success(resp.data.message || '处理成功')
    workflowModalOpen.value = false
    await loadEbooks()
  }
}

const exportData = () => {
  const params = new URLSearchParams()
  if (keyword.value.trim()) {
    params.set('name', keyword.value.trim())
  }
  const categoryId = getLastCategoryId(searchCategoryIds.value)
  if (categoryId) {
    params.set('categoryId', String(categoryId))
  }
  if (status.value) {
    params.set('status', status.value)
  }
  window.open(`/ebook/export?${params.toString()}`, '_blank')
}

const remove = (record: Ebook) => {
  Modal.confirm({
    title: `确认删除「${record.name}」吗？`,
    content: '如果这本电子书下面还有文档，或者已经产生阅读、收藏、评论数据，会阻止删除。建议将电子书状态设置为“已下架”。',
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/ebook/delete/${record.id}`)
      if (resp.data.success) {
        message.success('删除成功')
        await loadEbooks()
      }
    },
  })
}

const showOfflineReason = (record: Ebook) => {
  Modal.info({
    title: `「${record.name}」的下架原因`,
    content: record.offlineReason || '暂无下架原因',
  })
}

const beforeCoverUpload = async (file: File) => {
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning('封面图片不能超过 5MB')
    return false
  }

  const formData = new FormData()
  // file 是传给后端的封面图片字段，后端按这个字段名接收 MultipartFile
  formData.append('file', file)
  coverUploading.value = true
  try {
    const resp = await http.post<CommonResp<{ url: string }>>('/upload/ebook-cover', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    if (resp.data.success && resp.data.content?.url) {
      ebook.value.cover = resp.data.content.url
      message.success('封面上传成功')
    }
  } finally {
    coverUploading.value = false
  }

  // 返回 false 表示不让 a-upload 自动上传，由上面的代码手动控制上传流程
  return false
}

onMounted(async () => {
  if (reviewOnly) {
    status.value = 'pending'
  }
  await loadCategories()
  await loadEbooks()
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-typography-title :level="3">{{ pageTitle }}</a-typography-title>
      <div class="toolbar-left">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按名称模糊搜索"
          style="width: 240px"
          @search="search"
        >
          <template #enterButton>
            <a-button>
              <SearchOutlined />
              搜索
            </a-button>
          </template>
        </a-input-search>
        <a-cascader
          v-model:value="searchCategoryIds"
          allow-clear
          change-on-select
          :options="cascaderOptions"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          placeholder="按分类筛选"
          style="width: 240px"
          @change="handleSearchCategoryChange"
        />
        <a-select v-if="!reviewOnly" v-model:value="status" allow-clear placeholder="发布状态" style="width: 130px" @change="search">
          <a-select-option value="draft">草稿</a-select-option>
          <a-select-option value="pending">待审核</a-select-option>
          <a-select-option value="published">已发布</a-select-option>
          <a-select-option value="rejected">已驳回</a-select-option>
          <a-select-option value="offline">已下架</a-select-option>
        </a-select>
        <a-button @click="resetSearch">重置</a-button>
        <a-button @click="loadEbooks">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button v-if="canManageEbook" @click="exportData">
          <DownloadOutlined />
          导出
        </a-button>
        <a-button v-if="canManageEbook" type="primary" @click="add">
          <PlusOutlined />
          新增
        </a-button>
      </div>
    </div>

    <div class="panel">
      <a-table
        :data-source="ebooks"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        bordered
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'categoryId'">
            {{ findNameById(categoryTree, record.categoryId || record.category2Id || record.category1Id) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-space direction="vertical" size="small">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              <a-tooltip v-if="record.status === 'rejected' && record.reviewRemark" :title="record.reviewRemark">
                <span class="status-note">审核备注</span>
              </a-tooltip>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button v-if="canManageEbook" size="small" @click="edit(record as Ebook)">
                <EditOutlined />
                编辑
              </a-button>
              <a-button v-if="canSubmitReview(record as Ebook)" size="small" type="primary" ghost @click="submitReview(record as Ebook)">
                <SendOutlined />
                提交审核
              </a-button>
              <a-button v-if="canAudit(record as Ebook)" size="small" type="primary" @click="openWorkflowModal(record as Ebook, 'approve')">
                <CheckOutlined />
                通过
              </a-button>
              <a-button v-if="canAudit(record as Ebook)" size="small" danger ghost @click="openWorkflowModal(record as Ebook, 'reject')">
                <CloseOutlined />
                驳回
              </a-button>
              <a-button v-if="canOffline(record as Ebook)" size="small" danger ghost @click="openWorkflowModal(record as Ebook, 'offline')">
                <StopOutlined />
                下架
              </a-button>
              <a-button v-if="canManageEbook" size="small" danger @click="remove(record as Ebook)">
                <DeleteOutlined />
                删除
              </a-button>
              <a-button
                v-if="canManageEbook && record.status === 'offline' && record.offlineReason"
                size="small"
                @click="showOfflineReason(record as Ebook)"
              >
                <InfoCircleOutlined />
                下架原因
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="modalOpen" title="电子书表单" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="名称">
          <a-input v-model:value="ebook.name" placeholder="例如：虎鲸" />
        </a-form-item>
        <a-form-item label="分类">
          <a-cascader
            v-model:value="categoryIds"
            :options="cascaderOptions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择分类"
          />
        </a-form-item>
        <a-form-item label="封面图片">
          <a-radio-group v-model:value="coverInputType" button-style="solid" class="cover-type">
            <a-radio-button value="url">图片 URL</a-radio-button>
            <a-radio-button value="upload">本地上传</a-radio-button>
          </a-radio-group>

          <div v-if="coverInputType === 'url'" class="cover-input-row">
            <a-input v-model:value="ebook.cover" placeholder="请输入图片 URL，例如 http://..." />
          </div>

          <div v-else class="cover-input-row">
            <a-upload
              :before-upload="beforeCoverUpload"
              :max-count="1"
              accept="image/*"
              list-type="picture"
              :show-upload-list="false"
            >
              <a-button :loading="coverUploading">
                <UploadOutlined />
                选择本地图片
              </a-button>
            </a-upload>
<!--            <div class="muted">上传成功后，系统会自动把图片地址填到封面字段里。</div>-->
          </div>

          <div v-if="ebook.cover" class="cover-preview">
            <img :src="ebook.cover" alt="电子书封面预览" />
            <div class="cover-url">{{ ebook.cover }}</div>
          </div>
        </a-form-item>
        <a-form-item label="当前状态">
          <a-tag :color="statusColor(ebook.status)">{{ statusText(ebook.status) }}</a-tag>
          <span class="form-help">状态不能在表单里直接改，需要通过提交审核、审核通过、审核驳回、下架这些流程操作。</span>
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="ebook.description" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="workflowModalOpen" :title="workflowTitle" @ok="submitWorkflow">
      <a-form layout="vertical">
        <a-form-item label="电子书">
          <a-input :value="workflowTarget?.name" disabled />
        </a-form-item>
        <a-form-item :label="workflowAction === 'offline' ? '下架原因' : '审核说明'">
          <a-textarea
            v-model:value="workflowRemark"
            :rows="4"
            maxlength="300"
            show-count
            :placeholder="workflowAction === 'approve' ? '审核通过可以不填说明' : '请写清楚原因，方便编辑人员后续修改'"
          />
        </a-form-item>
      </a-form>
    </a-modal>

  </div>
</template>

<style scoped>
.cover-type {
  margin-bottom: 10px;
}

.cover-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.cover-preview {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.cover-preview img {
  width: 96px;
  height: 72px;
  object-fit: cover;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.cover-url {
  min-width: 0;
  color: #64748b;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.status-note {
  color: #64748b;
  font-size: 12px;
  cursor: help;
}

.form-help {
  display: inline-block;
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}
</style>
