<script setup lang="ts">
// 文件说明：这个页面负责文档阅读，包括目录、正文、收藏分组、点赞、评论、反馈和阅读进度。
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  CommentOutlined,
  DeleteOutlined,
  LikeFilled,
  LikeOutlined,
  SendOutlined,
  StarFilled,
  StarOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

import http from '@/api/http'
import type {
  CommonResp,
  Doc,
  DocVoteStatus,
  FavoriteStatus,
  IdValue,
  UserComment,
  UserFavoriteFolder,
  UserFeedback,
} from '@/types'
import { normalizeEditorHtml } from '@/utils/html'
import { getLoginUser, hasPermission, isCurrentSuperAdmin } from '@/utils/auth'
import { arrayToTree } from '@/utils/tree'

const route = useRoute()

interface DocTreeNode extends Omit<Doc, 'children'> {
  // 页面展示用的目录序号，例如 1、1.1、1.1.1
  displayIndex?: string
  // 是否有子文档，用来控制展开按钮
  hasChildren?: boolean
  children?: DocTreeNode[]
}

const docs = ref<DocTreeNode[]>([])
const flatDocs = ref<Doc[]>([])
const currentDoc = ref<Doc>()
const content = ref('')
const loading = ref(false)
const voted = ref(false)
const favorited = ref(false)
const comments = ref<UserComment[]>([])
const commentContent = ref('')
const replyContent = ref('')
const replyParentId = ref<IdValue>()
const docKeyword = ref('')
const favoriteFolderName = ref('默认分组')
const favoriteFolders = ref<UserFavoriteFolder[]>([])
const readingProgress = ref(0)
const feedbackOpen = ref(false)
const contentRef = ref<HTMLElement>()
const expandedCatalogKeys = ref<IdValue[]>([])
const loginUser = computed(() => getLoginUser())

const feedbackForm = reactive<UserFeedback>({
  // 反馈目标类型，阅读页默认反馈文档
  targetType: 'doc',
  // 反馈类型，correction 表示内容纠错
  type: 'correction',
  // 反馈标题
  title: '',
  // 反馈正文
  content: '',
})

const ebookId = computed<IdValue>(() => String(route.query.ebookId || ''))
const routeDocId = computed<IdValue>(() => String(route.query.docId || ''))

const filteredDocs = computed<DocTreeNode[]>(() => {
  const text = docKeyword.value.trim()
  if (!text) {
    return docs.value
  }
  return filterDocTree(docs.value, text)
})

const visibleCatalogRows = computed(() => flattenVisibleDocs(filteredDocs.value))

// 给文档树生成类似分类管理的层级序号
const buildDisplayDocs = (list: Doc[]): DocTreeNode[] => {
  const walk = (treeList: Doc[], parentIndex = ''): DocTreeNode[] => {
    return treeList.map((item, index) => {
      const displayIndex = parentIndex ? `${parentIndex}.${index + 1}` : String(index + 1)
      const children = walk((item.children || []) as Doc[], displayIndex)
      return {
        ...item,
        displayIndex,
        hasChildren: children.length > 0,
        children,
      }
    })
  }

  return walk(arrayToTree(list) as Doc[])
}

// 搜索目录时保留父级目录，否则搜索到子文档时看不出它属于哪个父文档
const filterDocTree = (list: DocTreeNode[], keyword: string): DocTreeNode[] => {
  return list
    .map((item) => {
      const children = filterDocTree(item.children || [], keyword)
      const matched = item.name.includes(keyword)
      if (matched || children.length) {
        return { ...item, children }
      }
      return undefined
    })
    .filter(Boolean) as DocTreeNode[]
}

// 只把已经展开的目录节点拍平成页面行
const flattenVisibleDocs = (list: DocTreeNode[], level = 0): Array<DocTreeNode & { level: number }> => {
  return list.flatMap((item) => {
    const current = { ...item, level }
    if (!item.id || !item.hasChildren || !isCatalogExpanded(item.id)) {
      return [current]
    }
    return [current, ...flattenVisibleDocs(item.children || [], level + 1)]
  })
}

const isCatalogExpanded = (id: IdValue) => {
  return expandedCatalogKeys.value.map(String).includes(String(id))
}

