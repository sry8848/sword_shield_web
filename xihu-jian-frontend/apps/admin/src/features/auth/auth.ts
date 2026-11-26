import type { AdminCredentials, AdminProfile } from './api'
import { ApiError } from '@xhlj/shared'
import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'
import { getAdminCsrf, getAdminIdentity, loginAdmin, logoutAdmin } from './api'

export const adminRoleNames: Record<AdminProfile['roles'][number], string> = {
  SUPER_ADMIN: '系统管理员',
  EVENT_OPERATOR: '会务运营',
  CHECKIN_STAFF: '现场核验员',
}

export const useAdminAuth = defineStore('admin-auth', () => {
  const profile = shallowRef<AdminProfile | null>(null)
  const sessionError = shallowRef<{ traceId?: string } | null>(null)
  const sessionMessage = ref('')

  // 登录前的临时 token 在会话建立时轮换，当前 token 仅保存在内存。
  let csrfToken: string | undefined
  let sessionRequests = new AbortController()
  let identityCheck: Promise<void> | undefined

  /** 并发导航共享一次身份检查，返回后由守卫判断是否放行。 */
  function checkIdentity() {
    return identityCheck ??= readIdentity(sessionRequests.signal).finally(() => {
      identityCheck = undefined
    })
  }

  /** 建立后台会话，后续导航读取当前身份与登录后的 CSRF token。 */
  async function login(credentials: AdminCredentials) {
    const signal = sessionRequests.signal

    // 1. 取临时 token，再用表单中的凭证建立会话。
    const temporaryToken = await getAdminCsrf(signal)
    const identity = await loginAdmin(credentials, temporaryToken, signal)
    signal.throwIfAborted()

    // 2. 清理上一会话的请求和 token，记录新会话返回的身份。
    clearSession()
    profile.value = identity
  }

  /** 仅在服务端确认退出后清理身份，失败由调用方提示。 */
  async function logout() {
    const signal = sessionRequests.signal

    // 1. 用当前会话的 CSRF token 提交退出。
    try {
      await logoutAdmin(csrfToken ?? await getAdminCsrf(signal), signal)
    }
    catch (error) {
      // Cookie 由同一浏览器的标签页共享，手动重试时重新读取当前 token。
      if (error instanceof ApiError && error.status === 403 && !signal.aborted)
        csrfToken = undefined
      throw error
    }

    // 2. 退出确认后结束本地会话。
    signal.throwIfAborted()
    clearSession()
  }

  /** 读取当前角色和写请求所需的 token，区分失效与暂时无法确认。 */
  async function readIdentity(signal: AbortSignal) {
    try {
      // 1. 服务端返回当前身份后，获取本标签页需要的 CSRF token。
      const identity = await getAdminIdentity(signal)
      const token = csrfToken ?? await getAdminCsrf(signal)
      signal.throwIfAborted()
      csrfToken = token
      profile.value = identity
      sessionError.value = null
    }
    catch (error) {
      // 2. 结束已确认失效的会话，保留暂时无法确认的会话供重试。
      if (signal.aborted)
        return
      if (error instanceof ApiError && error.status === 401 && error.code === 'UNAUTHENTICATED') {
        clearSession(profile.value ? '登录已失效，请重新登录' : sessionMessage.value)
      }
      else if (error instanceof ApiError && error.status === 403 && error.code === 'ADMIN_ACCESS_REVOKED') {
        clearSession('你没有后台访问权限')
      }
      else {
        sessionError.value = { traceId: error instanceof ApiError ? error.traceId : undefined }
      }
    }
  }

  /** 取消旧会话请求，清空内存中的身份与 CSRF token。 */
  function clearSession(message = '') {
    sessionRequests.abort()
    sessionRequests = new AbortController()
    csrfToken = undefined
    profile.value = null
    sessionError.value = null
    sessionMessage.value = message
  }

  return { profile, sessionError, sessionMessage, checkIdentity, login, logout }
})
