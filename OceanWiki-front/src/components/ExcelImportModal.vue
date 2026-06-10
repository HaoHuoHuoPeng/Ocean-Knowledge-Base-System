<script setup lang="ts">
// 文件说明：这个组件负责通用 Excel 批量导入弹窗，电子书、用户、敏感词导入都复用它。
import { h, ref } from 'vue'
import { DownloadOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp, ImportResp } from '@/types'

const props = defineProps<{
  title: string
  importUrl: string
  templateUrl: string
  templateFilename: string
  tip: string
}>()

const emit = defineEmits<{
  success: []
}>()

const open = ref(false)
const loading = ref(false)
const fileList = ref<any[]>([])

// 打开弹窗时清空上一次选中的文件，避免误导入旧文件
const show = () => {
  open.value = true
  fileList.value = []
}

// 暴露给父页面调用，例如 ebookImportModal.value?.show()
defineExpose({ show })

const beforeUpload = (file: File) => {
  const filename = file.name.toLowerCase()
  if (!filename.endsWith('.xls') && !filename.endsWith('.xlsx')) {
    message.warning('请上传 xls 或 xlsx 格式的 Excel 文件')
    return false
  }

  // 只允许保留一个文件，和参考项目里的导入弹窗保持一致
  fileList.value = [file]
  return false
}

const removeFile = () => {
  fileList.value = []
}

const downloadTemplate = async () => {
  const resp = await http.get(props.templateUrl, {
    responseType: 'blob',
  })
  const blob = new Blob([resp.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = props.templateFilename
  link.click()
  URL.revokeObjectURL(url)
}

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const buildResultHtml = (data: ImportResp) => {
  const lines = [
    `导入成功数量：${data.successNames.length}`,
    ...data.successNames.map((name) => `&lt; ${escapeHtml(name)} &gt;`),
    `导入失败数量：${Object.keys(data.failureMap).length}`,
    ...Object.entries(data.failureMap).map(
      ([row, reason]) => `&lt; ${escapeHtml(row)}：${escapeHtml(reason)} &gt;`,
    ),
  ]
  return lines.join('<br />')
}

const submit = async () => {
  if (!fileList.value.length) {
    message.warning('请先选择要导入的 Excel 文件')
    return
  }

  const formData = new FormData()
  // file 是传给后端的 Excel 文件字段，后端按这个字段名接收 MultipartFile
  formData.append('file', fileList.value[0])
  loading.value = true
  try {
    const resp = await http.post<CommonResp<ImportResp>>(props.importUrl, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    if (resp.data.success && resp.data.content) {
      Modal.info({
        title: '导入结果',
        content: h('div', { innerHTML: buildResultHtml(resp.data.content) }),
        okText: '知道了',
      })
      open.value = false
      fileList.value = []
      emit('success')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <a-modal v-model:open="open" :title="title" width="520px" :confirm-loading="loading" @ok="submit">
    <a-upload-dragger
      :file-list="fileList"
      :before-upload="beforeUpload"
      :on-remove="removeFile"
      :max-count="1"
      accept=".xls,.xlsx"
      name="file"
    >
      <p class="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p class="ant-upload-text">将 Excel 文件拖到这里，或点击选择文件</p>
      <p class="ant-upload-hint">{{ tip }}</p>
    </a-upload-dragger>

    <div class="import-footer">
      <span>仅允许导入 xls、xlsx 格式文件。</span>
      <a-button type="link" size="small" @click="downloadTemplate">
        <DownloadOutlined />
        下载模板
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped>
.import-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  color: #64748b;
  font-size: 13px;
}
</style>
