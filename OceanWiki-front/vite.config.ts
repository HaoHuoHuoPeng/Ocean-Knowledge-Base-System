// 文件说明：这个配置文件负责 Vite 开发服务、路径别名和后端代理转发。
import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置文件
// server.proxy 的作用：前端请求 /ebook/list 时，开发服务器会转发到后端 8881
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 8900,
    proxy: {
      '/ebook': 'http://127.0.0.1:8881',
      '/category': 'http://127.0.0.1:8881',
      '/doc': 'http://127.0.0.1:8881',
      '/content': 'http://127.0.0.1:8881',
      '/user': 'http://127.0.0.1:8881',
      '/role': 'http://127.0.0.1:8881',
      '/permission': 'http://127.0.0.1:8881',
      '/dashboard': 'http://127.0.0.1:8881',
      '/ebookSnapshot': 'http://127.0.0.1:8881',
      '/game': 'http://127.0.0.1:8881',
      '/favorite': 'http://127.0.0.1:8881',
      '/history': 'http://127.0.0.1:8881',
      '/comment': 'http://127.0.0.1:8881',
      '/feedback': 'http://127.0.0.1:8881',
      '/notice': 'http://127.0.0.1:8881',
      '/operationLog': 'http://127.0.0.1:8881',
      '/sensitiveWord': 'http://127.0.0.1:8881',
      '/upload': 'http://127.0.0.1:8881',
      '/uploads': 'http://127.0.0.1:8881',
    },
  },
})
