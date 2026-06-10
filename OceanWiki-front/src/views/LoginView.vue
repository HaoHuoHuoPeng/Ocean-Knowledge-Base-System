<script setup lang="ts">
// 文件说明：这个页面负责用户登录，登录成功后保存 token 和用户信息。
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOutlined,
  ClockCircleOutlined,
  LockOutlined,
  LoginOutlined,
  StarOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp, LoginUser } from '@/types'
import { isSuperAdminUser, setLoginUser } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const rememberAccount = ref(true)

const form = reactive({
  // 账号，提交给 /user/login 用来定位登录用户
  loginName: '',
  // 密码，只提交给后端校验，不保存到本地
  password: '',
})

// 页面打开时恢复上次记住的账号，密码不做本地保存。
const loadRememberedAccount = () => {
  form.loginName = localStorage.getItem('oceanwiki-login-name') || ''
}

const validateForm = () => {
  if (!form.loginName.trim()) {
    message.warning('请输入账号')
    return false
  }
  if (!form.password) {
    message.warning('请输入密码')
    return false
  }
  return true
}

const login = async () => {
  if (!validateForm()) {
    return
  }

  loading.value = true
  try {
    const resp = await http.post<CommonResp<LoginUser>>('/user/login', {
      // 账号，后端用它查询用户
      loginName: form.loginName.trim(),
      // 密码，后端用它和数据库密码做校验
      password: form.password,
    })
    if (resp.data.success && resp.data.content) {
      const loginUser = resp.data.content
      if (rememberAccount.value) {
        localStorage.setItem('oceanwiki-login-name', form.loginName.trim())
      } else {
        localStorage.removeItem('oceanwiki-login-name')
      }
      setLoginUser(loginUser)
      message.success('登录成功')
      // 超级管理员登录后先进入后台看板，普通用户仍然进入首页阅读电子书。
      await router.push(isSuperAdminUser(loginUser) ? '/admin/dashboard' : '/home')
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadRememberedAccount)
</script>

<template>
  <div class="auth-shell">
    <section class="auth-visual">
      <div class="brand-line">
        <div class="brand-mark">
          <BookOutlined />
        </div>
        <div>
          <div class="brand-name">OceanWiki</div>
          <div class="brand-desc">海洋知识库管理系统</div>
        </div>
      </div>

      <div class="visual-copy">
        <a-typography-title :level="1">沉淀海洋资料，连接阅读与知识</a-typography-title>
        <p>面向电子书、文档、分类、评论、反馈和权限管理的统一平台。</p>
      </div>

      <div class="visual-stats">
        <div class="visual-stat">
          <BookOutlined />
          <span>阅读电子书</span>
        </div>
        <div class="visual-stat">
          <StarOutlined />
          <span>收藏资料</span>
        </div>
        <div class="visual-stat">
          <ClockCircleOutlined />
          <span>继续阅读</span>
        </div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <div class="auth-form-panel">
        <div class="form-heading">
          <a-typography-title :level="2">账号登录</a-typography-title>
<!--          <div class="muted">登录后即可享受完整功能</div>-->
        </div>

        <a-form layout="vertical" @submit.prevent="login">
          <a-form-item label="账号">
            <a-input
              v-model:value="form.loginName"
              size="large"
              allow-clear
              placeholder="请输入账号"
              @press-enter="login"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item label="密码">
            <a-input-password
              v-model:value="form.password"
              size="large"
              placeholder="请输入密码"
              @press-enter="login"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="form-options">
            <a-checkbox v-model:checked="rememberAccount">记住账号</a-checkbox>
            <router-link to="/register">注册新账号</router-link>
          </div>

          <a-button block size="large" type="primary" :loading="loading" @click="login">
            <LoginOutlined />
            登录
          </a-button>
        </a-form>

<!--        <div class="auth-footer">
          <span>新用户注册后默认分配普通用户角色。</span>
        </div>-->
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(420px, 1.1fr) minmax(420px, 0.9fr);
  background: #f3f6fb;
}

.auth-visual {
  position: relative;
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  justify-content: space-between;
  padding: 42px;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(90deg, rgba(7, 32, 54, 0.88), rgba(13, 78, 112, 0.66)),
    url('http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/haitun.jpg') center / cover;
}

.auth-visual::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 160px;
  background: linear-gradient(180deg, transparent, rgba(7, 32, 54, 0.55));
  content: '';
}

.brand-line,
.visual-copy,
.visual-stats {
  position: relative;
  z-index: 1;
}

.brand-line {
  display: flex;
  gap: 12px;
  align-items: center;
}

.brand-mark {
  display: inline-flex;
  width: 46px;
  height: 46px;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  font-size: 24px;
}

.brand-name {
  font-size: 22px;
  font-weight: 800;
}

.brand-desc {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.visual-copy {
  max-width: 760px;
}

.visual-copy :deep(.ant-typography) {
  margin-bottom: 14px;
  color: #fff;
  font-size: 34px;
  line-height: 1.18;
  white-space: nowrap;
}

.visual-copy p {
  max-width: 520px;
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
  line-height: 1.9;
}

.visual-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.visual-stat {
  display: flex;
  min-height: 72px;
  gap: 10px;
  align-items: center;
  padding: 14px;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #fff;
}

.visual-stat :deep(.anticon) {
  font-size: 21px;
}

.auth-form-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px;
}

.auth-form-panel {
  width: 100%;
  max-width: 430px;
  padding: 34px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.1);
}

.form-heading {
  margin-bottom: 24px;
}

.form-heading :deep(.ant-typography) {
  margin-bottom: 8px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 18px;
}

.form-options a {
  color: #1677ff;
}

.auth-footer {
  margin-top: 18px;
  color: #64748b;
  text-align: center;
}

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 300px;
    padding: 28px;
  }

  .visual-copy :deep(.ant-typography) {
    font-size: 30px;
  }

  .visual-stats {
    grid-template-columns: 1fr;
  }

  .auth-form-wrap {
    padding: 24px;
  }
}
</style>
