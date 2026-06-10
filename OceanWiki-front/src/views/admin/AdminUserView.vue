<script setup lang="ts">
// 文件说明：这个 Vue 页面组件负责用户页面的数据请求、交互逻辑和界面展示。
import { computed, onMounted, reactive, ref } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import ExcelImportModal from '@/components/ExcelImportModal.vue'
import type { CommonResp, IdValue, Role, User } from '@/types'
import { getLoginUser } from '@/utils/auth'

const users = ref<User[]>([])
const roles = ref<Role[]>([])
const modalOpen = ref(false)
const roleModalOpen = ref(false)
const batchRoleModalOpen = ref(false)
const importModal = ref<InstanceType<typeof ExcelImportModal>>()
const selectedRoleId = ref<IdValue>()
const batchSelectedRoleId = ref<IdValue>()
const selectedRowKeys = ref<IdValue[]>([])
const currentUser = ref<User>()
const loginUser = getLoginUser()
const keyword = ref('')

const user = ref<User>({
  // 账号，新增或编辑用户时提交给后端
  loginName: '',
  // 姓名，页面展示用
  name: '',
  // 初始密码，新增用户默认 123456；编辑时为空表示不修改密码
  password: '123456',
})

const formTitle = computed(() => (user.value.id ? '编辑用户' : '新增用户'))
const availableRoles = computed(() => roles.value.filter((item) => item.code !== 'SUPER_ADMIN' && item.id))
const isFormSuperAdmin = computed(() => user.value.roleCodes?.includes('SUPER_ADMIN') || false)
const selectedUsers = computed(() =>
  users.value.filter((item) => selectedRowKeys.value.some((id) => String(id) === String(item.id))),
)
const selectedUserNames = computed(() => selectedUsers.value.map((item) => item.loginName).join('、'))

// 表格多选配置
// 当前登录用户和超级管理员不允许批量操作，避免误重置自己或破坏唯一超级管理员规则
const rowSelection = reactive({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: IdValue[]) => {
    selectedRowKeys.value = keys
    rowSelection.selectedRowKeys = keys
  },
  getCheckboxProps: (record: User) => ({
    disabled: record.id === loginUser?.id || record.roleCodes?.includes('SUPER_ADMIN'),
  }),
})

const columns = [
  { title: '账号', dataIndex: 'loginName' },
  { title: '昵称', dataIndex: 'name' },
  { title: '角色', dataIndex: 'roleNames' },
  { title: '操作', dataIndex: 'action', width: 360 },
]

const loadData = async () => {
  const resp = await http.get<CommonResp<User[]>>('/user/list', {
    params: {
      // 搜索关键词，后端按账号和姓名做模糊查询
      keyword: keyword.value.trim() || undefined,
    },
  })
  users.value = resp.data.content || []
  // 刷新列表后清理已经不存在或不能批量操作的选中项
  const selectableIds = users.value
    .filter((item) => item.id !== loginUser?.id && !item.roleCodes?.includes('SUPER_ADMIN'))
    .map((item) => String(item.id))
  selectedRowKeys.value = selectedRowKeys.value.filter((id) => selectableIds.includes(String(id)))
  rowSelection.selectedRowKeys = selectedRowKeys.value
}

const resetSearch = async () => {
  keyword.value = ''
  await loadData()
}

const loadRoles = async () => {
  const resp = await http.get<CommonResp<Role[]>>('/role/all')
  roles.value = resp.data.content || []
}

const getDefaultRoleId = () => {
  // 新增用户时默认勾选普通用户，避免忘记分配角色后无法正常使用基础功能
  const normalRole = availableRoles.value.find((item) => item.code === 'NORMAL_USER')
  return normalRole?.id
}

const add = () => {
  user.value = {
    loginName: '',
    name: '',
    password: '123456',
  }
  selectedRoleId.value = getDefaultRoleId()
  modalOpen.value = true
}

const edit = (record: User) => {
  user.value = { ...record, password: '' }
  selectedRoleId.value = availableRoles.value.find((item) =>
    record.roleIds?.some((roleId) => String(roleId) === String(item.id)),
  )?.id
  modalOpen.value = true
}

const save = async () => {
  if (!isFormSuperAdmin.value && !selectedRoleId.value) {
    message.warning('请选择一个角色')
    return
  }

  const payload: User = { ...user.value }
  // roleIds 是前端表单额外提交的字段，后端会用它同步 sys_user_role 关系表
  // 超级管理员不能在用户管理里调整角色，避免误删系统唯一超级管理员
  if (isFormSuperAdmin.value) {
    delete payload.roleIds
  } else {
    payload.roleIds = [selectedRoleId.value as IdValue]
  }

  const resp = await http.post<CommonResp<null>>('/user/save', payload)
  if (resp.data.success) {
    message.success('保存成功')
    modalOpen.value = false
    await loadData()
  }
}

