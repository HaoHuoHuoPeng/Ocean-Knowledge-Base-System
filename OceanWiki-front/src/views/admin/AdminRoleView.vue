<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责角色页面的数据请求、交互逻辑和界面展示。
import { computed, onMounted, ref } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp, IdValue, Permission, Role } from '@/types'
import { getLoginUser } from '@/utils/auth'

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const modalOpen = ref(false)
const permissionModalOpen = ref(false)
const checkedPermissionIds = ref<IdValue[]>([])
const keyword = ref('')

const role = ref<Role>({
  // 角色编码，后端用它判断权限身份
  code: '',
  // 角色名称，页面展示用
  name: '',
  // 角色说明
  description: '',
  // 是否内置角色，0 表示普通自定义角色
  builtIn: 0,
})

const currentRole = ref<Role>()
const loginUser = getLoginUser()
const isSuperAdmin = computed(() => loginUser?.roleCodes?.includes('SUPER_ADMIN') || false)
const formTitle = computed(() => (role.value.id ? '编辑角色' : '新增角色'))

const columns = [
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '角色编码', dataIndex: 'code', width: 160 },
  { title: '角色名称', dataIndex: 'name', width: 140 },
  { title: '说明', dataIndex: 'description' },
  { title: '内置', dataIndex: 'builtIn', width: 90 },
  { title: '操作', dataIndex: 'action', width: 300 },
]

const permissionGroups = computed(() => {
  const groups: Record<string, Permission[]> = {}
  permissions.value.forEach((item) => {
    const moduleName = item.module || '其他'
    if (!groups[moduleName]) {
      groups[moduleName] = []
    }
    groups[moduleName].push(item)
  })
  return groups
})

const loadRoles = async () => {
  const resp = await http.get<CommonResp<Role[]>>('/role/list', {
    params: {
      // 搜索关键词，后端按角色编码和角色名称做模糊查询
      keyword: keyword.value.trim() || undefined,
    },
  })
  roles.value = resp.data.content || []
}

const resetSearch = async () => {
  keyword.value = ''
  await loadRoles()
}

const loadPermissions = async () => {
  const resp = await http.get<CommonResp<Permission[]>>('/permission/list')
  permissions.value = resp.data.content || []
}

const add = () => {
  role.value = {
    code: '',
    name: '',
    description: '',
    builtIn: 0,
  }
  checkedPermissionIds.value = []
  modalOpen.value = true
}

const edit = async (record: Role) => {
  role.value = { ...record }

  // 编辑角色时也回显权限，保存角色资料时可以顺手修改权限
  const resp = await http.get<CommonResp<IdValue[]>>(`/role/permissionIds/${record.id}`)
  checkedPermissionIds.value = resp.data.content || []
  modalOpen.value = true
}

const save = async () => {
  // permissionIds 是前端表单额外提交的字段，后端会用它同步 sys_role_permission 关系表
  const resp = await http.post<CommonResp<null>>('/role/save', {
    ...role.value,
    // 角色拥有的权限 id 列表，保存角色时同步维护角色权限关系
    permissionIds: checkedPermissionIds.value,
  })
  if (resp.data.success) {
    message.success('保存成功')
    modalOpen.value = false
    await loadRoles()
  }
}

