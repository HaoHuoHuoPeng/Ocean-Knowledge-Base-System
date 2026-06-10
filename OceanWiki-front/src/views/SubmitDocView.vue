<script setup lang="ts">
// 文件说明：这个页面负责普通用户投稿文档，投稿会先经过敏感词检查，再进入管理员审核流程。
import { computed, onMounted, ref, watch } from 'vue'
import { FileAddOutlined, ReloadOutlined, SendOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import WangEditor from '@/components/WangEditor.vue'
import type { CommonResp, Doc, Ebook, IdValue } from '@/types'
import { normalizeEditorHtml } from '@/utils/html'
import { arrayToTree } from '@/utils/tree'

const ebooks = ref<Ebook[]>([])
const docs = ref<Doc[]>([])
const submissions = ref<Doc[]>([])
const loading = ref(false)
const submitting = ref(false)

interface ParentDocOption {
  // TreeSelect 展示文字，带层级序号，方便查找父文档
  title: string
  // TreeSelect 真实值，对应 doc.id
  value: string | number
  // TreeSelect 节点 key
  key: string | number
  // 子文档选项
  children?: ParentDocOption[]
}

const form = ref<Doc>({
  // 投稿到哪一本电子书下面
  ebookId: undefined,
  // 父文档 id，0 表示作为一级文档投稿
  parent: 0,
  // 投稿文档标题
  name: '',
  // 排序值，后端会按同级目录自动处理
  sort: 0,
  // 投稿默认进入待审核状态
  status: 'pending',
  // 投稿正文，富文本编辑器生成的 HTML 内容
  content: '',
})

const buildParentDocOptions = (list: Doc[]): ParentDocOption[] => {
  const walk = (treeList: Doc[], parentIndex = ''): ParentDocOption[] => {
    return treeList.map((item, index) => {
      const displayIndex = parentIndex ? `${parentIndex}.${index + 1}` : String(index + 1)
      return {
        title: `${displayIndex} ${item.name}`,
        value: item.id || 0,
        key: item.id || `${displayIndex}-${item.name}`,
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
    ...walk(arrayToTree(list)),
  ]
}

const docTree = computed(() => buildParentDocOptions(docs.value))

const loadEbooks = async () => {
  const resp = await http.get<CommonResp<{ list?: Ebook[] }>>('/ebook/listByPage', {
    params: {
      // 查询第 1 页电子书
      page: 1,
      // 最多取 100 条，给投稿下拉框使用
      size: 100,
      // 只允许投稿到已发布电子书
      status: 'published',
    },
  })
  ebooks.value = resp.data.content?.list || []
  if (!form.value.ebookId && ebooks.value[0]?.id) {
    form.value.ebookId = ebooks.value[0].id
  }
}

const loadDocs = async () => {
  if (!form.value.ebookId) {
    docs.value = []
    return
  }
  const resp = await http.get<CommonResp<Doc[]>>(`/doc/all/${form.value.ebookId}`)
  docs.value = resp.data.content || []
}

const loadSubmissions = async () => {
  const resp = await http.get<CommonResp<Doc[]>>('/doc/my-submissions')
  submissions.value = resp.data.content || []
}

const resetForm = async () => {
  form.value = {
    ebookId: form.value.ebookId || ebooks.value[0]?.id,
    parent: 0,
    name: '',
    sort: 0,
    status: 'pending',
    content: '',
  }
}

const submit = async () => {
  if (!form.value.ebookId) {
    message.warning('请选择电子书')
    return
  }
  if (!form.value.name.trim()) {
    message.warning('请填写文档标题')
    return
  }
  if (!form.value.content?.replace(/<[^>]+>/g, '').trim()) {
    message.warning('请填写正文内容')
    return
  }

  submitting.value = true
  try {
    const resp = await http.post<CommonResp<null>>('/doc/submit', {
      ...form.value,
      content: normalizeEditorHtml(form.value.content),
    })
    if (resp.data.success) {
      message.success(resp.data.message)
      await resetForm()
      await loadSubmissions()
    }
  } finally {
    submitting.value = false
  }
}

const statusText = (status?: string) => {
  if (status === 'pending') return '待审核'
  if (status === 'published') return '已发布'
  if (status === 'rejected') return '已驳回'
  if (status === 'draft') return '草稿'
  if (status === 'offline') return '已下架'
  return status || '-'
}

const statusColor = (status?: string) => {
  if (status === 'published') return 'green'
  if (status === 'pending') return 'orange'
  if (status === 'rejected') return 'red'
  return 'default'
}

const ebookName = (id?: IdValue) => {
  return ebooks.value.find((item) => String(item.id) === String(id))?.name || '-'
}

watch(
  () => form.value.ebookId,
  async () => {
    form.value.parent = 0
    await loadDocs()
  },
)

onMounted(async () => {
  loading.value = true
  try {
    await loadEbooks()
    await loadDocs()
    await loadSubmissions()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page submit-page">
    <div class="toolbar">
      <div>
        <a-typography-title :level="3">
          <FileAddOutlined />
          发文档
        </a-typography-title>
<!--        <div class="muted">投稿会先检查敏感词，未命中后进入内容审核员或超级管理员审核。</div>-->
      </div>
      <a-button @click="loadSubmissions">
        <ReloadOutlined />
        刷新投稿状态
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="15">
          <div class="panel">
            <a-form layout="vertical">
              <a-row :gutter="12">
                <a-col :xs="24" :md="8">
                  <a-form-item label="投稿到电子书">
                    <a-select v-model:value="form.ebookId" placeholder="请选择电子书">
                      <a-select-option v-for="item in ebooks" :key="item.id" :value="item.id">
                        {{ item.name }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :md="8">
                  <a-form-item label="父文档">
                    <a-tree-select
                      v-model:value="form.parent"
                      :tree-data="docTree"
                      show-search
                      tree-line
                      tree-node-filter-prop="title"
                      placeholder="请选择父文档"
                    />
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :md="8">
                  <a-form-item label="文档标题">
                    <a-input v-model:value="form.name" placeholder="例如：虎鲸捕食行为补充资料" />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>

            <div class="submit-editor">
              <div class="paper">
                <div class="paper-title">{{ form.name || '未命名投稿' }}</div>
                <WangEditor
                  v-model="form.content"
                  height="360px"
                  placeholder="在这里写正文内容。可以直接使用工具栏插入图片、表格、引用、列表等内容。"
                />
              </div>
            </div>

            <div class="submit-actions">
              <a-button @click="resetForm">清空</a-button>
              <a-button type="primary" :loading="submitting" @click="submit">
                <SendOutlined />
                提交审核
              </a-button>
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="9">
          <div class="panel">
            <a-typography-title :level="4">我的投稿</a-typography-title>
            <a-table :data-source="submissions" row-key="id" size="small" bordered>
              <a-table-column title="标题" data-index="name" />
              <a-table-column title="电子书" width="120">
                <template #default="{ record }">{{ ebookName(record.ebookId) }}</template>
              </a-table-column>
              <a-table-column title="状态" width="90">
                <template #default="{ record }">
                  <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column title="备注" data-index="reviewRemark" />
            </a-table>
          </div>
        </a-col>
      </a-row>
    </a-spin>

  </div>
</template>

<style scoped>
.submit-page {
  max-width: 1380px;
  margin: 0 auto;
}

.submit-editor {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}

.paper {
  max-width: 820px;
  min-height: 480px;
  margin: 18px auto;
  padding: 28px 38px;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.paper-title {
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  color: #0f172a;
  font-size: 23px;
  font-weight: 700;
  text-align: center;
}

.submit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}
</style>
