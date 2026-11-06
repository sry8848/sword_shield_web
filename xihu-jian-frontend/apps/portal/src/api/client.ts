import { createApi, createHttpClient } from '@xhlj/shared'

const TOKEN_KEY = 'xhlj.access-token'
export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  getAccessToken: () => sessionStorage.getItem(TOKEN_KEY),
  onUnauthorized: () => sessionStorage.removeItem(TOKEN_KEY),
})
export const api = createApi(http)
export { TOKEN_KEY }
