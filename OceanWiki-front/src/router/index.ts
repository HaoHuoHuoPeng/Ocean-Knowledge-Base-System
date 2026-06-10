// 文件说明：这个文件负责前端路由配置，控制页面跳转、登录校验和权限校验。
import { createRouter, createWebHistory } from 'vue-router'

import { hasAnyPermission, hasPermission, isCurrentSuperAdmin, isLoggedIn } from '@/utils/auth'

// 前端路由表
// path 是浏览器地址，component 是这个地址要显示的 Vue 页面
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/doc',
      name: 'doc',
      component: () => import('@/views/ReadDocView.vue'),
    },
    {
      path: '/library',
      name: 'library',
      component: () => import('@/views/UserLibraryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/submit-doc',
      name: 'submit-doc',
      component: () => import('@/views/SubmitDocView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: () => import('@/views/StatisticsView.vue'),
      meta: { requiresAuth: true, permission: 'statistics:view' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      path: '/piano-tiles',
      name: 'piano-tiles',
      component: () => import('@/views/PianoTilesView.vue'),
      meta: { requiresAuth: true, permission: 'game:play' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: () => import('@/views/admin/AdminDashboardView.vue'),
      meta: { requiresAuth: true, superAdminOnly: true },
    },
    {
      path: '/admin/ebook',
      name: 'admin-ebook',
      component: () => import('@/views/admin/AdminEbookView.vue'),
      meta: { requiresAuth: true, anyPermission: ['ebook:manage', 'ebook:review'] },
    },
    {
      path: '/admin/category',
      name: 'admin-category',
      component: () => import('@/views/admin/AdminCategoryView.vue'),
      meta: { requiresAuth: true, permission: 'category:manage' },
    },
    {
      path: '/admin/doc',
      name: 'admin-doc',
      component: () => import('@/views/admin/AdminDocView.vue'),
      meta: { requiresAuth: true, anyPermission: ['doc:manage', 'doc:review'] },
    },
    {
      path: '/admin/user',
      name: 'admin-user',
      component: () => import('@/views/admin/AdminUserView.vue'),
      meta: { requiresAuth: true, permission: 'user:manage' },
    },
    {
      path: '/admin/role',
      name: 'admin-role',
      component: () => import('@/views/admin/AdminRoleView.vue'),
      meta: { requiresAuth: true, permission: 'role:manage' },
    },
    {
      path: '/admin/comment',
      name: 'admin-comment',
      component: () => import('@/views/admin/AdminCommentView.vue'),
      meta: { requiresAuth: true, permission: 'comment:manage' },
    },
    {
      path: '/admin/feedback',
      name: 'admin-feedback',
      component: () => import('@/views/admin/AdminFeedbackView.vue'),
      meta: { requiresAuth: true, permission: 'feedback:manage' },
    },
    {
      path: '/admin/sensitive-word',
      name: 'admin-sensitive-word',
      component: () => import('@/views/admin/AdminSensitiveWordView.vue'),
      meta: { requiresAuth: true, permission: 'sensitive:manage' },
    },
  ],
})

router.beforeEach((to) => {
  const isAuthPage = to.path === '/login' || to.path === '/register'

  // 除了登录和注册，其他页面都要求先登录
  // 这样打开前端根地址时，未登录用户会先看到登录页
  if (!isAuthPage && !isLoggedIn()) {
    return '/login'
  }

  const permission = to.meta.permission
  if (typeof permission === 'string' && !hasPermission(permission)) {
    return '/home'
  }

  const anyPermission = to.meta.anyPermission
  if (Array.isArray(anyPermission) && !hasAnyPermission(anyPermission as string[])) {
    return '/home'
  }

  if (to.meta.superAdminOnly && !isCurrentSuperAdmin()) {
    return '/home'
  }

  return true
})

export default router
