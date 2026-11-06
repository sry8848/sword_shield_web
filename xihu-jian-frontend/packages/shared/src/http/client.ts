import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import type { ApiEnvelope } from '../types/api'

export interface HttpClientOptions {
  baseURL?: string
  timeout?: number
  getAccessToken?: () => string | null
  onUnauthorized?: () => void
}

export class ApiError extends Error {
  code: string
  status?: number
  traceId?: string

  constructor(code: string, message: string, status?: number, traceId?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.traceId = traceId
  }
}

export function createHttpClient(options: HttpClientOptions = {}): AxiosInstance {
  const client = axios.create({
    baseURL: options.baseURL ?? '',
    timeout: options.timeout ?? 15_000,
    headers: { 'Content-Type': 'application/json' },
  })

  client.interceptors.request.use((config) => {
    const token = options.getAccessToken?.()
    if (token) config.headers.set('Authorization', `Bearer ${token}`)
    return config
  })

  client.interceptors.response.use(
    response => response,
    (error: unknown) => {
      if (axios.isAxiosError(error) && error.response?.status === 401) options.onUnauthorized?.()
      return Promise.reject(error)
    },
  )

  return client
}

export async function request<T>(client: AxiosInstance, config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await client.request<ApiEnvelope<T>>(config)
    const envelope = response.data
    if (!envelope || envelope.code !== 'SUCCESS') {
      throw new ApiError(
        envelope?.code ?? 'INVALID_RESPONSE',
        envelope?.message ?? '服务器返回了无法识别的数据',
        response.status,
        envelope?.traceId,
      )
    }
    return envelope.data
  }
  catch (error) {
    if (error instanceof ApiError) throw error
    if (axios.isAxiosError(error)) {
      const payload = error.response?.data as Partial<ApiEnvelope<unknown>> | undefined
      throw new ApiError(
        payload?.code ?? 'NETWORK_ERROR',
        payload?.message ?? error.message ?? '网络请求失败',
        error.response?.status,
        payload?.traceId,
      )
    }
    throw error
  }
}