const resetPassword = (record: User) => {
  Modal.confirm({
    title: `确认重置“${record.loginName}”的密码吗？`,
    content: '重置后密码会变为 123456。',
    okText: '确认重置',
    cancelText: '取消',
    onOk: async () => {
      const resp = await http.post<CommonResp<null>>('/user/resetPassword', {
        // 要重置密码的用户 id
        id: record.id,
        // 重置后的默认密码
        password: '123456',
      })
      if (resp.data.success) {
        message.success('密码已重置为 123456')
      }
    },
  })
}

const openAssignRoles = async (record: User) => {
  if (record.roleCodes?.includes('SUPER_ADMIN')) {
    message.warning('超级管理员角色不能在用户管理里调整')
    return
  }

  currentUser.value = record
  const resp = await http.get<CommonResp<IdValue[]>>(`/user/roleIds/${record.id}`)
  const roleIds = resp.data.content || []
  selectedRoleId.value = availableRoles.value.find((item) =>
    roleIds.some((roleId) => String(roleId) === String(item.id)),
  )?.id
  roleModalOpen.value = true
}

const saveUserRoles = async () => {
  if (!selectedRoleId.value) {
    message.warning('请选择一个角色')
    return
  }

  const resp = await http.post<CommonResp<null>>('/user/assignRoles', {
    // 要分配角色的用户 id
    userId: currentUser.value?.id,
    // 分配给用户的角色 id 列表，当前系统限制只能选一个角色
    roleIds: [selectedRoleId.value],
  })
  if (resp.data.success) {
    message.success('角色已保存')
    roleModalOpen.value = false
    await loadData()
  }
}

const batchResetPassword = () => {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要重置密码的用户')
    return
  }

  Modal.confirm({
    title: `确认批量重置 ${selectedRowKeys.value.length} 个用户的密码吗？`,
    content: `选中账号：${selectedUserNames.value || '无'}。重置后密码都会变为 123456。`,
    okText: '确认重置',
    cancelText: '取消',
    onOk: async () => {
      const resp = await http.post<CommonResp<null>>('/user/batchResetPassword', {
        // 批量重置密码的用户 id 列表
        userIds: selectedRowKeys.value,
        // 批量重置后的默认密码
        password: '123456',
      })
      if (resp.data.success) {
        message.success('批量重置成功，密码已重置为 123456')
        selectedRowKeys.value = []
        rowSelection.selectedRowKeys = []
        await loadData()
      }
    },
  })
}

const openBatchAssignRoles = () => {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要分配角色的用户')
    return
  }
  batchSelectedRoleId.value = getDefaultRoleId()
  batchRoleModalOpen.value = true
}

const saveBatchUserRoles = async () => {
  if (!batchSelectedRoleId.value) {
    message.warning('请选择一个角色')
    return
  }

  const resp = await http.post<CommonResp<null>>('/user/batchAssignRoles', {
    // 批量分配角色的用户 id 列表
    userIds: selectedRowKeys.value,
    // 要分配给这些用户的角色 id
    roleId: batchSelectedRoleId.value,
  })
  if (resp.data.success) {
    message.success('批量分配角色成功')
    batchRoleModalOpen.value = false
    selectedRowKeys.value = []
    rowSelection.selectedRowKeys = []
    await loadData()
  }
}

const openImport = () => {
  importModal.value?.show()
}

