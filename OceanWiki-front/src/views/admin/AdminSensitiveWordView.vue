<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责敏感词页面的数据请求、交互逻辑和界面展示。
import { computed, onMounted, reactive, ref } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import ExcelImportModal from '@/components/ExcelImportModal.vue'
import type { CommonResp, PageResp, SensitiveWord } from '@/types'

const words = ref<SensitiveWord[]>([])
const loading = ref(false)
const keyword = ref('')
const modalOpen = ref(false)
const importModal = ref<InstanceType<typeof ExcelImportModal>>()
const selectedRowKeys = ref<Array<string | number>>([])
const pagination = reactive({
  // 当前页码，传给后端分页查询
  current: 1,
  // 每页条数，传给后端限制返回数量
  pageSize: 10,
  // 敏感词总数，后端返回后用于分页器展示
  total: 0,
})

const selectedWords = computed(() =>
  words.value.filter((item) => selectedRowKeys.value.some((id) => String(id) === String(item.id))),
)
const selectedWordText = computed(() => selectedWords.value.map((item) => item.word).join('、'))

const rowSelection = reactive({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: Array<string | number>) => {
    selectedRowKeys.value = keys
    rowSelection.selectedRowKeys = keys
  },
})

const form = ref<SensitiveWord>({
  // 敏感词内容
  word: '',
  // 是否启用，1 表示启用，0 表示停用
  enabled: 1,
  // 备注说明
  remark: '',
})

// 分页查询敏感词
const loadData = async () => {
  loading.value = true
  try {
    const resp = await http.get<CommonResp<PageResp<SensitiveWord>>>('/sensitiveWord/page', {
      params: {
        // 敏感词或备注关键词，后端做模糊查询
        keyword: keyword.value.trim() || undefined,
        // 当前页码
        current: pagination.current,
        // 每页条数
        pageSize: pagination.pageSize,
      },
    })
    words.value = resp.data.content?.records || []
    pagination.total = Number(resp.data.content?.total || 0)
    const currentPageIds = words.value.map((item) => String(item.id))
    selectedRowKeys.value = selectedRowKeys.value.filter((id) => currentPageIds.includes(String(id)))
    rowSelection.selectedRowKeys = selectedRowKeys.value
  } finally {
    loading.value = false
  }
}

const add = () => {
  form.value = {
    word: '',
    enabled: 1,
    remark: '',
  }
  modalOpen.value = true
}

const edit = (record: SensitiveWord) => {
  form.value = { ...record }
  modalOpen.value = true
}

const save = async () => {
  const resp = await http.post<CommonResp<null>>('/sensitiveWord/save', form.value)
  if (resp.data.success) {
    message.success('保存成功')
    modalOpen.value = false
    await loadData()
  }
}

const remove = (record: SensitiveWord) => {
  Modal.confirm({
    title: `确认删除“${record.word}”吗？`,
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/sensitiveWord/delete/${record.id}`)
      if (resp.data.success) {
        message.success('删除成功')
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

const batchSetStatus = (enabled: 0 | 1) => {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要设置状态的敏感词')
    return
  }

  const actionText = enabled === 1 ? '启用' : '停用'
  Modal.confirm({
    title: `确认批量${actionText} ${selectedRowKeys.value.length} 个敏感词吗？`,
    content: `选中敏感词：${selectedWordText.value || '无'}`,
    okText: `确认${actionText}`,
    cancelText: '取消',
    onOk: async () => {
      const resp = await http.post<CommonResp<null>>('/sensitiveWord/batchStatus', {
        // 批量设置状态的敏感词 id 列表
        ids: selectedRowKeys.value,
        // 目标状态，1 表示启用，0 表示停用
        enabled,
      })
      if (resp.data.success) {
        message.success(`批量${actionText}成功`)
        selectedRowKeys.value = []
        rowSelection.selectedRowKeys = []
        await loadData()
      }
    },
  })
}

const openImport = () => {
  importModal.value?.show()
}

const afterImportSuccess = async () => {
  pagination.current = 1
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-typography-title :level="3">敏感词管理</a-typography-title>
      <div class="toolbar-left">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按敏感词或备注模糊搜索"
          style="width: 260px"
          @search="() => { pagination.current = 1; loadData() }"
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
        <a-button type="primary" @click="add">
          <PlusOutlined />
          新增
        </a-button>
        <a-button :disabled="!selectedRowKeys.length" @click="batchSetStatus(1)">批量启用</a-button>
        <a-button :disabled="!selectedRowKeys.length" @click="batchSetStatus(0)">批量停用</a-button>
        <a-button @click="openImport">
          <UploadOutlined />
          批量导入
        </a-button>
      </div>
    </div>

    <div class="panel">
      <div class="batch-tip">已选择 {{ selectedRowKeys.length }} 个敏感词</div>
      <a-table
        :data-source="words"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        bordered
        :pagination="pagination"
        @change="handleTableChange"
      >
        <a-table-column title="敏感词" data-index="word" />
        <a-table-column title="状态" width="100">
          <template #default="{ record }">
            <a-tag :color="record.enabled === 1 ? 'green' : 'default'">
              {{ record.enabled === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="备注" data-index="remark" />
        <a-table-column title="创建时间" data-index="createTime" width="190" />
        <a-table-column title="操作" width="160">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" @click="edit(record)">
                <EditOutlined />
                编辑
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

    <a-modal v-model:open="modalOpen" title="敏感词" width="520px" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="敏感词">
          <a-input v-model:value="form.word" placeholder="请输入敏感词" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.enabled">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="3" placeholder="可填写加入原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <ExcelImportModal
      ref="importModal"
      title="敏感词批量导入"
      import-url="/sensitiveWord/import"
      template-url="/sensitiveWord/get-import-template"
      template-filename="敏感词导入模板.xlsx"
      tip="模板列为：敏感词、备注。导入成功后统一设为停用，确认无误后再批量启用。"
      @success="afterImportSuccess"
    />
  </div>
</template>

<style scoped>
.batch-tip {
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
}
</style>
