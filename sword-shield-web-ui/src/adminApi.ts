/**
 * 管理后台 API 客户端（axios 实例）。
 *
 * - 与前台共用 `/api` 前缀（由网关区分路径）。
 * - 请求拦截器：除登录接口外，从 localStorage 读取 `adminToken` 并注入 Authorization。
 * - 之所以跳过 `/admin/auth/login`：避免携带过期 token 被网关提前拒绝导致无法登录。
 */
import axios, { AxiosHeaders } from 'axios'

const adminApi = axios.create({
  baseURL: '/api',
  validateStatus: () => true,
})

adminApi.interceptors.request.use((config) => {
  const url = config.url || ''
  // Admin login is a public endpoint; sending an expired token will cause the gateway to reject the request.
  if (url.includes('/admin/auth/login')) {
    return config
  }
  const adminToken = localStorage.getItem('adminToken')
  if (adminToken) {
    const current = config.headers ?? new AxiosHeaders()
    const headers = current instanceof AxiosHeaders ? current : new AxiosHeaders(current as any)
    headers.set('Authorization', `Bearer ${adminToken}`)
    config.headers = headers
  }
  return config
})

adminApi.interceptors.response.use((response) => {
  const data = response.data as any
  if (data && typeof data.code === 'number') {
    return response
  }
  response.data = { code: response.status || 500, message: '请求失败', data: null }
  return response
})

export default adminApi
