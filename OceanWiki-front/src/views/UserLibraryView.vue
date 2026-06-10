<script setup lang="ts">
// 文件说明：这个页面负责普通用户的个人书架，把阅读历史、收藏、评论、反馈和通知集中展示。
import { computed, onMounted, reactive, ref } from 'vue'
import {
  BellOutlined,
  ClockCircleOutlined,
  CommentOutlined,
  DeleteOutlined,
  MessageOutlined,
  PlusOutlined,
  SendOutlined,
  StarOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import type {
  CommonResp,
  IdValue,
  ReadingHistory,
  UserComment,
  UserFavorite,
  UserFavoriteFolder,
  UserFeedback,
  UserNotice,
} from '@/types'

const loading = ref(false)
const favorites = ref<UserFavorite[]>([])
const favoriteFolders = ref<UserFavoriteFolder[]>([])
const newFavoriteFolderName = ref('')
const histories = ref<ReadingHistory[]>([])
const comments = ref<UserComment[]>([])
const feedbackList = ref<UserFeedback[]>([])
const notices = ref<UserNotice[]>([])
const activeTab = ref('history')

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

const feedbackForm = reactive<UserFeedback>({
  // 反馈类型，默认是普通建议
  type: 'suggestion',
  // 反馈标题
  title: '',
  // 反馈正文内容
  content: '',
})

const unreadNoticeCount = computed(() => notices.value.filter((item) => item.readFlag === 0).length)
const pendingCommentCount = computed(() => comments.value.filter((item) => item.status === 'pending').length)
const openFeedbackCount = computed(() => feedbackList.value.filter((item) => item.status === 'open').length)
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

const loadData = async () => {
  loading.value = true
  try {
    const [folderResp, favoriteResp, historyResp, commentResp, feedbackResp, noticeResp] = await Promise.all([
      http.get<CommonResp<UserFavoriteFolder[]>>('/favorite/folders'),
      http.get<CommonResp<UserFavorite[]>>('/favorite/my'),
      http.get<CommonResp<ReadingHistory[]>>('/history/my'),
      http.get<CommonResp<UserComment[]>>('/comment/my'),
      http.get<CommonResp<UserFeedback[]>>('/feedback/my'),
      http.get<CommonResp<UserNotice[]>>('/notice/my'),
    ])
    favoriteFolders.value = folderResp.data.content || []
    favorites.value = favoriteResp.data.content || []
    histories.value = historyResp.data.content || []
    comments.value = commentResp.data.content || []
    feedbackList.value = feedbackResp.data.content || []
    notices.value = noticeResp.data.content || []
  } finally {
    loading.value = false
  }
}

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
    await loadData()
  }
}

const deleteFavoriteFolder = async (record: FavoriteFolderRow) => {
  if (!record.id) return
  const resp = await http.delete<CommonResp<null>>(`/favorite/folders/${record.id}`)
  if (resp.data.success) {
    message.success(resp.data.message)
    await loadData()
  }
}

const deleteHistory = async (id?: IdValue) => {
  if (!id) return
  const resp = await http.delete<CommonResp<null>>(`/history/delete/${id}`)
  if (resp.data.success) {
    message.success('阅读历史已删除')
    await loadData()
  }
}

const deleteComment = async (id?: IdValue) => {
  if (!id) return
  const resp = await http.delete<CommonResp<null>>(`/comment/delete/${id}`)
  if (resp.data.success) {
    message.success('评论已删除')
    await loadData()
  }
}

const submitFeedback = async () => {
  if (!feedbackForm.title.trim() || !feedbackForm.content.trim()) {
    message.warning('请填写反馈标题和内容')
    return
  }
  const resp = await http.post<CommonResp<null>>('/feedback/submit', feedbackForm)
  if (resp.data.success) {
    message.success(resp.data.message)
    feedbackForm.title = ''
    feedbackForm.content = ''
    await loadData()
  }
}

const markNoticeRead = async (id?: IdValue) => {
  if (!id) return
  const resp = await http.post<CommonResp<null>>(`/notice/read/${id}`)
  if (resp.data.success) {
    await loadData()
  }
}

