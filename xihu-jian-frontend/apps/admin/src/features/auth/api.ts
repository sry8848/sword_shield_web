import type { paths } from '@xhlj/shared'
import { createHttpClient, request } from '@xhlj/shared'

type Login = paths['/auth/admin/login']['post']
type Csrf = paths['/auth/admin/csrf']['get']
type Identity = paths['/auth/admin/me']['get']
type Logout = paths['/auth/admin/logout']['post']

export type AdminCredentials = Login['requestBody']['content']['application/json']
export type AdminProfile = Identity['responses'][200]['content']['application/json']['data']
type CsrfData = Csrf['responses'][200]['content']['application/json']['data']
type LogoutData = Logout['responses'][200]['content']['application/json']['data']

const client = createHttpClient({ baseURL: import.meta.env.VITE_API_BASE_URL })

/** 读取浏览器当前后台会话关联的 CSRF token。 */
export async function getAdminCsrf(signal: AbortSignal) {
  const { token } = await request<CsrfData>(client, {
    url: '/api/v1/auth/admin/csrf',
    withCredentials: true,
    signal,
  })
  return token
}

/** 使用凭证和当前 CSRF token 建立后台会话。 */
export function loginAdmin(data: AdminCredentials, csrf: string, signal: AbortSignal) {
  return request<AdminProfile>(client, {
    method: 'POST',
    url: '/api/v1/auth/admin/login',
    data,
    headers: { 'X-CSRF-TOKEN': csrf },
    withCredentials: true,
    signal,
  })
}

/** 用浏览器 Cookie 读取当前账号和有效角色。 */
export function getAdminIdentity(signal: AbortSignal) {
  return request<AdminProfile>(client, {
    url: '/api/v1/auth/admin/me',
    withCredentials: true,
    signal,
  })
}

/** 撤销浏览器当前后台会话。 */
export function logoutAdmin(csrf: string, signal: AbortSignal) {
  return request<LogoutData>(client, {
    method: 'POST',
    url: '/api/v1/auth/admin/logout',
    headers: { 'X-CSRF-TOKEN': csrf },
    withCredentials: true,
    signal,
  })
}
