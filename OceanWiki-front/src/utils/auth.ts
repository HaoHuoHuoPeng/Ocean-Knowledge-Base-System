// 文件说明：这个文件负责前端登录信息保存、token 读取和权限判断。
import type { LoginUser } from '@/types'

const LOGIN_USER_KEY = 'oceanwiki-login-user'

// 从浏览器本地缓存读取当前登录用户
export const getLoginUser = (): LoginUser | null => {
  const text = localStorage.getItem(LOGIN_USER_KEY)
  if (!text) {
    return null
  }

  try {
    return JSON.parse(text) as LoginUser
  } catch {
    localStorage.removeItem(LOGIN_USER_KEY)
    return null
  }
}

// 登录成功后保存用户信息
export const setLoginUser = (user: LoginUser) => {
  localStorage.setItem(LOGIN_USER_KEY, JSON.stringify(user))
}

// 退出登录时清空用户信息
export const clearLoginUser = () => {
  localStorage.removeItem(LOGIN_USER_KEY)
}

// 判断是否已经登录
export const isLoggedIn = () => {
  return !!getLoginUser()?.token
}

// 判断指定用户是否是超级管理员
export const isSuperAdminUser = (user: LoginUser | null) => {
  return user?.roleCodes?.includes('SUPER_ADMIN') || false
}

// 判断当前登录用户是否是超级管理员
export const isCurrentSuperAdmin = () => {
  return isSuperAdminUser(getLoginUser())
}

// 判断当前用户是否拥有某个权限
export const hasPermission = (permission: string) => {
  const user = getLoginUser()
  if (!user) {
    return false
  }
  return user.permissions?.includes(permission) || isSuperAdminUser(user)
}

// 判断当前用户是否拥有任意一个权限
// 适合一个页面允许多个角色进入的情况，比如电子书编辑员和电子书审核员都能进入电子书管理页
export const hasAnyPermission = (permissions: string[]) => {
  return permissions.some((permission) => hasPermission(permission))
}
