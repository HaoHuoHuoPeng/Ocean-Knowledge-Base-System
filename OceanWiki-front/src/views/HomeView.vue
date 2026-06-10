<script setup lang="ts">
// 文件说明：这个页面负责首页展示，包括分类树、电子书列表、推荐电子书、搜索和统计数据。
import { onMounted, ref } from 'vue'
import { BellOutlined, ClockCircleOutlined, CommentOutlined, EyeOutlined, LikeOutlined, ReadOutlined, SearchOutlined, StarOutlined } from '@ant-design/icons-vue'

import http from '@/api/http'
import type { Category, CommonResp, Ebook, IdValue, ReadingHistory, Statistic, UserComment, UserFavorite, UserNotice } from '@/types'
import { hasPermission, isLoggedIn } from '@/utils/auth'
import { arrayToTree } from '@/utils/tree'

const ebooks = ref<Ebook[]>([])
const recommendedEbooks = ref<Ebook[]>([])
const categoryTree = ref<Category[]>([])
const loading = ref(false)
const selectedCategoryId = ref<IdValue>()
const keyword = ref('')
const searchHistory = ref<string[]>(JSON.parse(localStorage.getItem('ocean-search-history') || '[]') as string[])
const latestHistory = ref<ReadingHistory | null>(null)
const canViewStatistic = hasPermission('statistics:view')
const userSummary = ref({
  // 当前用户收藏数量
  favoriteCount: 0,
  // 当前用户阅读历史数量
  historyCount: 0,
  // 当前用户待审核评论数量
  pendingCommentCount: 0,
  // 当前用户未读通知数量
  unreadNoticeCount: 0,
})

const statistic = ref<Statistic>({
  viewCount: 0,
  voteCount: 0,
  todayViewCount: 0,
  todayVoteCount: 0,
})

// 查询全部分类，前端使用 Ant Design Vue 的 a-tree 展示，支持任意层级分类。
const loadCategories = async () => {
  const resp = await http.get<CommonResp<Category[]>>('/category/all')
  categoryTree.value = arrayToTree(resp.data.content || [])
}

// 查询电子书。
// categoryId 有值时，后端会查询当前分类以及它下面所有子分类的电子书。
const loadEbooks = async (categoryId?: IdValue) => {
  loading.value = true
  try {
    const url = keyword.value.trim() ? '/ebook/search' : '/ebook/list'
    const resp = await http.get<CommonResp<Ebook[]>>(url, {
      params: {
        // 分类 id，后端按当前分类和所有子分类查询电子书
        categoryId,
        // 搜索关键词，后端按电子书名称或简介做模糊查询
        keyword: keyword.value.trim() || undefined,
      },
    })
    ebooks.value = resp.data.content || []
  } finally {
    loading.value = false
  }
}

// 查询统计数字。
// 普通用户没有 statistics:view 权限，所以不能无条件请求统计接口。
const loadStatistic = async () => {
  if (!canViewStatistic) {
    return
  }

  const resp = await http.get<CommonResp<Statistic>>('/ebookSnapshot/getStatistic')
  statistic.value = {
    viewCount: Number(resp.data.content?.viewCount || 0),
    voteCount: Number(resp.data.content?.voteCount || 0),
    todayViewCount: Number(resp.data.content?.todayViewCount || 0),
    todayVoteCount: Number(resp.data.content?.todayVoteCount || 0),
  }
}

// 查询推荐电子书。
// 后端会使用基于用户和基于物品的协同过滤计算推荐指数，首页只展示前三个。
const loadRecommendedEbooks = async () => {
  const resp = await http.get<CommonResp<Ebook[]>>('/ebook/recommend', {
    params: {
      // 推荐数量，首页只展示 top1、top2、top3
      limit: 3,
    },
  })
  recommendedEbooks.value = resp.data.content || []
}

// 查询最近一次阅读记录，用来展示“继续阅读”。
const loadLatestHistory = async () => {
  if (!isLoggedIn()) {
    latestHistory.value = null
    return
  }
  const resp = await http.get<CommonResp<ReadingHistory | null>>('/history/latest')
  latestHistory.value = resp.data.content || null
}

const loadUserSummary = async () => {
  if (!isLoggedIn()) {
    return
  }
  const [favoriteResp, historyResp, commentResp, noticeResp] = await Promise.all([
    http.get<CommonResp<UserFavorite[]>>('/favorite/my'),
    http.get<CommonResp<ReadingHistory[]>>('/history/my'),
    http.get<CommonResp<UserComment[]>>('/comment/my'),
    http.get<CommonResp<UserNotice[]>>('/notice/my'),
  ])
  userSummary.value = {
    favoriteCount: favoriteResp.data.content?.length || 0,
    historyCount: historyResp.data.content?.length || 0,
    pendingCommentCount: (commentResp.data.content || []).filter((item) => item.status === 'pending').length,
    unreadNoticeCount: (noticeResp.data.content || []).filter((item) => item.readFlag === 0).length,
  }
}

