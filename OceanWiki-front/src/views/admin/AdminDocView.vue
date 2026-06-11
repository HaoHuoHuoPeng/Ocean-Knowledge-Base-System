<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责文档页面的数据请求、交互逻辑和界面展示。
import { computed, h, onMounted, ref } from 'vue'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  HistoryOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import WangEditor from '@/components/WangEditor.vue'
import type { CommonResp, Doc, DocVersion, Ebook } from '@/types'
import { hasPermission } from '@/utils/auth'
import { normalizeEditorHtml } from '@/utils/html'
import { arrayToTree } from '@/utils/tree'

interface DocRow extends Omit<Doc, 'children'> {
  // 页面展示用序号，只显示在表格里，不保存到数据库
  displayIndex?: string
  // 父文档只是目录时加粗显示，但它仍然是真实 doc 记录，可以编辑
  hasChildren?: boolean
  children?: DocRow[]
}

const docs = ref<Doc[]>([])
const displayDocs = ref<DocRow[]>([])
const ebooks = ref<Ebook[]>([])
const modalOpen = ref(false)
const versionOpen = ref(false)
const editLoading = ref(false)
const keyword = ref('')
const versions = ref<DocVersion[]>([])
const currentVersionDoc = ref<Doc>()

// 是否拥有文档日常管理权限，有这个权限的人可以新增、编辑、删除和处理版本
const canManageDoc = computed(() => hasPermission('doc:manage'))
// 是否拥有文档投稿审核权限，有这个权限的人可以审核普通用户提交的待审核文档
const canReviewDoc = computed(() => hasPermission('doc:review'))
// 纯审核员只做审核，不开放文档编辑维护能力
const reviewOnly = computed(() => canReviewDoc.value && !canManageDoc.value)
// 同一个页面根据权限展示不同标题，避免内容审核员看到“文档管理”误以为自己要维护目录
const pageTitle = computed(() => (reviewOnly.value ? '文档审核' : '海洋生物文档管理'))
const modalTitle = computed(() => (reviewOnly.value ? '查看投稿文档' : '文档编辑'))
const modalFooter = computed(() => (reviewOnly.value ? null : undefined))
const statusOptions = computed(() => {
  const options = [
    { value: 'draft', label: '草稿' },
    { value: 'pending', label: '待审核' },
    { value: 'published', label: '已发布' },
  ]

  // 已驳回和已下架属于处理结果，不应该在新增文档时让管理员主动选择。
  // 如果编辑的是历史上已经处于这些状态的文档，只保留当前状态用于正常回显。
  if (doc.value.id && doc.value.status === 'rejected') {
    options.push({ value: 'rejected', label: '已驳回' })
  }
  if (doc.value.id && doc.value.status === 'offline') {
    options.push({ value: 'offline', label: '已下架' })
  }

  return options
})

const doc = ref<Doc>({
  // 所属电子书 id
  ebookId: undefined,
  // 父文档 id，0 表示一级文档
  parent: 0,
  // 文档标题
  name: '',
  // 排序值，后端会按同级目录自动处理
  sort: 0,
  // 文档状态，后台新增默认草稿
  status: 'draft',
  // 文档正文，富文本编辑器生成的 HTML 内容
  content: '',
})

const columns = [
  { title: '文档名称', dataIndex: 'name' },
  { title: '电子书ID', dataIndex: 'ebookId', width: 110 },
  { title: '父文档ID', dataIndex: 'parent', width: 110 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '阅读', dataIndex: 'viewCount', width: 90 },
  { title: '点赞', dataIndex: 'voteCount', width: 90 },
  { title: '操作', dataIndex: 'action', width: 250 },
]

// 生成文档目录序号
// 这里只展示 doc 表里的真实文档，不额外插入电子书行
// 顶级父文档显示 1、2，例如 1 虎鲸、2 海豚
// 子文档显示 1.1、1.2，例如 1.1 虎鲸简介、1.2 虎鲸习性
const buildDisplayDocs = (list: Doc[]): DocRow[] => {
  const walk = (treeList: Doc[], parentIndex = ''): DocRow[] => {
    return treeList.map((item, index) => {
      const displayIndex = parentIndex ? `${parentIndex}.${index + 1}` : String(index + 1)
      const children = walk(item.children || [], displayIndex)

      return {
        ...item,
        displayIndex,
        hasChildren: children.length > 0,
        children,
      }
    })
  }

  return walk(arrayToTree(list))
}

