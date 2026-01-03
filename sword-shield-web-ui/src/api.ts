/**
 * 前台 API 客户端（axios 实例）。
 *
 * - baseURL 使用 `/api`：由前端开发代理 / Nginx 转发到后端网关。
 * - 请求拦截器：从 localStorage 读取 `token` 并注入 `Authorization: Bearer <token>`。
 * - 响应拦截器：将非统一返回结构兜底为 `{ code, message, data }`，便于页面统一处理。
 */
import axios, { AxiosHeaders } from 'axios'

const api = axios.create({
  baseURL: '/api',
  validateStatus: () => true,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    const current = config.headers ?? new AxiosHeaders()
    const headers = current instanceof AxiosHeaders ? current : new AxiosHeaders(current as any)
    headers.set('Authorization', `Bearer ${token}`)
    config.headers = headers
  }
  return config
})

api.interceptors.response.use((response) => {
  const data = response.data as any
  if (data && typeof data.code === 'number') {
    return response
  }
  response.data = { code: response.status || 500, message: '请求失败', data: null }
  return response
})

export default api
