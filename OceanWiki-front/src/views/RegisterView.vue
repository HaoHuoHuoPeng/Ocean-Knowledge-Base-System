<script setup lang="ts">
// 文件说明：这个页面负责新用户注册，注册后默认是普通用户。
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOutlined,
  CommentOutlined,
  LockOutlined,
  StarOutlined,
  UserAddOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import http from '@/api/http'
import type { CommonResp } from '@/types'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  // 注册账号，后端会校验是否重复
  loginName: '',
  // 用户姓名，注册成功后用于页面展示
  name: '',
  // 注册密码，后端会加密后保存
  password: '',
  // 确认密码，只在前端校验，不提交给后端
  confirmPassword: '',
})

const canSubmit = computed(() => {
  return Boolean(form.loginName.trim() && form.name.trim() && form.password && form.confirmPassword)
})

const validateForm = () => {
  if (!form.loginName.trim()) {
    message.warning('请输入账号')
    return false
  }
  if (form.loginName.trim().length < 3) {
    message.warning('账号至少 3 个字符')
    return false
  }
  if (!form.name.trim()) {
    message.warning('请输入姓名')
    return false
  }
  if (form.password.length < 6) {
    message.warning('密码至少 6 位')
    return false
  }
  if (form.password !== form.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return false
  }
  return true
}

const register = async () => {
  if (!validateForm()) {
    return
  }

  loading.value = true
  try {
    const resp = await http.post<CommonResp<null>>('/user/register', {
      // 注册账号，后端会用它创建登录账号
      loginName: form.loginName.trim(),
      // 用户姓名，注册后展示在页面右上角和评论等位置
      name: form.name.trim(),
      // 原始密码，后端接收后会加密保存
      password: form.password,
    })
    if (resp.data.success) {
      message.success('注册成功，默认角色是普通用户')
      await router.push('/login')
    }
  } finally {
    loading.value = false
  }
}
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
        <a-typography-title :level="1">创建账号，开始你的海洋知识阅读</a-typography-title>
        <p>注册后即可享受阅读电子书、收藏文档、提交评论和反馈等功能。</p>
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
          <CommentOutlined />
          <span>评论反馈</span>
        </div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <div class="auth-form-panel">
        <div class="form-heading">
          <a-typography-title :level="2">注册账号</a-typography-title>
<!--          <div class="muted">注册后默认分配普通用户角色，管理员可在后台调整。</div>-->
        </div>

        <a-form layout="vertical" @submit.prevent="register">
          <a-form-item label="账号">
            <a-input
              v-model:value="form.loginName"
              size="large"
              allow-clear
              placeholder="例如：student01"
              @press-enter="register"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item label="昵称">
            <a-input
              v-model:value="form.name"
              size="large"
              allow-clear
              placeholder="请输入昵称"
              @press-enter="register"
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
              placeholder="至少 6 位"
              @press-enter="register"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item label="确认密码">
            <a-input-password
              v-model:value="form.confirmPassword"
              size="large"
              placeholder="请再次输入密码"
              @press-enter="register"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-button block size="large" type="primary" :loading="loading" :disabled="!canSubmit" @click="register">
            <UserAddOutlined />
            注册
          </a-button>
        </a-form>

        <div class="auth-footer">
          <span>已有账号？</span>
          <router-link to="/login">返回登录</router-link>
        </div>
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
    url('http://oceanwiki.oss-cn-guangzhou.aliyuncs.com/image/shuimu.jpg') center / cover;
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

.auth-footer {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 18px;
  color: #64748b;
}

.auth-footer a {
  color: #1677ff;
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