const buildParentDocOptions = (list: Doc[]): ParentDocOption[] => {
  const selectedEbookId = doc.value.ebookId
  const sameEbookDocs = selectedEbookId
    ? list.filter((item) => String(item.ebookId) === String(selectedEbookId))
    : list

  const walk = (treeList: Doc[], parentIndex = ''): ParentDocOption[] => {
    return treeList.map((item, index) => {
      const displayIndex = parentIndex ? `${parentIndex}.${index + 1}` : String(index + 1)
      const disabled = doc.value.id != null && String(item.id) === String(doc.value.id)
      return {
        title: `${displayIndex} ${item.name}`,
        value: item.id || 0,
        key: item.id || `${displayIndex}-${item.name}`,
        disabled,
        children: walk(item.children || [], displayIndex),
      }
    })
  }

  return [
    {
      title: '无父文档',
      value: 0,
      key: 0,
    },
    ...walk(arrayToTree(sameEbookDocs)),
  ]
}

const parentDocOptions = computed(() => buildParentDocOptions(docs.value))

const loadDocs = async () => {
  const resp = await http.get<CommonResp<Doc[]>>('/doc/all', {
    params: {
      // 文档关键词，后端按文档名称模糊查询
      keyword: keyword.value.trim() || undefined,
    },
  })
  docs.value = resp.data.content || []
  displayDocs.value = buildDisplayDocs(docs.value)
}

const resetSearch = async () => {
  keyword.value = ''
  await loadDocs()
}

const loadEbooks = async () => {
  const resp = await http.get<CommonResp<{ list?: Ebook[] }>>('/ebook/listByPage', {
    params: {
      // 查询第 1 页电子书
      page: 1,
      // 最多取 100 条，用于文档所属电子书下拉框
      size: 100,
    },
  })
  ebooks.value = resp.data.content?.list || []
}

const add = () => {
  editLoading.value = false
  doc.value = {
    ebookId: ebooks.value[0]?.id,
    parent: 0,
    name: '',
    sort: 0,
    status: 'draft',
    content: '',
  }
  modalOpen.value = true
}

const edit = async (record: Doc) => {
  doc.value = { ...record, content: '' }
  modalOpen.value = true
  editLoading.value = true

  // 编辑时先把正文查出来放进文本框
  try {
    if (record.id) {
      const resp = await http.get<CommonResp<{ content?: string }>>(`/content/${record.id}`)
      doc.value.content = normalizeEditorHtml(resp.data.content?.content)
    }
  } finally {
    editLoading.value = false
  }
}

const reviewDoc = (record: Doc, status: 'published' | 'rejected') => {
  const passed = status === 'published'
  const remark = ref('')

  Modal.confirm({
    title: passed ? `确认通过“${record.name}”吗？` : `确认驳回“${record.name}”吗？`,
    content: passed
      ? '通过后，这篇投稿会进入对应电子书的阅读目录。'
      : h('div', [
          h('div', { class: 'review-reason-tip' }, '请填写驳回原因，投稿人会在通知或我的投稿里看到。'),
          h('textarea', {
            class: 'review-reason-input',
            rows: 4,
            maxlength: 300,
            placeholder: '例如：内容不完整、格式不符合要求、资料来源不清晰',
            onInput: (event: Event) => {
              remark.value = (event.target as HTMLTextAreaElement).value
            },
          }),
        ]),
    okText: passed ? '通过' : '驳回',
    okType: 'primary',
    okButtonProps: passed ? undefined : { danger: true },
    cancelText: '取消',
    onOk: async () => {
      if (!passed && !remark.value.trim()) {
        message.warning('驳回时必须填写原因')
        return Promise.reject()
      }
      const resp = await http.post<CommonResp<null>>('/doc/review', {
        id: record.id,
        status,
        remark: remark.value.trim() || undefined,
      })
      if (resp.data.success) {
        message.success(passed ? '已通过投稿' : '已驳回投稿')
        await loadDocs()
      }
    },
  })
}

interface ParentDocOption {
  // TreeSelect 展示文字，带层级序号，方便查找父文档
  title: string
  // TreeSelect 真实值，对应 doc.id
  value: string | number
  // TreeSelect 节点 key
  key: string | number
  // 编辑当前文档时，不能把自己选成自己的父文档
  disabled?: boolean
  // 子文档选项
  children?: ParentDocOption[]
}

const save = async () => {
  if (!doc.value.ebookId) {
    message.warning('请选择所属电子书')
    return
  }
  if (!doc.value.name.trim()) {
    message.warning('请填写文档名称')
    return
  }
  const resp = await http.post<CommonResp<null>>('/doc/save', {
    ...doc.value,
    content: normalizeEditorHtml(doc.value.content),
  })
  if (resp.data.success) {
    message.success('保存成功')
    modalOpen.value = false
    await loadDocs()
  }
}

