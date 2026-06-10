<script setup lang="ts">
// 文件说明：这个组件负责封装 wangEditor 富文本编辑器，文档管理和用户投稿都复用它。
import '@wangeditor/editor/dist/css/style.css'

import { computed, onBeforeUnmount, shallowRef } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { message } from 'ant-design-vue'

import type { CommonResp } from '@/types'
import { getLoginUser } from '@/utils/auth'

const props = withDefaults(
  defineProps<{
    // 编辑器 HTML 内容，父组件通过 v-model 传入和接收
    modelValue?: string
    // 编辑器占位提示
    placeholder?: string
    // 编辑区域高度
    height?: string
  }>(),
  {
    placeholder: '请输入正文内容',
    height: '420px',
  },
)

const emit = defineEmits<{
  // wangEditor 内容变化后，把最新 HTML 回传给父组件
  'update:modelValue': [value: string]
}>()

// 保存编辑器实例，组件销毁时必须手动销毁，避免内存残留
const editorRef = shallowRef<IDomEditor>()

const toolbarConfig: Partial<IToolbarConfig> = {}

const valueHtml = computed({
  get: () => props.modelValue || '',
  set: (value: string) => emit('update:modelValue', value),
})

const editorConfig = computed<Partial<IEditorConfig>>(() => ({
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      // 文档正文里只保存图片 URL，不保存 base64 大文本，避免正文太大导致编辑器打开慢
      server: '/upload/doc-image',
      fieldName: 'file',
      maxFileSize: 5 * 1024 * 1024,
      allowedFileTypes: ['image/*'],
      headers: getLoginUser()?.token ? { Authorization: getLoginUser()?.token || '' } : {},
      customInsert: (response: unknown, insertFn: (url: string, alt?: string, href?: string) => void) => {
        const resp = response as CommonResp<{ url?: string }>
        if (resp.success && resp.content?.url) {
          insertFn(resp.content.url, '文档图片', resp.content.url)
          return
        }
        message.error(resp.message || '图片上传失败')
      },
    },
  },
}))

const handleCreated = (editor: IDomEditor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<template>
  <div class="wang-editor">
    <Toolbar
      class="wang-editor-toolbar"
      :editor="editorRef"
      :default-config="toolbarConfig"
      mode="default"
    />
    <Editor
      v-model="valueHtml"
      class="wang-editor-body"
      :default-config="editorConfig"
      mode="default"
      @onCreated="handleCreated"
    />
  </div>
</template>

<style scoped>
.wang-editor {
  overflow: hidden;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.wang-editor-toolbar {
  border-bottom: 1px solid #e5e7eb;
}

.wang-editor-body {
  min-height: v-bind(height);
}

.wang-editor :deep(.w-e-text-container) {
  min-height: v-bind(height);
}
</style>
