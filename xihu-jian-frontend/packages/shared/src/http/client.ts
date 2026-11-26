import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import type { ApiEnvelope, ApiErrorResponse } from '../types/api'
import axios from 'axios'

export interface HttpClientOptions {
  baseURL?: string
  timeout?: number
  getAccessToken?: () => string | null
  refreshAccessToken?: (expiredToken: string) => Promise<string>
  onUnauthorized?: () => void
  onRefreshError?: (error: unknown) => void
}

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status?: number,
    public traceId?: string,
    public timestamp?: string,
    public title?: string,
    public errors?: ApiErrorResponse['errors'],
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/** 创建 Axios 客户端，统一注入访问令牌并在令牌过期时重试原请求。 */
export function createHttpClient(options: HttpClientOptions = {}): AxiosInstance {
  const client = axios.create({
    baseURL: options.baseURL ?? '',
    timeout: options.timeout ?? 15_000,
  })

  // 并发 401 共用一次刷新。记录上次换发的令牌，用于处理晚到的旧请求响应。
  let refreshing: Promise<string> | undefined
  let refreshedToken: { previous: string, current: string } | undefined

  // 1. 只为受保护接口添加 Bearer。认证和公共接口显式禁用该请求头。
  client.interceptors.request.use((config) => {
    const token = options.getAccessToken?.()
    if (token && config.headers.get('Authorization') !== false) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
  })

  // 2. 区分访问令牌过期和其他 401。仅在原请求带了 Bearer 时处理。
  client.interceptors.response.use(undefined, async (error: unknown) => {
    if (!axios.isAxiosError<ApiErrorResponse>(error) || error.response?.status !== 401 || !error.config)
      throw error
    const config = error.config as typeof error.config & { retriedAfterRefresh?: boolean }
    const authorization = config.headers.get('Authorization')
    const token = options.getAccessToken?.()
    if (!token || typeof authorization !== 'string')
      throw error

    // 旧会话的响应不能清除新会话，也不能用另一账号的令牌重放。
    const usesCurrentToken = authorization === `Bearer ${token}`
    const usesJustRefreshedToken = refreshedToken?.current === token
      && authorization === `Bearer ${refreshedToken.previous}`
    if (!usesCurrentToken && !usesJustRefreshedToken)
      throw error

    if (config.retriedAfterRefresh || error.response.data?.code !== 'ACCESS_TOKEN_EXPIRED' || !options.refreshAccessToken) {
      if (usesCurrentToken)
        options.onUnauthorized?.()
      throw error
    }

    // 3. 刷新成功后最多重试一次。由应用判断刷新错误是否结束会话。
    if (usesCurrentToken) {
      refreshing ??= options.refreshAccessToken(token)
        .then((current) => {
          refreshedToken = { previous: token, current }
          return current
        })
        .catch((refreshError: unknown) => {
          if (options.getAccessToken?.() === token) {
            const onRefreshError = options.onRefreshError ?? options.onUnauthorized
            onRefreshError?.(refreshError)
          }
          throw refreshError
        })
        .finally(() => { refreshing = undefined })
      const current = await refreshing
      if (options.getAccessToken?.() !== current)
        throw error
    }
    config.retriedAfterRefresh = true
    return client.request(config)
  })

  return client
}

/** 发起契约请求，返回成功响应的 data，或抛出带 HTTP 状态和错误码的 ApiError。 */
export async function request<T>(client: AxiosInstance, config: AxiosRequestConfig): Promise<T> {
  try {
    // 1. 解析成功响应，保留非预期响应中的追踪信息。
    const { data: envelope, status } = await client.request<ApiEnvelope<T>>(config)
    if (envelope?.code !== 'SUCCESS' || !('data' in envelope)) {
      throw new ApiError(envelope?.code ?? 'INVALID_RESPONSE', envelope?.message ?? '服务器返回了无法识别的数据', status, envelope?.traceId, envelope?.timestamp)
    }
    return envelope.data
  }
  catch (error) {
    // 2. 保留服务端的业务错误，区分网络错误和非契约的 HTTP 响应。
    if (axios.isAxiosError<ApiErrorResponse>(error)) {
      if (axios.isCancel(error))
        throw error
      const response = error.response
      const payload = response?.data
      throw new ApiError(
        payload?.code ?? (response ? 'INVALID_RESPONSE' : 'NETWORK_ERROR'),
        payload?.detail ?? payload?.message ?? (response ? '服务器返回了无法识别的数据' : '无法连接服务，请稍后重试'),
        response?.status,
        payload?.traceId,
        payload?.timestamp,
        payload?.title,
        payload?.errors,
      )
    }
    throw error
  }
}
