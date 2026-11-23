import type { LoginCredentials, UserProfile } from './api'
import { createHttpClient } from '@xhlj/shared'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPortalIdentity, loginPortal } from './api'

export const useAuthStore = defineStore('portal-auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<UserProfile | null>(null)
  const client = createHttpClient({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    getAccessToken: () => accessToken.value,
    onUnauthorized: clear,
  })

  /** 建立会话并通过 me 确认身份后，允许导航进入受保护页面。 */
  async function login(credentials: LoginCredentials) {
    // 1. 保存只能用于当前应用生命周期的 Access Token。
    const session = await loginPortal(client, credentials)
    accessToken.value = session.accessToken

    // 2. 身份查询失败时，丢弃没有通过校验的新登录态。
    try {
      user.value = await getPortalIdentity(client)
    }
    catch (error) {
      clear()
      throw error
    }
  }

  /** 清除当前应用内存中的身份与访问凭证。 */
  function clear() {
    accessToken.value = null
    user.value = null
  }

  return { login, user }
})
