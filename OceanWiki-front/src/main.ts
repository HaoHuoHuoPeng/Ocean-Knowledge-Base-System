// 文件说明：这是前端入口文件，负责创建 Vue 应用，并挂载路由、Pinia 和 Ant Design Vue。
import 'ant-design-vue/dist/reset.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'

import App from './App.vue'
import router from './router'

// 创建 Vue 应用实例
const app = createApp(App)

// Pinia 用来做全局状态管理，这里先保留，后面登录信息可以放进去
app.use(createPinia())

// 路由负责页面跳转，例如首页、后台管理、阅读页
app.use(router)

// Ant Design Vue 提供表格、表单、按钮、菜单等组件
app.use(Antd)

app.mount('#app')
