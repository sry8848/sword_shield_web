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
