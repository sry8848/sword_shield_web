import axios, { AxiosHeaders } from 'axios'

const adminApi = axios.create({
  baseURL: '/api',
})

adminApi.interceptors.request.use((config) => {
  const adminToken = localStorage.getItem('adminToken')
  if (adminToken) {
    const current = config.headers ?? new AxiosHeaders()
    const headers = current instanceof AxiosHeaders ? current : new AxiosHeaders(current as any)
    headers.set('Authorization', `Bearer ${adminToken}`)
    config.headers = headers
  }
  return config
})

export default adminApi