const toggleCatalog = (doc: DocTreeNode) => {
  if (!doc.id || !doc.hasChildren) {
    return
  }
  if (isCatalogExpanded(doc.id)) {
    expandedCatalogKeys.value = expandedCatalogKeys.value.filter((id) => String(id) !== String(doc.id))
    return
  }
  expandedCatalogKeys.value = [...expandedCatalogKeys.value, doc.id]
}

const collectParentDocKeys = (list: DocTreeNode[]): IdValue[] => {
  return list.flatMap((item) => [
    ...(item.id && item.hasChildren ? [item.id] : []),
    ...collectParentDocKeys(item.children || []),
  ])
}

// 查询当前电子书的文档目录
const loadDocs = async () => {
  if (!ebookId.value) {
    return
  }

  const resp = await http.get<CommonResp<Doc[]>>(`/doc/all/${ebookId.value}`)
  flatDocs.value = resp.data.content || []
  docs.value = buildDisplayDocs(flatDocs.value)
  expandedCatalogKeys.value = collectParentDocKeys(docs.value)

  const target = flatDocs.value.find((item) => String(item.id) === String(routeDocId.value)) || flatDocs.value[0]
  if (target?.id) {
    await openDoc(target)
  }
}

// 打开文档正文
const openDoc = async (doc: Doc) => {
  if (!doc.id) {
    return
  }

  if (currentDoc.value?.id && String(currentDoc.value.id) !== String(doc.id)) {
    updateReadingProgress()
    await saveProgress()
  }

  loading.value = true
  currentDoc.value = doc
  try {
    const resp = await http.get<CommonResp<string>>(`/doc/findContent/${doc.id}`)
    content.value = normalizeEditorHtml(resp.data.content)
    readingProgress.value = 0
    doc.viewCount = (doc.viewCount || 0) + 1
    await Promise.all([loadVoteStatus(doc), loadFavoriteStatus(doc), loadComments(doc)])
    await nextTick()
    updateReadingProgress()
    await saveProgress()
  } finally {
    loading.value = false
  }
}

const loadVoteStatus = async (doc: Doc) => {
  if (!doc.id) {
    voted.value = false
    return
  }

  const resp = await http.get<CommonResp<DocVoteStatus>>(`/doc/voteStatus/${doc.id}`)
  if (resp.data.success && resp.data.content) {
    voted.value = resp.data.content.voted
    doc.voteCount = resp.data.content.voteCount
  }
}

const loadFavoriteStatus = async (doc: Doc) => {
  if (!doc.id) {
    favorited.value = false
    favoriteFolderName.value = '默认分组'
    return
  }

  const resp = await http.get<CommonResp<FavoriteStatus>>('/favorite/status', {
    params: {
      // 收藏目标类型
      targetType: 'doc',
      // 收藏目标 id
      targetId: doc.id,
    },
  })
  if (resp.data.success && resp.data.content) {
    favorited.value = resp.data.content.favorited
    favoriteFolderName.value = resp.data.content.folderName || '默认分组'
  }
}

const loadFavoriteFolders = async () => {
  const resp = await http.get<CommonResp<UserFavoriteFolder[]>>('/favorite/folders')
  favoriteFolders.value = resp.data.content || []
  if (!favoriteFolderName.value && favoriteFolders.value.length) {
    favoriteFolderName.value = favoriteFolders.value[0].name
  }
}

const loadComments = async (doc = currentDoc.value) => {
  if (!doc?.id) {
    comments.value = []
    return
  }

  const resp = await http.get<CommonResp<UserComment[]>>('/comment/public', {
    params: {
      // 评论目标类型
      targetType: 'doc',
      // 评论目标 id
      targetId: doc.id,
    },
  })
  comments.value = resp.data.content || []
}

const vote = async () => {
  if (!currentDoc.value?.id) {
    message.warning('请先选择一篇文档')
    return
  }

  const resp = await http.get<CommonResp<DocVoteStatus>>(`/doc/vote/${currentDoc.value.id}`)
  if (resp.data.success && resp.data.content) {
    voted.value = resp.data.content.voted
    currentDoc.value.voteCount = resp.data.content.voteCount
    message.success(voted.value ? '点赞成功' : '已取消点赞')
  }
}

