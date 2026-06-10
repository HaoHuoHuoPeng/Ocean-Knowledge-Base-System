<script setup lang="ts">
// 文件说明：这个页面负责个人中心，展示账号资料、收藏、阅读历史、评论、反馈和通知。
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { DeleteOutlined, LockOutlined, PlusOutlined, SaveOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import type {
  CommonResp,
  IdValue,
  LoginUser,
  ReadingHistory,
  UserComment,
  UserFavorite,
  UserFavoriteFolder,
  UserFeedback,
  UserNotice,
} from '@/types'
import { setLoginUser } from '@/utils/auth'

const route = useRoute()
const activeTab = ref('account')
const favorites = ref<UserFavorite[]>([])
const favoriteFolders = ref<UserFavoriteFolder[]>([])
const newFavoriteFolderName = ref('')
const histories = ref<ReadingHistory[]>([])
const comments = ref<UserComment[]>([])
const feedbackList = ref<UserFeedback[]>([])
const notices = ref<UserNotice[]>([])

interface FavoriteFolderRow extends UserFavoriteFolder {
  // 表格行唯一标识
  key: string
  // 当前分组里的收藏记录
  items: UserFavorite[]
  // 当前分组收藏数量
  count: number
  // 是否允许删除
  deletable: boolean
}

const form = reactive({
  // 当前登录账号
  loginName: '',
  // 当前用户姓名
  name: '',
  // 修改密码时用来确认是本人操作
  currentPassword: '',
  // 新密码，不填表示不修改密码
  newPassword: '',
})

// 按收藏分组整理成表格行，分组是第一层，收藏记录是展开后的第二层
const favoriteGroups = computed<FavoriteFolderRow[]>(() => {
  const groupMap = new Map<string, FavoriteFolderRow>()
  favoriteFolders.value.forEach((folder) => {
    const name = folder.name || '默认分组'
    groupMap.set(name, {
      ...folder,
      key: String(folder.id || name),
      name,
      items: [],
      count: 0,
      deletable: name !== '默认分组',
    })
  })
  favorites.value.forEach((favorite) => {
    const name = favorite.folderName || '默认分组'
    if (!groupMap.has(name)) {
      groupMap.set(name, {
        key: name,
        userId: favorite.userId,
        name,
        items: [],
        count: 0,
        deletable: name !== '默认分组',
      })
    }
    groupMap.get(name)?.items.push(favorite)
  })
  return Array.from(groupMap.values()).map((group) => ({
    ...group,
    count: group.items.length,
  }))
})

// 查询当前登录用户资料
const loadProfile = async () => {
  const resp = await http.get<CommonResp<LoginUser>>('/user/profile')
  const user = resp.data.content
  if (resp.data.success && user) {
    form.loginName = user.loginName
    form.name = user.name
  }
}

// 保存账号、姓名和密码
const save = async () => {
  const resp = await http.post<CommonResp<LoginUser>>('/user/profile/update', form)
  const user = resp.data.content
  if (resp.data.success && user) {
    setLoginUser(user)
    form.currentPassword = ''
    form.newPassword = ''
    message.success('账号资料已保存')
  }
}

// 查询我的收藏
const loadFavorites = async () => {
  const resp = await http.get<CommonResp<UserFavorite[]>>('/favorite/my')
  favorites.value = resp.data.content || []
}

// 查询我的收藏分组
const loadFavoriteFolders = async () => {
  const resp = await http.get<CommonResp<UserFavoriteFolder[]>>('/favorite/folders')
  favoriteFolders.value = resp.data.content || []
}

// 查询阅读历史
const loadHistories = async () => {
  const resp = await http.get<CommonResp<ReadingHistory[]>>('/history/my')
  histories.value = resp.data.content || []
}

// 查询我的评论
const loadComments = async () => {
  const resp = await http.get<CommonResp<UserComment[]>>('/comment/my')
  comments.value = resp.data.content || []
}

// 查询我的反馈
const loadFeedback = async () => {
  const resp = await http.get<CommonResp<UserFeedback[]>>('/feedback/my')
  feedbackList.value = resp.data.content || []
}

// 查询我的通知
const loadNotices = async () => {
  const resp = await http.get<CommonResp<UserNotice[]>>('/notice/my')
  notices.value = resp.data.content || []
}

const deleteHistory = async (id?: IdValue) => {
  if (!id) return
  const resp = await http.delete<CommonResp<null>>(`/history/delete/${id}`)
  if (resp.data.success) {
    message.success('阅读历史已删除')
    await loadHistories()
  }
}

const deleteComment = async (id?: IdValue) => {
  if (!id) return
  const resp = await http.delete<CommonResp<null>>(`/comment/delete/${id}`)
  if (resp.data.success) {
    message.success('评论已删除')
    await loadComments()
  }
}

const markNoticeRead = async (id?: IdValue) => {
  if (!id) return
  const resp = await http.post<CommonResp<null>>(`/notice/read/${id}`)
  if (resp.data.success) {
    await loadNotices()
  }
}

// 新增收藏分组
const addFavoriteFolder = async () => {
  const name = newFavoriteFolderName.value.trim()
  if (!name) {
    message.warning('请先填写分组名称')
    return
  }
  const resp = await http.post<CommonResp<UserFavoriteFolder>>('/favorite/folders', { name })
  if (resp.data.success) {
    message.success(resp.data.message)
    newFavoriteFolderName.value = ''
    await loadFavoriteFolders()
  }
}

// 删除收藏分组，后端会连带删除这个分组里的收藏记录
const deleteFavoriteFolder = async (record: FavoriteFolderRow) => {
  if (!record.id) return
  const resp = await http.delete<CommonResp<null>>(`/favorite/folders/${record.id}`)
  if (resp.data.success) {
    message.success(resp.data.message)
    await loadFavoriteFolders()
    await loadFavorites()
  }
}

const statusText = (status?: string) => {
  const map: Record<string, string> = {
    published: '已发布',
    pending: '待审核',
    rejected: '已驳回',
    open: '待处理',
    handled: '已处理',
  }
  return status ? map[status] || status : '-'
}

const targetTypeText = (type?: string) => {
  if (type === 'ebook') return '电子书'
  if (type === 'doc') return '文档'
  return '-'
}

// 文档或电子书已经被删除时，历史记录只保留查看价值，不能继续跳转阅读
const canContinueRead = (record: ReadingHistory) => {
  return !!record.ebookId && !!record.docId && record.ebookName !== '电子书已删除' && record.docName !== '文档已删除'
}

const syncTabFromRoute = () => {
  const tab = route.query.tab
  if (typeof tab === 'string') {
    activeTab.value = tab
  }
}

watch(
  () => route.query.tab,
  () => {
    syncTabFromRoute()
  },
)

onMounted(async () => {
  syncTabFromRoute()
  await loadProfile()
  await Promise.all([loadFavoriteFolders(), loadFavorites(), loadHistories(), loadComments(), loadFeedback(), loadNotices()])
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <a-typography-title :level="3">我的中心</a-typography-title>
        <div class="muted">在这里可以管理自己的账号资料、收藏、阅读历史、评论、反馈和通知。</div>
      </div>
    </div>

    <div class="panel">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="account" tab="账号资料">
          <a-form class="profile-form" layout="vertical" @submit.prevent="save">
            <a-form-item label="账号">
              <a-input v-model:value="form.loginName" placeholder="请输入账号">
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="姓名">
              <a-input v-model:value="form.name" placeholder="请输入姓名">
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="当前密码">
              <a-input-password v-model:value="form.currentPassword" placeholder="修改密码时才需要填写">
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item label="新密码">
              <a-input-password v-model:value="form.newPassword" placeholder="不填表示不修改密码">
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-button type="primary" @click="save">
              <SaveOutlined />
              保存
            </a-button>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="favorites" tab="我的收藏">
          <div class="favorite-folder-toolbar">
            <a-input
              v-model:value="newFavoriteFolderName"
              class="favorite-folder-input"
              allow-clear
              maxlength="50"
              placeholder="新分组名称"
              @press-enter="addFavoriteFolder"
            />
            <a-button type="primary" @click="addFavoriteFolder">
              <PlusOutlined />
              新增分组
            </a-button>
          </div>

          <a-table :data-source="favoriteGroups" row-key="key" bordered>
            <template #expandedRowRender="{ record }">
              <a-table :data-source="record.items" row-key="id" size="small" bordered :pagination="false">
                <a-table-column title="类型" data-index="targetType" width="110">
                  <template #default="{ record: item }">
                    {{ targetTypeText(item.targetType) }}
                  </template>
                </a-table-column>
                <a-table-column title="名称" data-index="targetName" />
                <a-table-column title="收藏时间" data-index="createTime" />
                <a-table-column title="操作" width="110">
                  <template #default="{ record: item }">
                    <router-link :to="`/doc?ebookId=${item.ebookId || item.targetId}`">
                      <a-button size="small">去阅读</a-button>
                    </router-link>
                  </template>
                </a-table-column>
              </a-table>
            </template>
            <a-table-column title="分组名称" data-index="name" />
            <a-table-column title="收藏数量" data-index="count" width="120" />
            <a-table-column title="创建时间" data-index="createTime" width="190" />
            <a-table-column title="操作" width="160">
              <template #default="{ record }">
                <a-popconfirm
                  v-if="record.deletable"
                  title="删除后，这个分组里的收藏内容也会一起删除，确定删除吗？"
                  ok-text="确定删除"
                  cancel-text="取消"
                  @confirm="deleteFavoriteFolder(record)"
                >
                  <a-button size="small" danger>
                    <DeleteOutlined />
                    删除分组
                  </a-button>
                </a-popconfirm>
                <a-tag v-else>默认保留</a-tag>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="history" tab="阅读历史">
          <a-table :data-source="histories" row-key="id" bordered>
            <a-table-column title="电子书" data-index="ebookName" />
            <a-table-column title="文档" data-index="docName" />
            <a-table-column title="阅读进度" data-index="progress" width="140">
              <template #default="{ record }">
                <a-progress :percent="record.progress || 0" size="small" />
              </template>
            </a-table-column>
            <a-table-column title="最近阅读时间" data-index="readTime" />
            <a-table-column title="操作">
              <template #default="{ record }">
                <a-space>
                  <router-link v-if="canContinueRead(record)" :to="`/doc?ebookId=${record.ebookId}&docId=${record.docId}`">
                    <a-button size="small">继续阅读</a-button>
                  </router-link>
                  <a-button v-else size="small" disabled>继续阅读</a-button>
                  <a-button size="small" danger @click="deleteHistory(record.id)">
                    <DeleteOutlined />
                    删除
                  </a-button>
                </a-space>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="comments" tab="我的评论">
          <a-table :data-source="comments" row-key="id" bordered>
            <a-table-column title="对象" data-index="targetName" />
            <a-table-column title="评论内容" data-index="content" />
            <a-table-column title="状态">
              <template #default="{ record }">
                <a-tag>{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="敏感词" data-index="sensitiveHit" />
            <a-table-column title="审核备注" data-index="reviewRemark" />
            <a-table-column title="操作">
              <template #default="{ record }">
                <a-button size="small" danger @click="deleteComment(record.id)">
                  <DeleteOutlined />
                  删除
                </a-button>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="feedback" tab="我的反馈">
          <a-table :data-source="feedbackList" row-key="id" bordered>
            <a-table-column title="标题" data-index="title" />
            <a-table-column title="内容" data-index="content" />
            <a-table-column title="状态">
              <template #default="{ record }">
                <a-tag>{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="处理备注" data-index="handleRemark" />
            <a-table-column title="提交时间" data-index="createTime" />
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="notices" tab="通知">
          <a-list :data-source="notices">
            <template #renderItem="{ item }">
              <a-list-item>
                <template #actions>
                  <a-button v-if="item.readFlag === 0" size="small" @click="markNoticeRead(item.id)">
                    标为已读
                  </a-button>
                  <a-tag v-else>已读</a-tag>
                </template>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <span>{{ item.title }}</span>
                      <a-tag v-if="item.readFlag === 0" color="red">未读</a-tag>
                    </a-space>
                  </template>
                  <template #description>
                    <div>{{ item.content }}</div>
                    <div class="muted">{{ item.createTime }}</div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<style scoped>
.profile-form {
  max-width: 560px;
}

.favorite-folder-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.favorite-folder-input {
  width: 240px;
}

</style>