const remove = (record: Role) => {
  if (record.code === 'SUPER_ADMIN') {
    message.warning('超级管理员角色不能删除')
    return
  }

  Modal.confirm({
    title: `确认删除“${record.name}”吗？`,
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/role/delete/${record.id}`)
      if (resp.data.success) {
        message.success('删除成功')
        await loadRoles()
      }
    },
  })
}

const openAssignPermissions = async (record: Role) => {
  currentRole.value = record
  const resp = await http.get<CommonResp<IdValue[]>>(`/role/permissionIds/${record.id}`)
  checkedPermissionIds.value = resp.data.content || []
  permissionModalOpen.value = true
}

const savePermissions = async () => {
  const resp = await http.post<CommonResp<null>>('/role/assignPermissions', {
    // 要分配权限的角色 id
    roleId: currentRole.value?.id,
    // 勾选后的权限 id 列表
    permissionIds: checkedPermissionIds.value,
  })
  if (resp.data.success) {
    message.success('权限已保存')
    permissionModalOpen.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadPermissions()])
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <a-typography-title :level="3">角色权限管理</a-typography-title>
<!--        <div class="muted">新增角色时直接分配权限；保存后如果需要调整，可以使用列表里的“分配权限”。</div>-->
      </div>
      <div class="toolbar-left">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按编码或名称模糊搜索"
          style="width: 260px"
          @search="loadRoles"
        >
          <template #enterButton>
            <a-button>
              <SearchOutlined />
              搜索
            </a-button>
          </template>
        </a-input-search>
        <a-button @click="resetSearch">重置</a-button>
        <a-button @click="loadRoles">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button type="primary" @click="add">
          <PlusOutlined />
          新增角色
        </a-button>
      </div>
    </div>

    <div class="panel">
      <a-table :data-source="roles" :columns="columns" row-key="id" bordered>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'builtIn'">
            <a-tag :color="record.builtIn === 1 ? 'blue' : 'default'">
              {{ record.builtIn === 1 ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button size="small" @click="edit(record as Role)">
                <EditOutlined />
                编辑
              </a-button>
              <a-button size="small" @click="openAssignPermissions(record as Role)">
                <KeyOutlined />
                分配权限
              </a-button>
              <a-button
                size="small"
                danger
                :disabled="record.code === 'SUPER_ADMIN' || (record.builtIn === 1 && !isSuperAdmin)"
                @click="remove(record as Role)"
              >
                <DeleteOutlined />
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="modalOpen" :title="formTitle" width="760px" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="角色编码">
          <a-input v-model:value="role.code" placeholder="例如：BOOK_EDITOR" />
        </a-form-item>
        <a-form-item label="角色名称">
          <a-input v-model:value="role.name" placeholder="例如：电子书编辑" />
        </a-form-item>
        <a-form-item label="角色说明">
          <a-textarea v-model:value="role.description" :rows="3" />
        </a-form-item>
        <a-form-item label="权限">
          <a-alert
            v-if="role.code === 'SUPER_ADMIN'"
            type="info"
            show-icon
            message="本系统中超级管理员有且仅有一个"
          />
          <a-alert
            v-else
            type="info"
            show-icon
            message="新增角色时就在这里分配权限；保存后如果需要调整，可以再使用列表里的“分配权限”。"
          />

          <a-checkbox-group v-model:value="checkedPermissionIds" class="permission-check-list">
            <div v-for="(items, moduleName) in permissionGroups" :key="moduleName" class="permission-group">
              <a-typography-title :level="5">{{ moduleName }}</a-typography-title>
              <a-space wrap>
                <a-checkbox v-for="item in items" :key="item.id" :value="item.id">
                  {{ item.name }}（{{ item.code }}）
                </a-checkbox>
              </a-space>
            </div>
          </a-checkbox-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="permissionModalOpen"
      title="分配权限"
      width="760px"
      @ok="savePermissions"
    >
      <a-alert
        v-if="currentRole?.code === 'SUPER_ADMIN'"
        type="info"
        show-icon
        message="超级管理员建议保留全部权限，并且只能分配给一个用户。"
      />

      <a-checkbox-group v-model:value="checkedPermissionIds" class="permission-check-list">
        <div v-for="(items, moduleName) in permissionGroups" :key="moduleName" class="permission-group">
          <a-typography-title :level="5">{{ moduleName }}</a-typography-title>
          <a-space wrap>
            <a-checkbox v-for="item in items" :key="item.id" :value="item.id">
              {{ item.name }}（{{ item.code }}）
            </a-checkbox>
          </a-space>
        </div>
      </a-checkbox-group>
    </a-modal>
  </div>
</template>

<style scoped>
.permission-check-list {
  display: block;
  margin-top: 14px;
}

.permission-group {
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
}

.permission-group:last-child {
  border-bottom: 0;
}
</style>