const toggleFavorite = async () => {
  if (!currentDoc.value?.id) {
    message.warning('请先选择一篇文档')
    return
  }

  const resp = await http.post<CommonResp<FavoriteStatus>>('/favorite/toggle', {
    // 收藏目标类型
    targetType: 'doc',
    // 收藏目标 id
    targetId: currentDoc.value.id,
    // 收藏分组名称
    folderName: favoriteFolderName.value || '默认分组',
  })
  if (resp.data.success && resp.data.content) {
    favorited.value = resp.data.content.favorited
    favoriteFolderName.value = resp.data.content.folderName || favoriteFolderName.value
    message.success(resp.data.message)
  }
}

const submitComment = async () => {
  if (!currentDoc.value?.id) {
    message.warning('请先选择一篇文档')
    return
  }
  if (!commentContent.value.trim()) {
    message.warning('请先填写评论内容')
    return
  }

  const resp = await http.post<CommonResp<UserComment>>('/comment/submit', {
    // 评论目标类型
    targetType: 'doc',
    // 评论目标 id
    targetId: currentDoc.value.id,
    // 父评论 id，0 表示一级评论
    parentId: 0,
    // 评论正文
    content: commentContent.value.trim(),
  })
  if (resp.data.success) {
    message.success(resp.data.message)
    commentContent.value = ''
    await loadComments()
  }
}

const startReply = (comment: UserComment) => {
  replyParentId.value = comment.id
  replyContent.value = ''
}

const cancelReply = () => {
  replyParentId.value = undefined
  replyContent.value = ''
}

const submitReply = async () => {
  if (!currentDoc.value?.id || !replyParentId.value) {
    return
  }
  if (!replyContent.value.trim()) {
    message.warning('请先填写回复内容')
    return
  }

  const resp = await http.post<CommonResp<UserComment>>('/comment/submit', {
    // 回复目标类型
    targetType: 'doc',
    // 回复目标 id
    targetId: currentDoc.value.id,
    // 被回复的父评论 id
    parentId: replyParentId.value,
    // 回复正文
    content: replyContent.value.trim(),
  })
  if (resp.data.success) {
    message.success(resp.data.message)
    cancelReply()
    await loadComments()
  }
}

const canDeleteComment = (comment: UserComment) => {
  const user = loginUser.value
  if (!user || !comment.id) {
    return false
  }
  const isCommentOwner = String(comment.userId) === String(user.id)
  const isDocAuthor = currentDoc.value?.createUserId && String(currentDoc.value.createUserId) === String(user.id)
  const isCommentAdmin = isCurrentSuperAdmin() || hasPermission('comment:manage')
  return isCommentOwner || isDocAuthor || isCommentAdmin
}

const deleteComment = (comment: UserComment) => {
  if (!comment.id) {
    return
  }
  Modal.confirm({
    title: '确认删除这条评论吗？',
    content: '删除后这条评论不会继续展示在阅读页，后台仍会保留操作记录。',
    onOk: async () => {
      const resp = await http.delete<CommonResp<null>>(`/comment/delete/${comment.id}`)
      if (resp.data.success) {
        message.success('评论已删除')
        await loadComments()
      }
    },
  })
}

const openFeedback = () => {
  if (!currentDoc.value?.id) {
    message.warning('请先选择一篇文档')
    return
  }
  feedbackForm.targetType = 'doc'
  feedbackForm.targetId = currentDoc.value.id
  feedbackForm.type = 'correction'
  feedbackForm.title = `文档纠错：${currentDoc.value.name}`
  feedbackForm.content = ''
  feedbackOpen.value = true
}

const submitFeedback = async () => {
  if (!feedbackForm.title.trim() || !feedbackForm.content.trim()) {
    message.warning('请填写反馈标题和内容')
    return
  }

  const resp = await http.post<CommonResp<null>>('/feedback/submit', feedbackForm)
  if (resp.data.success) {
    message.success(resp.data.message)
    feedbackOpen.value = false
  }
}

const saveProgress = async () => {
  if (!currentDoc.value?.id) {
    return
  }
  await http.post<CommonResp<null>>('/history/progress', {
    // 当前正在阅读的文档 id
    docId: currentDoc.value.id,
    // 当前阅读进度百分比
    progress: readingProgress.value,
  })
}

const handleScroll = () => {
  updateReadingProgress()
}