const remove = (record: User) => {
  if (record.id === loginUser?.id) {
    message.warning('不能删除当前登录用户')
    return
  }

  Modal.confirm({
    title: `确认删除“${record.loginName}”吗？`,
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/user/delete/${record.id}`)
      if (resp.data.success) {
        message.success('删除成功')
        await loadData()
      }
    },
  })
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadData()])
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-typography-title :level="3">用户管理</a-typography-title>
      <div class="toolbar-left">
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按账号或姓名模糊搜索"
          style="width: 260px"
          @search="loadData"
        >
          <template #enterButton>
            <a-button>
              <SearchOutlined />
              搜索
            </a-button>
          </template>
        </a-input-search>
        <a-button @click="resetSearch">重置</a-button>
        <a-button @click="loadData">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button type="primary" @click="add">
          <PlusOutlined />
          新增
        </a-button>
        <a-button :disabled="!selectedRowKeys.length" @click="batchResetPassword">
          批量重置密码
        </a-button>
        <a-button :disabled="!selectedRowKeys.length" @click="openBatchAssignRoles">
          <KeyOutlined />
          批量分配角色
        </a-button>
        <a-button @click="openImport">
          <UploadOutlined />
          批量导入
        </a-button>
      </div>
    </div>

    <div class="panel">
      <div class="batch-tip">
        已选择 {{ selectedRowKeys.length }} 个用户
      </div>
      <a-table
        :data-source="users"
        :columns="columns"
        :row-selection="rowSelection"
        row-key="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'roleNames'">
            <a-space wrap>
              <a-tag v-for="roleName in record.roleNames || []" :key="roleName" color="blue">
                {{ roleName }}
              </a-tag>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button size="small" @click="edit(record as User)">
                <EditOutlined />
                编辑
              </a-button>
              <a-button
                size="small"
                :disabled="record.roleCodes?.includes('SUPER_ADMIN')"
                @click="openAssignRoles(record as User)"
              >
                <KeyOutlined />
                分配角色
              </a-button>
              <a-button size="small" @click="resetPassword(record as User)">重置密码</a-button>
              <a-button
                size="small"
                danger
                :disabled="record.id === loginUser?.id"
                @click="remove(record as User)"
              >
                <DeleteOutlined />
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="modalOpen" :title="formTitle" width="620px" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="账号">
          <a-input v-model:value="user.loginName" placeholder="例如：admin" />
        </a-form-item>
        <a-form-item label="姓名">
          <a-input v-model:value="user.name" placeholder="例如：管理员" />
        </a-form-item>
        <a-form-item :label="user.id ? '新密码' : '密码'">
          <a-input-password
            v-model:value="user.password"
            placeholder="新增用户必填；编辑时留空表示不改密码"
          />
        </a-form-item>
        <a-form-item label="角色">
          <a-alert
            v-if="isFormSuperAdmin"
            type="info"
            show-icon
            message="超级管理员角色不能在用户管理里调整。"
          />
          <a-alert
            v-else
            type="info"
            show-icon
            message="每个用户只能选择一个角色，新增用户时必须在这里分配角色。"
          />
          <a-radio-group
            v-if="!isFormSuperAdmin"
            v-model:value="selectedRoleId"
            class="role-check-list"
          >
            <a-space direction="vertical">
              <a-radio v-for="item in availableRoles" :key="item.id" :value="item.id">
                {{ item.name }}（{{ item.code }}）
              </a-radio>
            </a-space>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="roleModalOpen" title="分配角色" width="620px" @ok="saveUserRoles">
      <a-alert
        type="info"
        show-icon
        message="每个用户只能选择一个角色，整个系统超级管理员有且仅有一个，不予分配。"
      />
      <a-radio-group v-model:value="selectedRoleId" class="role-check-list">
        <a-space direction="vertical">
          <a-radio v-for="item in availableRoles" :key="item.id" :value="item.id">
            {{ item.name }}（{{ item.code }}）
          </a-radio>
        </a-space>
      </a-radio-group>
    </a-modal>

    <a-modal v-model:open="batchRoleModalOpen" title="批量分配角色" width="620px" @ok="saveBatchUserRoles">
      <a-alert
        type="warning"
        show-icon
        :message="`将给 ${selectedRowKeys.length} 个用户统一分配一个角色。超级管理员和当前登录用户不能参与批量操作。`"
      />
      <div class="batch-users">
        选中账号：{{ selectedUserNames || '无' }}
      </div>
      <a-radio-group v-model:value="batchSelectedRoleId" class="role-check-list">
        <a-space direction="vertical">
          <a-radio v-for="item in availableRoles" :key="item.id" :value="item.id">
            {{ item.name }}（{{ item.code }}）
          </a-radio>
        </a-space>
      </a-radio-group>
    </a-modal>

    <ExcelImportModal
      ref="importModal"
      title="用户批量导入"
      import-url="/user/import"
      template-url="/user/get-import-template"
      template-filename="用户导入模板.xlsx"
      tip="模板列为：账号、姓名、密码。密码为空时默认 123456，导入成功后统一分配普通用户角色。"
      @success="loadData"
    />
  </div>
</template>

<style scoped>
.batch-tip {
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
}

.batch-users {
  margin-top: 12px;
  color: #334155;
  line-height: 1.7;
  word-break: break-all;
}

.role-check-list {
  display: block;
  margin-top: 14px;
}
</style>
