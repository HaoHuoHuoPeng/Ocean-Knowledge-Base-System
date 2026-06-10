<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责分类页面的数据请求、交互逻辑和界面展示。
import { onMounted, ref } from 'vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import type { Category, CommonResp, IdValue } from '@/types'
import { arrayToTree, findNameById } from '@/utils/tree'

interface CategoryRow extends Category {
  displayIndex?: string
  children?: CategoryRow[]
}

const categories = ref<Category[]>([])
const categoryTree = ref<CategoryRow[]>([])
const parentTreeOptions = ref<CategoryRow[]>([])
const modalOpen = ref(false)

const category = ref<Category>({
  // 父分类 id，0 表示一级分类
  parent: 0,
  // 分类名称
  name: '',
})

const columns = [
  { title: '分类名称', dataIndex: 'name' },
  { title: '父分类', dataIndex: 'parent', width: 160 },
  { title: '操作', dataIndex: 'action', width: 170 },
]

// 给树形分类生成展示序号
// 一级是 1、2、3；二级是 1.1、1.2；三级继续变成 1.1.1
const fillDisplayIndex = (list: Category[], parentIndex = ''): CategoryRow[] => {
  return list.map((item, index) => {
    const displayIndex = parentIndex ? `${parentIndex}.${index + 1}` : String(index + 1)
    return {
      ...item,
      displayIndex,
      children: fillDisplayIndex(item.children || [], displayIndex),
    }
  })
}

// 父分类选择也使用树形数据，这样分类层级很多时也能按层级查找。
// 编辑分类时，不能把自己或者自己的子分类选为父分类，否则会形成循环树。
const buildParentTreeOptions = (list: CategoryRow[], currentId?: IdValue): CategoryRow[] => {
  return list
    .filter((item) => String(item.id) !== String(currentId))
    .map((item) => ({
      ...item,
      children: buildParentTreeOptions(item.children || [], currentId),
    }))
}

const loadData = async () => {
  const resp = await http.get<CommonResp<Category[]>>('/category/all')
  categories.value = resp.data.content || []
  categoryTree.value = fillDisplayIndex(arrayToTree(categories.value))
  parentTreeOptions.value = categoryTree.value
}

const add = () => {
  category.value = {
    parent: 0,
    name: '',
  }
  parentTreeOptions.value = categoryTree.value
  modalOpen.value = true
}

const edit = (record: Category) => {
  category.value = { ...record }
  parentTreeOptions.value = buildParentTreeOptions(categoryTree.value, record.id)
  modalOpen.value = true
}

const save = async () => {
  const resp = await http.post<CommonResp<null>>('/category/save', category.value)
  if (resp.data.success) {
    message.success('保存成功')
    modalOpen.value = false
    await loadData()
  }
}

const remove = (record: Category) => {
  Modal.confirm({
    title: `确认删除「${record.name}」吗？`,
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/category/delete/${record.id}`)
      if (resp.data.success) {
        message.success('删除成功')
        await loadData()
      }
    },
  })
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-typography-title :level="3">海洋生物分类管理</a-typography-title>
      <div class="toolbar-left">
        <a-button @click="loadData">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button type="primary" @click="add">
          <PlusOutlined />
          新增
        </a-button>
      </div>
    </div>

    <div class="panel">
      <a-table
        :data-source="categoryTree"
        :columns="columns"
        :children-column-name="'children'"
        :default-expand-all-rows="true"
        :indent-size="24"
        :pagination="false"
        row-key="id"
        size="middle"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <span class="category-name-cell">
              <span class="category-index">{{ record.displayIndex }}</span>
              <span class="category-name">{{ record.name }}</span>
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'parent'">
            {{ record.parent === 0 ? '无父分类' : findNameById(categoryTree, record.parent) }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button size="small" @click="edit(record as Category)">
                <EditOutlined />
                编辑
              </a-button>
              <a-button size="small" danger @click="remove(record as Category)">
                <DeleteOutlined />
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="modalOpen" title="分类表单" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="父分类">
          <a-tree-select
            v-model:value="category.parent"
            :tree-data="parentTreeOptions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择父分类"
            tree-default-expand-all
            allow-clear
          >
            <template #title="{ name, displayIndex }">
              <span>{{ displayIndex }} {{ name }}</span>
            </template>
          </a-tree-select>
          <div class="form-tip">不选父分类时，保存后会作为一级分类。</div>
        </a-form-item>
        <a-form-item label="分类名称">
          <a-input v-model:value="category.name" placeholder="例如：鲸豚类" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.category-name-cell {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.category-index {
  display: inline-flex;
  min-width: 38px;
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

.category-name {
  color: #0f172a;
  font-weight: 600;
}

.form-tip {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}
</style>