// 点击分类树以后，刷新右侧电子书列表。
const handleSelectCategory = (id?: IdValue) => {
  selectedCategoryId.value = id
  loadEbooks(id)
}

// 搜索电子书，同时把搜索词保存到本地搜索历史。
const handleSearch = () => {
  const text = keyword.value.trim()
  if (text) {
    searchHistory.value = [text, ...searchHistory.value.filter((item) => item !== text)].slice(0, 6)
    localStorage.setItem('ocean-search-history', JSON.stringify(searchHistory.value))
  }
  loadEbooks(selectedCategoryId.value)
}

const useSearchHistory = (text: string) => {
  keyword.value = text
  handleSearch()
}

// 最近阅读的文档被删除时，只展示历史信息，不再允许跳转
const canContinueRead = (record: ReadingHistory) => {
  return !!record.ebookId && !!record.docId && record.ebookName !== '电子书已删除' && record.docName !== '文档已删除'
}

// a-tree 的 select 事件返回的是选中的 key 数组，这里只取第一个分类 id。
const handleTreeSelect = (keys: IdValue[]) => {
  handleSelectCategory(keys[0])
}

onMounted(async () => {
  await loadCategories()
  await Promise.all([loadEbooks(), loadRecommendedEbooks(), loadStatistic(), loadLatestHistory(), loadUserSummary()])
})
</script>

<template>
  <div class="page home-page">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :md="6">
        <div class="panel">
          <a-typography-title :level="4">海洋分类</a-typography-title>
          <a-button block class="all-category-btn" @click="handleSelectCategory()">全部电子书</a-button>
          <a-tree
            class="category-tree"
            :tree-data="categoryTree"
            :field-names="{ title: 'name', key: 'id', children: 'children' }"
            :selected-keys="selectedCategoryId ? [selectedCategoryId] : []"
            default-expand-all
            block-node
            @select="handleTreeSelect"
          />
        </div>
      </a-col>

      <a-col :xs="24" :md="18">
        <div class="panel search-panel">
          <a-input-search
            v-model:value="keyword"
            size="large"
            placeholder="搜索电子书名称、简介或文档标题"
            enter-button="搜索"
            @search="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input-search>
          <div v-if="searchHistory.length" class="search-history">
            <span>搜索历史</span>
            <a-tag v-for="item in searchHistory" :key="item" class="history-tag" @click="useSearchHistory(item)">
              {{ item }}
            </a-tag>
          </div>
        </div>

        <div v-if="latestHistory" class="panel continue-panel">
          <div>
            <a-typography-title :level="4">继续阅读</a-typography-title>
            <div class="muted">{{ latestHistory.ebookName }} / {{ latestHistory.docName }}</div>
          </div>
          <router-link v-if="canContinueRead(latestHistory)" :to="`/doc?ebookId=${latestHistory.ebookId}&docId=${latestHistory.docId}`">
            <a-button type="primary">继续阅读</a-button>
          </router-link>
          <a-button v-else type="primary" disabled>继续阅读</a-button>
        </div>

        <div v-if="isLoggedIn()" class="panel user-summary-panel">
          <div class="section-title">
            <a-typography-title :level="4">我的阅读概览</a-typography-title>
            <router-link to="/library">
              <a-button type="link">进入我的书架</a-button>
            </router-link>
          </div>
          <a-row :gutter="[12, 12]">
            <a-col :xs="12" :lg="6">
              <div class="summary-item">
                <ClockCircleOutlined />
                <span>阅读记录</span>
                <strong>{{ userSummary.historyCount }}</strong>
              </div>
            </a-col>
            <a-col :xs="12" :lg="6">
              <div class="summary-item">
                <StarOutlined />
                <span>收藏资料</span>
                <strong>{{ userSummary.favoriteCount }}</strong>
              </div>
            </a-col>
            <a-col :xs="12" :lg="6">
              <div class="summary-item">
                <CommentOutlined />
                <span>待审核评论</span>
                <strong>{{ userSummary.pendingCommentCount }}</strong>
              </div>
            </a-col>
            <a-col :xs="12" :lg="6">
              <div class="summary-item">
                <BellOutlined />
                <span>未读通知</span>
                <strong>{{ userSummary.unreadNoticeCount }}</strong>
              </div>
            </a-col>
          </a-row>
        </div>

        <a-row v-if="canViewStatistic" :gutter="[16, 16]" class="stat-row">
          <a-col :xs="12" :lg="6">
            <a-statistic title="总阅读" :value="statistic.viewCount" />
          </a-col>
          <a-col :xs="12" :lg="6">
            <a-statistic title="总点赞" :value="statistic.voteCount" />
          </a-col>
          <a-col :xs="12" :lg="6">
            <a-statistic title="今日阅读" :value="statistic.todayViewCount" />
          </a-col>
          <a-col :xs="12" :lg="6">
            <a-statistic title="今日点赞" :value="statistic.todayVoteCount" />
          </a-col>
        </a-row>

        <div v-if="recommendedEbooks.length" class="panel recommend-panel">
          <div class="section-title">
            <a-typography-title :level="4">推荐电子书</a-typography-title>
