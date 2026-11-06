import { createApi, createHttpClient } from '@xhlj/shared'
import { useAuthStore } from '../stores/auth'

export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  getAccessToken: () => useAuthStore().accessToken,
  refreshAccessToken: token => useAuthStore().refresh(token),
  onUnauthorized: () => useAuthStore().clearSession(),
})

export const api = createApi(http)