const remove = (record: Doc) => {
  Modal.confirm({
    title: `确认删除“${record.name}”吗？`,
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/doc/delete/${record.id}`)
      if (resp.data.success) {
        message.success('删除成功')
        await loadDocs()
      }
    },
  })
}

const openVersions = async (record: Doc) => {
  currentVersionDoc.value = record
  const resp = await http.get<CommonResp<DocVersion[]>>(`/doc/versions/${record.id}`)
  versions.value = resp.data.content || []
  versionOpen.value = true
}

const rollback = (record: DocVersion) => {
  Modal.confirm({
    title: `确认回滚到 v${record.versionNo} 吗？`,
    content: '回滚前会自动备份当前内容，方便后续继续恢复。',
    onOk: async () => {
      const resp = await http.post<CommonResp<null>>(`/doc/rollback/${record.id}`)
      if (resp.data.success) {
        message.success('回滚成功')
        versionOpen.value = false
        await loadDocs()
      }
    },
  })
}

const removeVersion = (record: DocVersion) => {
  Modal.confirm({
    title: `确认删除 v${record.versionNo} 吗？`,
    content: '只会删除这条历史版本记录，不会影响当前文档内容。',
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/doc/version/${record.id}`)
      if (resp.data.success) {
        message.success('版本删除成功')
        if (currentVersionDoc.value?.id) {
          const versionResp = await http.get<CommonResp<DocVersion[]>>(`/doc/versions/${currentVersionDoc.value.id}`)
          versions.value = versionResp.data.content || []
        }
      }
    },
  })
}

const statusText = (value?: string) => {
  if (value === 'draft') return '草稿'
  if (value === 'pending') return '待审核'
  if (value === 'published') return '已发布'
  if (value === 'rejected') return '已驳回'
  if (value === 'offline') return '已下架'
  return value || '-'
}

const statusColor = (value?: string) => {
  if (value === 'published') return 'green'
  if (value === 'pending') return 'orange'
  if (value === 'rejected') return 'red'
  if (value === 'offline') return 'default'
  return 'blue'
}