<!--            采用协同过滤算法，user-based和item-based进行推荐-->
<!--            <div class="muted">根据相似用户和相似电子书计算推荐指数。</div>-->
          </div>
          <a-row :gutter="[12, 12]">
            <a-col v-for="(ebook, index) in recommendedEbooks" :key="ebook.id" :xs="24" :sm="12" :lg="8">
              <router-link class="recommend-item" :to="`/doc?ebookId=${ebook.id}`">
                <span class="top-badge">TOP {{ index + 1 }}</span>
                <img class="recommend-cover" :src="ebook.cover" :alt="ebook.name" />
                <div class="recommend-info">
                  <div class="recommend-name">{{ ebook.name }}</div>
                  <div class="recommend-desc">{{ ebook.description }}</div>
                  <a-space wrap>
                    <a-tag color="blue">推荐 {{ ebook.recommendScore || 0 }}</a-tag>
                    <a-tag>阅读 {{ ebook.viewCount || 0 }}</a-tag>
                    <a-tag>点赞 {{ ebook.voteCount || 0 }}</a-tag>
                  </a-space>
                </div>
              </router-link>
            </a-col>
          </a-row>
        </div>

        <a-spin :spinning="loading">
          <a-row :gutter="[16, 16]">
            <a-col v-for="ebook in ebooks" :key="ebook.id" :xs="24" :sm="12" :lg="8">
              <a-card hoverable class="ebook-card">
                <template #cover>
                  <img class="cover" :src="ebook.cover" :alt="ebook.name" />
                </template>
                <a-card-meta :title="ebook.name">
                  <template #description>
                    <div class="desc">{{ ebook.description }}</div>
                  </template>
                </a-card-meta>
                <div class="ebook-meta">
                  <span><ReadOutlined /> {{ ebook.docCount || 0 }}</span>
                  <span><EyeOutlined /> {{ ebook.viewCount || 0 }}</span>
                  <span><LikeOutlined /> {{ ebook.voteCount || 0 }}</span>
                </div>
                <router-link :to="`/doc?ebookId=${ebook.id}`">
                  <a-button type="primary" block>开始阅读</a-button>
                </router-link>
              </a-card>
            </a-col>
          </a-row>
        </a-spin>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1280px;
  margin: 0 auto;
}

.stat-row {
  margin-bottom: 16px;
}

.all-category-btn {
  margin-bottom: 10px;
}

.category-tree {
  background: transparent;
}

.section-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.recommend-panel {
  margin-bottom: 16px;
}

.recommend-item {
  position: relative;
  display: grid;
  grid-template-columns: 88px 1fr;
  min-height: 118px;
  gap: 12px;
  padding: 10px;
  color: inherit;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.top-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 1;
  padding: 2px 7px;
  color: #fff;
  background: #1677ff;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 800;
}

.recommend-cover {
  width: 88px;
  height: 98px;
  object-fit: cover;
  border-radius: 6px;
}

.recommend-info {
  min-width: 0;
}

.recommend-name {
  margin-bottom: 4px;
  color: #111827;
  font-size: 15px;
  font-weight: 700;
}

.recommend-desc {
  display: -webkit-box;
  height: 38px;
  margin-bottom: 8px;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.search-panel {
  margin-bottom: 16px;
}

.search-history {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 12px;
  color: #64748b;
}

.history-tag {
  cursor: pointer;
}

.continue-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.user-summary-panel {
  margin-bottom: 16px;
}

.summary-item {
  display: grid;
  grid-template-columns: 26px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.summary-item :deep(.anticon) {
  color: #1677ff;
  font-size: 20px;
}

.summary-item span {
  color: #475569;
}

.summary-item strong {
  color: #0f172a;
  font-size: 18px;
}

.stat-row :deep(.ant-col) {
  background: #fff;
  border-radius: 8px;
}

.ebook-card {
  height: 100%;
}

.desc {
  min-height: 66px;
  color: #5f6b7a;
}

.ebook-meta {
  display: flex;
  gap: 14px;
  margin: 14px 0;
  color: #667085;
}
</style>
