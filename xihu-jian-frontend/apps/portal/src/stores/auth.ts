import type { User } from '@xhlj/shared'
import { ApiError } from '@xhlj/shared'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '../api/client'

const mockMode = import.meta.env.VITE_USE_MOCK === 'true'
const tokenKey = 'xhlj.access-token'

export const useAuthStore = defineStore('portal-auth', () => {
  const user = ref<User | null>(null)
  // 真实会话的访问令牌只存在内存中，刷新页面后由 HttpOnly Cookie 恢复。
  const accessToken = ref(mockMode ? sessionStorage.getItem(tokenKey) : null)
  const isAuthenticated = computed(() => Boolean(accessToken.value && user.value))
  let hydration: Promise<void> | undefined

  /** 更新访问令牌，演示环境额外保存到当前标签页。 */
  function setAccessToken(token: string | null) {
    accessToken.value = token
    if (mockMode) {
      if (token)
        sessionStorage.setItem(tokenKey, token)
      else sessionStorage.removeItem(tokenKey)
    }
  }

  /** 清除本地身份和访问令牌。 */
  function clearSession() {
    user.value = null
    setAccessToken(null)
  }

  /** 用账号和密码建立门户会话。 */
  async function login(account: string, password: string) {
    const session = await api.auth.login({ account, password })
    setAccessToken(session.accessToken)
    user.value = session.user
  }

  /** 通过 Cookie 刷新指定会话，返回供 Axios 重试使用的新访问令牌。 */
  async function refresh(previousToken: string) {
    const session = await api.auth.refresh()
    if (accessToken.value !== previousToken)
      throw new ApiError('UNAUTHENTICATED', '登录会话已变更', 401)
    setAccessToken(session.accessToken)
    user.value = session.user
    return session.accessToken
  }

  /** 恢复登录态。首次导航共用一次恢复请求。 */
  function hydrate() {
    hydration ??= (async () => {
      try {
        // 1. 演示模式只恢复有本地令牌的会话，实际环境通过浏览器 Cookie 恢复。
        if (mockMode && !accessToken.value)
          return
        if (!accessToken.value) {
          const session = await api.auth.refresh()
          setAccessToken(session.accessToken)
          user.value = session.user
          return
        }
        // 2. 验证演示访问令牌并恢复用户资料。
        user.value = await api.auth.me()
      }
      catch (error) {
        clearSession()
        // 未登录是正常状态。其他恢复失败交由路由入口提示。
        if (!(error instanceof ApiError && (error.status === 401)))
          throw error
      }
    })().catch((error: unknown) => {
      hydration = undefined
      throw error
    })
    return hydration
  }

  /** 撤销服务端会话，令牌已失效时直接清理本地状态。 */
  async function logout() {
    try {
      await api.auth.logout()
    }
    catch (error) {
      if (!(error instanceof ApiError && error.status === 401))
        throw error
    }
    clearSession()
  }

  return { user, accessToken, isAuthenticated, login, refresh, hydrate, logout, clearSession }
})