onMounted(async () => {
  await loadEbooks()
  await loadDocs()
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
          placeholder="按文档名称模糊搜索"
          style="width: 260px"
          @search="loadDocs"
        >
          <template #enterButton>
            <a-button>
              <SearchOutlined />
              搜索
            </a-button>
          </template>
        </a-input-search>
        <a-button @click="resetSearch">重置</a-button>
        <a-button @click="loadDocs">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button v-if="!reviewOnly" type="primary" @click="add">
          <PlusOutlined />
          新增
        </a-button>
      </div>
    </div>

    <div class="panel">
      <a-table
        :data-source="displayDocs"
        :columns="columns"
        :children-column-name="'children'"
        :default-expand-all-rows="true"
        :indent-size="28"
        :pagination="false"
        row-key="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <span class="doc-name-cell" :class="{ 'parent-doc-name': record.hasChildren }">
              <span class="doc-index">{{ record.displayIndex }}</span>
              <span class="doc-name-text">{{ record.name }}</span>
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button size="small" @click="edit(record as Doc)">
                <template v-if="reviewOnly">
                  <EyeOutlined />
                  查看
                </template>
                <template v-else>
                <EditOutlined />
                编辑
                </template>
              </a-button>
              <a-button v-if="reviewOnly && record.status === 'pending'" size="small" type="primary" @click="reviewDoc(record as Doc, 'published')">
                <CheckCircleOutlined />
                通过
              </a-button>
              <a-button v-if="reviewOnly && record.status === 'pending'" size="small" danger @click="reviewDoc(record as Doc, 'rejected')">
                <CloseCircleOutlined />
                驳回
              </a-button>
              <a-button v-if="!reviewOnly" size="small" @click="openVersions(record as Doc)">
                <HistoryOutlined />
                版本
              </a-button>
              <a-button v-if="!reviewOnly" size="small" danger @click="remove(record as Doc)">
                <DeleteOutlined />
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      width="1180px"
      wrap-class-name="doc-editor-modal"
      :footer="modalFooter"
      @ok="save"
    >
      <div class="doc-editor">
        <a-form layout="vertical">
          <a-row :gutter="12">
            <a-col :span="6">
              <a-form-item label="所属电子书">
                <a-select v-model:value="doc.ebookId" placeholder="请选择电子书" :disabled="reviewOnly">
                  <a-select-option v-for="item in ebooks" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="父文档">
                <a-tree-select
                  v-model:value="doc.parent"
                  :tree-data="parentDocOptions"
                  show-search
                  tree-line
                  tree-node-filter-prop="title"
                  placeholder="请选择父文档"
                  :disabled="reviewOnly"
                />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="文档名称">
                <a-input v-model:value="doc.name" placeholder="例如：虎鲸简介" :disabled="reviewOnly" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="发布状态">
                <a-select v-model:value="doc.status" :disabled="reviewOnly">
                  <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item v-if="doc.status === 'rejected' || doc.status === 'offline'" label="审核或下架原因">
            <a-textarea v-model:value="doc.reviewRemark" :rows="3" maxlength="300" show-count :disabled="reviewOnly" />
          </a-form-item>
        </a-form>

        <a-spin :spinning="editLoading">
          <div class="editor-shell">
          <div class="editor-pane">
            <div class="editor-header">
              <div>
                <div class="editor-title">编辑框</div>
<!--                该编辑框使用的是集成富文本插件wangEditor-->
                <div class="editor-subtitle">可在下方编辑你的文档文本</div>
              </div>
            </div>
            <div class="case-paper">
              <div class="case-paper-title">{{ doc.name || '在这里给文档命名' }}</div>
              <WangEditor
                v-if="!editLoading"
                v-show="!reviewOnly"
                v-model="doc.content"
                height="430px"
                placeholder="在这里编写正文。可以直接使用工具栏插入图片、表格、引用、列表等内容。"
              />
              <div v-if="!editLoading && reviewOnly" class="review-content" v-html="doc.content || '<p>暂无正文内容</p>'"></div>
              <div v-if="editLoading" class="editor-loading-tip">正文加载中，请稍等...</div>
<!--              <div class="editor-help">
                <span>建议结构：</span>
                <span>背景说明</span>
                <span>知识点分析</span>
                <span>关键结论</span>
                <span>扩展资料</span>
              </div>-->
            </div>
          </div>
          </div>
        </a-spin>
      </div>
    </a-modal>

    <a-modal v-model:open="versionOpen" :title="`历史版本：${currentVersionDoc?.name || ''}`" width="820px" :footer="null">
      <a-table :data-source="versions" row-key="id" bordered>
        <a-table-column title="版本" data-index="versionNo" width="80">
          <template #default="{ record }">v{{ record.versionNo }}</template>
        </a-table-column>
        <a-table-column title="标题" data-index="name" />
        <a-table-column title="状态" data-index="status" width="100">
          <template #default="{ record }">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="保存时间" data-index="createTime" width="180" />
        <a-table-column title="操作" width="160">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" type="primary" @click="rollback(record)">回滚</a-button>
              <a-button size="small" danger @click="removeVersion(record)">删除</a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
      <a-empty v-if="!versions.length" description="暂无历史版本" />
    </a-modal>
  </div>
</template>

<style scoped>
.doc-index {
  display: inline-flex;
  min-width: 42px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  color: #0958d9;
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.doc-name-cell {
  display: inline-flex;
  max-width: 100%;
  gap: 10px;
  align-items: center;
}

.doc-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.parent-doc-name {
  color: #0f172a;
  font-weight: 700;
}

.doc-editor {
  max-height: 72vh;
  overflow: auto;
  padding-right: 4px;
}

.editor-shell {
  display: block;
}

.editor-pane {
  min-height: 620px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}

.editor-header {
  min-height: 68px;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.editor-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.editor-subtitle {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.case-paper {
  max-width: 860px;
  min-height: 540px;
  margin: 18px auto;
  padding: 30px 42px;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.case-paper-title {
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
  text-align: center;
}

.editor-loading-tip {
  display: flex;
  min-height: 430px;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

.editor-help {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px dashed #d9d9d9;
  color: #64748b;
  font-size: 12px;
}

.editor-help span:not(:first-child) {
  padding: 2px 8px;
  background: #f1f5f9;
  border-radius: 4px;
}

.review-content {
  min-height: 430px;
  padding: 18px 20px;
  color: #1f2937;
  line-height: 1.9;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.review-content :deep(table) {
  width: 100%;
  margin: 14px 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.review-content :deep(th),
.review-content :deep(td) {
  min-width: 72px;
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  vertical-align: top;
  word-break: break-word;
}

.review-content :deep(th) {
  background: #f8fafc;
  font-weight: 700;
}

:global(.review-reason-tip) {
  margin-bottom: 8px;
  color: #64748b;
}

:global(.review-reason-input) {
  width: 100%;
  resize: vertical;
  padding: 8px 10px;
  color: #1f2937;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  outline: none;
}

:global(.review-reason-input:focus) {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgb(5 145 255 / 10%);
}
</style>
