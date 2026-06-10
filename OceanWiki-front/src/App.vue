<script setup lang="ts">
// 文件说明：这是前端根组件，负责整体布局、顶部菜单、用户下拉菜单和页面出口。
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChartOutlined,
  BellOutlined,
  BookOutlined,
  ClockCircleOutlined,
  CommentOutlined,
  DownOutlined,
  HomeOutlined,
  KeyOutlined,
  LoginOutlined,
  LogoutOutlined,
  MessageOutlined,
  FileAddOutlined,
  PlayCircleOutlined,
  ReadOutlined,
  StarOutlined,
  StopOutlined,
  TagsOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'

import { clearLoginUser, getLoginUser, hasAnyPermission, hasPermission, isSuperAdminUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const loginUser = ref(getLoginUser())

// 当前顶部菜单选中项
const selectedKeys = computed(() => [route.path])
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

const can = (permission: string) => {
  return hasPermission(permission)
}

const canAny = (permissions: string[]) => {
  return hasAnyPermission(permissions)
}

const isSuperAdmin = computed(() => isSuperAdminUser(loginUser.value))
const ebookMenuTitle = computed(() => {
  const user = loginUser.value
  const permissions = user?.permissions || []
  const canManage = permissions.includes('ebook:manage') || isSuperAdminUser(user)
  const canReview = permissions.includes('ebook:review') || isSuperAdminUser(user)
  return canReview && !canManage ? '电子书审核' : '电子书'
})

const commentMenuTitle = computed(() => {
  const user = loginUser.value
  const onlyReviewer = user?.roleCodes?.includes('CONTENT_REVIEWER') && !isSuperAdminUser(user)
  return onlyReviewer ? '评论审核' : '评论'
})

const feedbackMenuTitle = computed(() => {
  const user = loginUser.value
  const onlyReviewer = user?.roleCodes?.includes('CONTENT_REVIEWER') && !isSuperAdminUser(user)
  return onlyReviewer ? '反馈审核' : '反馈'
})

const docMenuTitle = computed(() => {
  const user = loginUser.value
  const permissions = user?.permissions || []
  const canManage = permissions.includes('doc:manage') || isSuperAdminUser(user)
  const canReview = permissions.includes('doc:review') || isSuperAdminUser(user)
  return canReview && !canManage ? '文档审核' : '文档'
})

const logout = async () => {
  clearLoginUser()
  loginUser.value = null
  await router.push('/login')
}

watch(
  () => route.fullPath,
  () => {
    loginUser.value = getLoginUser()
  },
)
</script>

<template>
  <a-layout class="app-layout">
    <template v-if="!isAuthPage">
      <a-layout-header class="app-header">
        <router-link class="brand" to="/home">
          <ReadOutlined />
          <span>OceanWiki</span>
        </router-link>

        <a-menu class="app-menu" mode="horizontal" theme="dark" :selected-keys="selectedKeys">
          <a-menu-item v-if="isSuperAdmin" key="/admin/dashboard">
            <router-link to="/admin/dashboard">
              <BarChartOutlined />
              <span>看板</span>
            </router-link>
          </a-menu-item>
          <a-menu-item key="/home">
            <router-link to="/home">
              <HomeOutlined />
              <span>首页</span>
            </router-link>
          </a-menu-item>
          <a-menu-item key="/library">
            <router-link to="/library">
              <StarOutlined />
              <span>我的书架</span>
            </router-link>
          </a-menu-item>
          <a-menu-item key="/submit-doc">
            <router-link to="/submit-doc">
              <FileAddOutlined />
              <span>发文档</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('statistics:view')" key="/statistics">
            <router-link to="/statistics">
              <BarChartOutlined />
              <span>统计</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="canAny(['ebook:manage', 'ebook:review'])" key="/admin/ebook">
            <router-link to="/admin/ebook">
              <BookOutlined />
              <span>{{ ebookMenuTitle }}</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('category:manage')" key="/admin/category">
            <router-link to="/admin/category">
              <TagsOutlined />
              <span>分类</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="canAny(['doc:manage', 'doc:review'])" key="/admin/doc">
            <router-link to="/admin/doc">
              <ReadOutlined />
              <span>{{ docMenuTitle }}</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('comment:manage')" key="/admin/comment">
            <router-link to="/admin/comment">
              <CommentOutlined />
              <span>{{ commentMenuTitle }}</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('feedback:manage')" key="/admin/feedback">
            <router-link to="/admin/feedback">
              <MessageOutlined />
              <span>{{ feedbackMenuTitle }}</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('user:manage')" key="/admin/user">
            <router-link to="/admin/user">
              <TeamOutlined />
              <span>用户</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('role:manage')" key="/admin/role">
            <router-link to="/admin/role">
              <KeyOutlined />
              <span>角色权限</span>
            </router-link>
          </a-menu-item>
          <a-menu-item v-if="can('sensitive:manage')" key="/admin/sensitive-word">
            <router-link to="/admin/sensitive-word">
              <StopOutlined />
              <span>敏感词</span>
            </router-link>
          </a-menu-item>
          <a-menu-item key="/piano-tiles" class="right-menu-item">
            <router-link to="/piano-tiles">
              <PlayCircleOutlined />
              <span>小游戏</span>
            </router-link>
          </a-menu-item>
        </a-menu>

        <div class="app-actions">
          <template v-if="!loginUser">
            <router-link to="/login">
              <a-button size="small" type="primary" ghost>
                <LoginOutlined />
                登录
              </a-button>
            </router-link>
            <router-link to="/register">
              <a-button size="small" ghost>
                <UserAddOutlined />
                注册
              </a-button>
            </router-link>
          </template>
          <template v-else>
            <a-dropdown placement="bottomRight">
              <button class="user-trigger">
                <UserOutlined />
                <span>{{ loginUser.name }}</span>
                <DownOutlined />
              </button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="profile-account">
                    <router-link :to="{ path: '/profile', query: { tab: 'account' } }">
                      <UserOutlined />
                      <span>账号资料</span>
                    </router-link>
                  </a-menu-item>
                  <a-menu-item key="profile-favorites">
                    <router-link :to="{ path: '/profile', query: { tab: 'favorites' } }">
                      <StarOutlined />
                      <span>我的收藏</span>
                    </router-link>
                  </a-menu-item>
                  <a-menu-item key="profile-history">
                    <router-link :to="{ path: '/profile', query: { tab: 'history' } }">
                      <ClockCircleOutlined />
                      <span>阅读历史</span>
                    </router-link>
                  </a-menu-item>
                  <a-menu-item key="profile-comments">
                    <router-link :to="{ path: '/profile', query: { tab: 'comments' } }">
                      <CommentOutlined />
                      <span>我的评论</span>
                    </router-link>
                  </a-menu-item>
                  <a-menu-item key="profile-feedback">
                    <router-link :to="{ path: '/profile', query: { tab: 'feedback' } }">
                      <MessageOutlined />
                      <span>我的反馈</span>
                    </router-link>
                  </a-menu-item>
                  <a-menu-item key="profile-notices">
                    <router-link :to="{ path: '/profile', query: { tab: 'notices' } }">
                      <BellOutlined />
                      <span>通知</span>
                    </router-link>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="logout">
                    <LogoutOutlined />
                    <span>退出登录</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </div>
      </a-layout-header>
    </template>

    <a-layout-content :class="isAuthPage ? 'auth-content' : 'app-content'">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 22px;
}

.brand {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.app-menu {
  flex: 1;
  min-width: 0;
}

:deep(.right-menu-item) {
  margin-left: auto;
}

.app-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
}

.user-trigger {
  display: inline-flex;
  height: 28px;
  gap: 6px;
  align-items: center;
  max-width: 120px;
  padding: 0 8px;
  overflow: hidden;
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.user-trigger span {
  overflow: hidden;
  color: #fff;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-content {
  min-height: calc(100vh - 64px);
}

.auth-content {
  min-height: 100vh;
}

@media (max-width: 760px) {
  .app-header {
    height: auto;
    min-height: 64px;
    flex-direction: column;
    align-items: flex-start;
    gap: 0;
    padding: 10px 14px;
  }

  .app-menu {
    width: 100%;
  }

  .app-actions {
    width: 100%;
    padding-top: 8px;
  }
}
</style>
