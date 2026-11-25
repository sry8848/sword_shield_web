import type { createHttpClient, paths } from '@xhlj/shared'
import { request } from '@xhlj/shared'

type PortalLogin = paths['/auth/portal/login']['post']
type PortalMe = paths['/auth/portal/me']['get']
type PortalLogout = paths['/auth/portal/logout']['post']
type LogoutData = PortalLogout['responses'][200]['content']['application/json']['data']
type Client = ReturnType<typeof createHttpClient>

export type AuthSession = PortalLogin['responses'][200]['content']['application/json']['data']
export type LoginCredentials = PortalLogin['requestBody']['content']['application/json']
export type UserProfile = PortalMe['responses'][200]['content']['application/json']['data']

/** 通过 Portal 凭证入口创建会话，浏览器接收 HttpOnly Cookie。 */
export function loginPortal(client: Client, credentials: LoginCredentials) {
  return request<AuthSession>(client, {
    method: 'POST',
    url: '/api/v1/auth/portal/login',
    data: credentials,
    headers: { Authorization: false },
    withCredentials: true,
  })
}

/** 由浏览器发送刷新 Cookie，换取 Access Token。 */
export function refreshPortal(client: Client, signal?: AbortSignal) {
  return request<AuthSession>(client, {
    method: 'POST',
    url: '/api/v1/auth/portal/refresh',
    headers: { Authorization: false },
    withCredentials: true,
    signal,
  })
}

/** 撤销 Cookie 所属的当前 Portal 会话。 */
export function logoutPortal(client: Client) {
  return request<LogoutData>(client, {
    method: 'POST',
    url: '/api/v1/auth/portal/logout',
    headers: { Authorization: false },
    withCredentials: true,
  })
}

/** 用当前内存凭证查询身份，沿用当前会话的取消信号。 */
export function getPortalIdentity(client: Client, signal: AbortSignal) {
  return request<UserProfile>(client, { url: '/api/v1/auth/portal/me', signal })
}