const statusText = (status?: string) => {
  const map: Record<string, string> = {
    published: '已发布',
    pending: '待审核',
    rejected: '已驳回',
    deleted: '已删除',
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

// 文档或电子书已经被删除时，历史记录不能再跳转到阅读页
const canContinueRead = (record: ReadingHistory) => {
  return !!record.ebookId && !!record.docId && record.ebookName !== '电子书已删除' && record.docName !== '文档已删除'
}

onMounted(loadData)
</script>

<template>
  <div class="page library-page">
    <a-spin :spinning="loading">
      <div class="toolbar">
        <div>
          <a-typography-title :level="3">我的书架</a-typography-title>
          <div class="muted">集中查看阅读进度、收藏资料、评论状态、反馈和通知。</div>
        </div>
      </div>

      <a-row :gutter="[16, 16]" class="library-stats">
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="panel stat-card">
            <ClockCircleOutlined />
            <a-statistic title="阅读记录" :value="histories.length" />
          </div>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="panel stat-card">
            <StarOutlined />
            <a-statistic title="收藏资料" :value="favorites.length" />
          </div>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="panel stat-card">
            <CommentOutlined />
            <a-statistic title="待审评论" :value="pendingCommentCount" />
          </div>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="panel stat-card">
            <BellOutlined />
            <a-statistic title="未读通知" :value="unreadNoticeCount" />
          </div>
        </a-col>
      </a-row>

      <div class="panel">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="history" tab="阅读历史">
            <a-table :data-source="histories" row-key="id" bordered>
              <a-table-column title="电子书" data-index="ebookName" />
              <a-table-column title="文档" data-index="docName" />
              <a-table-column title="阅读进度" data-index="progress" width="150">
                <template #default="{ record }">
                  <a-progress :percent="record.progress || 0" size="small" />
                </template>
              </a-table-column>
              <a-table-column title="最近阅读时间" data-index="readTime" />
              <a-table-column title="操作" width="180">
                <template #default="{ record }">
                  <a-space>
                    <router-link v-if="canContinueRead(record)" :to="`/doc?ebookId=${record.ebookId}&docId=${record.docId}`">
                      <a-button size="small" type="primary">继续阅读</a-button>
                    </router-link>
                    <a-button v-else size="small" type="primary" disabled>继续阅读</a-button>
                    <a-button size="small" danger @click="deleteHistory(record.id)">
                      <DeleteOutlined />
                    </a-button>
                  </a-space>
                </template>
              </a-table-column>
            </a-table>
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
                  <a-table-column title="类型" data-index="targetType" width="100">
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
              <a-table-column title="操作" width="100">
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
            <div class="feedback-form">
              <a-radio-group v-model:value="feedbackForm.type">
                <a-radio value="suggestion">功能建议</a-radio>
                <a-radio value="correction">内容纠错</a-radio>
              </a-radio-group>
              <a-input v-model:value="feedbackForm.title" class="feedback-input" placeholder="反馈标题" />
              <a-textarea
                v-model:value="feedbackForm.content"
                :rows="4"
                maxlength="1000"
                show-count
                placeholder="写下你发现的问题或建议"
              />
              <a-button class="feedback-submit" type="primary" @click="submitFeedback">
                <SendOutlined />
                提交反馈
              </a-button>
            </div>
            <a-table :data-source="feedbackList" row-key="id" bordered>
              <a-table-column title="标题" data-index="title" />
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
                    <template #avatar>
                      <MessageOutlined />
                    </template>
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
    </a-spin>
  </div>
</template>

<style scoped>
.library-page {
  max-width: 1280px;
  margin: 0 auto;
}

.library-stats {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stat-card :deep(.anticon) {
  color: #1677ff;
  font-size: 24px;
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

.feedback-form {
  padding: 4px 8px 8px 0;
}

.feedback-input {
  margin: 14px 0;
}

.feedback-submit {
  margin-top: 12px;
}
</style>
