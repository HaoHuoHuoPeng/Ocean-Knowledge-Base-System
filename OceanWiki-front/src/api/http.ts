// 文件说明：这个文件负责封装 Axios 请求，统一加 token 请求头和接口错误提示。
import axios from 'axios'
import { message } from 'ant-design-vue'

import type { CommonResp } from '@/types'
import { getLoginUser } from '@/utils/auth'

// 创建 axios 实例
// baseURL 留空表示请求当前前端地址，再由 vite.config.ts 里的 proxy 转发给后端
const http = axios.create({
  baseURL: '',
  timeout: 10000,
})

// 请求拦截器：每次请求发出前都会经过这里
http.interceptors.request.use((config) => {
  const user = getLoginUser()
  if (user?.token) {
    config.headers.Authorization = user.token
  }

  return config
})

// 响应拦截器：统一处理后端 CommonResp
http.interceptors.response.use(
  (response) => {
    const data = response.data as CommonResp<unknown>

    // 后端返回 success=false 时，在页面右上角给出提示
    if (data && data.success === false) {
      message.error(data.message || '请求失败')
    }

    return response
  },
  (error) => {
    message.error('请求后端失败，请确认后端项目已经启动')
    return Promise.reject(error)
  },
)

export default http