const updateReadingProgress = () => {
  const contentElement = contentRef.value
  if (!contentElement || !content.value) {
    readingProgress.value = 0
    return
  }

  const rect = contentElement.getBoundingClientRect()
  const contentHeight = contentElement.scrollHeight
  const viewportHeight = window.innerHeight

  // 正文较短时不需要等待滚动，打开就认为已读完
  if (contentHeight <= viewportHeight * 0.9) {
    readingProgress.value = 100
    return
  }

  const readBottom = viewportHeight - rect.top
  readingProgress.value = Math.max(0, Math.min(100, Math.round((readBottom / contentHeight) * 100)))
}

const statusText = (status?: string) => {
  if (status === 'pending') return '待审核'
  if (status === 'rejected') return '已驳回'
  if (status === 'published') return '已发布'
  return status || '-'
}

let progressTimer: number | undefined

onMounted(() => {
  loadFavoriteFolders()
  loadDocs()
  window.addEventListener('scroll', handleScroll)
  progressTimer = window.setInterval(saveProgress, 12000)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (progressTimer) {
    window.clearInterval(progressTimer)
  }
  saveProgress()
})
</script>

<template>
  <div class="page read-page">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :md="7">
        <div class="panel">
          <a-typography-title :level="4">文档目录</a-typography-title>
          <a-input-search
            v-model:value="docKeyword"
            allow-clear
            class="doc-search"
            placeholder="搜索目录"
          />
          <div class="doc-catalog-list">
            <div
              v-for="record in visibleCatalogRows"
              :key="record.id"
              class="doc-catalog-row"
              :class="{ 'doc-row-active': String(record.id) === String(currentDoc?.id) }"
              :style="{ paddingLeft: `${record.level * 22}px` }"
            >
              <button
                type="button"
                class="doc-catalog-toggle"
                :class="{ empty: !record.hasChildren }"
                @click.stop="toggleCatalog(record)"
              >
                {{ record.hasChildren ? (isCatalogExpanded(record.id || 0) ? '▾' : '▸') : '' }}
              </button>
              <button
                type="button"
                class="doc-catalog-name"
                :class="{ 'parent-doc-name': record.hasChildren }"
                @click="openDoc(record as Doc)"
              >
                <span class="doc-catalog-index">{{ record.displayIndex }}</span>
                <span class="doc-catalog-title">{{ record.name }}</span>
              </button>
            </div>
            <a-empty v-if="!visibleCatalogRows.length" description="暂无目录" />
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :md="17">
        <div class="panel">
          <div class="toolbar">
            <div>
              <a-typography-title :level="3">
                {{ currentDoc?.name || '请选择文档' }}
              </a-typography-title>
              <div class="muted">
                阅读 {{ currentDoc?.viewCount || 0 }} · 点赞 {{ currentDoc?.voteCount || 0 }} · 进度 {{ readingProgress }}%
              </div>
            </div>
            <a-space>
              <a-select
                v-model:value="favoriteFolderName"
                class="favorite-folder"
                placeholder="收藏分组"
              >
                <a-select-option
                  v-for="folder in favoriteFolders"
                  :key="folder.id || folder.name"
                  :value="folder.name"
                >
                  {{ folder.name }}
                </a-select-option>
              </a-select>
              <a-button :type="favorited ? 'default' : 'primary'" @click="toggleFavorite">
                <StarFilled v-if="favorited" />
                <StarOutlined v-else />
                {{ favorited ? '取消收藏' : '收藏' }}
              </a-button>
              <a-button :type="voted ? 'default' : 'primary'" @click="vote">
                <LikeFilled v-if="voted" />
                <LikeOutlined v-else />
                {{ voted ? '取消点赞' : '点赞' }}
              </a-button>
            </a-space>
          </div>

          <a-spin :spinning="loading">
            <div v-if="content" ref="contentRef" class="html-content" v-html="content"></div>
            <a-empty v-else description="暂无正文内容" />
          </a-spin>
        </div>

        <div class="panel comment-panel">
          <div class="toolbar">
            <a-typography-title :level="4">
              <CommentOutlined />
              评论
            </a-typography-title>
            <a-button @click="openFeedback">提交纠错</a-button>
          </div>

          <a-textarea
            v-model:value="commentContent"
            :rows="4"
            maxlength="1000"
            show-count
            placeholder="这里可以写下你的评论"
          />
          <div class="comment-actions">
            <a-button type="primary" @click="submitComment">
              <SendOutlined />
              发布评论
            </a-button>
          </div>

          <a-list :data-source="comments" class="comment-list">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <span>{{ item.userName || '用户' }}</span>
                      <a-tag color="blue">{{ statusText(item.status) }}</a-tag>
                    </a-space>
                  </template>
                  <template #description>
                    <div class="comment-content">{{ item.content }}</div>
                    <div class="muted">{{ item.createTime }}</div>
                    <div class="comment-row-actions">
                      <a-button type="link" size="small" @click="startReply(item)">回复</a-button>
                      <a-button v-if="canDeleteComment(item)" type="link" size="small" danger @click="deleteComment(item)">
                        <DeleteOutlined />
                        删除
                      </a-button>
                    </div>
                    <div v-if="replyParentId === item.id" class="reply-box">
                      <a-textarea v-model:value="replyContent" :rows="3" maxlength="1000" show-count />
                      <a-space class="reply-actions">
                        <a-button size="small" type="primary" @click="submitReply">提交回复</a-button>
                        <a-button size="small" @click="cancelReply">取消</a-button>
                      </a-space>
                    </div>
                    <div v-if="item.children?.length" class="reply-list">
                      <div v-for="child in item.children" :key="child.id" class="reply-item">
                        <strong>{{ child.userName || '用户' }}</strong>
                        <span>{{ child.content }}</span>
                        <div class="reply-footer">
                          <span class="muted">{{ child.createTime }}</span>
                          <a-button v-if="canDeleteComment(child)" type="link" size="small" danger @click="deleteComment(child)">
                            <DeleteOutlined />
                            删除
                          </a-button>
                        </div>
                      </div>
                    </div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </a-col>
    </a-row>

    <a-modal v-model:open="feedbackOpen" title="提交纠错或建议" width="620px" @ok="submitFeedback">
      <a-form layout="vertical">
        <a-form-item label="反馈类型">
          <a-radio-group v-model:value="feedbackForm.type">
            <a-radio value="correction">内容纠错</a-radio>
            <a-radio value="suggestion">功能建议</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="标题">
          <a-input v-model:value="feedbackForm.title" />
        </a-form-item>
        <a-form-item label="内容">
          <a-textarea
            v-model:value="feedbackForm.content"
            :rows="5"
            maxlength="1000"
            show-count
            placeholder="这里可以写下你发现的问题，处理完毕会通过通知反馈给你"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.read-page {
  max-width: 1280px;
  margin: 0 auto;
}

