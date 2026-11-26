import type { AuthSession, LoginCredentials, UserProfile } from './api'
import { ApiError, createHttpClient } from '@xhlj/shared'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPortalIdentity, loginPortal, logoutPortal, refreshPortal } from './api'

const baseURL = import.meta.env.VITE_API_BASE_URL
const expiredMessage = '登录已失效，请重新登录'

export const useAuthStore = defineStore('portal-auth', () => {
  const user = ref<UserProfile | null>(null)
  const ready = ref(false)
  const accountError = ref('')
  const sessionMessage = ref('')
  const cookieClient = createHttpClient({ baseURL })
  let restoration: Promise<void> | undefined
  let session: {
    client: ReturnType<typeof createHttpClient>
    requests: AbortController
    accessToken: string | null
  } | undefined

  /** 启动时每个标签页只尝试一次恢复，受保护路由等待同一个结果。 */
  function initialize() {
    return restoration ??= restore().finally(() => {
      ready.value = true
    })
  }

  /** 使用登录 Cookie 建立会话，确认身份后由登录页跳转。 */
  async function login(credentials: LoginCredentials) {
    clear()
    await establish(await loginPortal(cookieClient, credentials))
  }

  /** 服务端确认退出后清理本地状态，失败时保留身份供用户重试。 */
  async function logout() {
    const current = session
    await logoutPortal(cookieClient)
    if (session === current)
      clear()
  }

  /** 查询当前账号，临时错误由账号页展示并允许重试。 */
  async function loadIdentity() {
    const current = session
    if (!current)
      return

    accountError.value = ''
    try {
      await confirmIdentity(current)
    }
    catch (error) {
      if (!current.requests.signal.aborted)
        accountError.value = accountErrorMessage(error)
    }
  }

  /** 重试看账号页时遇到的身份读取或恢复失败。 */
  async function retryAccount() {
    if (session)
      await loadIdentity()
    else
      await (restoration = restore())
  }

  /** 用 Cookie 恢复 Access Token，再通过 me 建立本地身份。 */
  async function restore() {
    accountError.value = ''
    try {
      await establish(await refreshPortal(cookieClient))
    }
    catch (error) {
      // 游客首次打开页面也会收到 UNAUTHENTICATED。
      if (isInvalidRefresh(error)) {
        if (error.code !== 'UNAUTHENTICATED')
          sessionMessage.value = expiredMessage
      }
      else {
        accountError.value = accountErrorMessage(error)
      }
    }
  }

  /** 为已签发的凭证创建专属客户端，并用 me 确认身份。 */
  async function establish(tokens: AuthSession) {
    // 1. 将私有请求与当前会话绑定，换号时旧客户端不会读取新令牌。
    const requests = new AbortController()
    const accountId = tokens.user.id
    const current: NonNullable<typeof session> = {
      accessToken: tokens.accessToken,
      requests,
      client: createHttpClient({
        baseURL,
        getAccessToken: () => current.accessToken,
        refreshAccessToken: async () => {
          const refreshed = await refreshPortal(cookieClient, requests.signal)
          requests.signal.throwIfAborted()

          // 多个标签页共享 Cookie。返回的身份不一致时不能替原账号重试。
          if (refreshed.user.id !== accountId) {
            clear('登录账号已变更，请重新登录')
            throw new Error('Portal account changed')
          }
          current.accessToken = refreshed.accessToken
          return refreshed.accessToken
        },
        onRefreshError: (error) => {
          if (isInvalidRefresh(error))
            clear(expiredMessage)
        },
        onUnauthorized: () => clear(expiredMessage),
      }),
    }
    session = current

    // 2. me 成功后再让页面读取身份。
    try {
      await confirmIdentity(current)
    }
    catch (error) {
      if (!requests.signal.aborted)
        clear()
      throw error
    }
  }

  /** 保留确认过的当前身份，旧会话响应不能写入 Store。 */
  async function confirmIdentity(current: NonNullable<typeof session>) {
    const profile = await getPortalIdentity(current.client, current.requests.signal)
    current.requests.signal.throwIfAborted()
    user.value = profile
  }

  /** 取消旧会话的私有请求并清除内存身份及凭证。 */
  function clear(message = '') {
    const previous = session
    session = undefined
    if (previous) {
      previous.accessToken = null
      previous.requests.abort()
    }
    user.value = null
    accountError.value = ''
    sessionMessage.value = message
  }

  return { user, ready, accountError, sessionMessage, initialize, loadIdentity, retryAccount, login, logout }
})

/** 只有服务端确认刷新凭证失效时，才结束当前登录。 */
function isInvalidRefresh(error: unknown): error is ApiError {
  return error instanceof ApiError && error.status === 401
    && ['UNAUTHENTICATED', 'REFRESH_TOKEN_EXPIRED', 'REFRESH_TOKEN_REPLAYED'].includes(error.code)
}

function accountErrorMessage(error: unknown) {
  return error instanceof ApiError && error.status && error.status < 500 ? error.message : '暂时无法读取账号，请稍后重试'
}
