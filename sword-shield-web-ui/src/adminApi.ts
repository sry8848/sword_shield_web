import axios from 'axios'

const adminApi = axios.create({
  baseURL: '/api/admin',
})

adminApi.interceptors.request.use((config) => {
  const adminToken = localStorage.getItem('adminToken')
  if (adminToken) {
    config.headers.Authorization = `Bearer ${adminToken}`
  }
  return config
})

export default adminApi