.comment-panel {
  margin-top: 16px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  margin-top: 14px;
}

.comment-content {
  color: #1f2937;
  line-height: 1.8;
}

.html-content {
  color: #1f2937;
  line-height: 1.9;
}

.html-content :deep(table) {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.html-content :deep(th),
.html-content :deep(td) {
  min-width: 72px;
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  vertical-align: top;
  word-break: break-word;
}

.html-content :deep(th) {
  background: #f8fafc;
  font-weight: 700;
}

.html-content :deep(p) {
  margin: 0 0 12px;
}

.comment-row-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 4px;
}

.doc-search {
  margin-bottom: 12px;
}

.favorite-folder {
  width: 150px;
}

.doc-catalog-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.doc-catalog-row {
  display: flex;
  min-width: 0;
  align-items: center;
  border-radius: 6px;
}

.doc-catalog-row:hover,
.doc-catalog-row.doc-row-active {
  background: #e6f4ff;
}

.doc-catalog-toggle {
  display: inline-flex;
  flex: 0 0 24px;
  width: 24px;
  height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #475569;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
}

.doc-catalog-toggle.empty {
  cursor: default;
}

.doc-catalog-name {
  display: inline-flex;
  width: 100%;
  min-width: 0;
  gap: 8px;
  align-items: center;
  overflow: hidden;
  padding: 0;
  color: #334155;
  background: transparent;
  border: 0;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.doc-catalog-name:hover {
  color: #0958d9;
}

.parent-doc-name {
  color: #0f172a;
  font-weight: 700;
}

.doc-catalog-index {
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

.doc-catalog-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-box {
  margin-top: 8px;
}

.reply-actions {
  margin-top: 8px;
}

.reply-list {
  margin-top: 10px;
  padding-left: 14px;
  border-left: 3px solid #e5e7eb;
}

.reply-item {
  margin-bottom: 8px;
  color: #334155;
}

.reply-item span {
  margin-left: 8px;
}

.reply-footer {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 2px;
}
</style>
