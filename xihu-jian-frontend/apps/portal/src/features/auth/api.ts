import type { createHttpClient, paths } from '@xhlj/shared'
import { request } from '@xhlj/shared'

type PortalLogin = paths['/auth/portal/login']['post']
type PortalMe = paths['/auth/portal/me']['get']
type AuthSession = PortalLogin['responses'][200]['content']['application/json']['data']
type Client = ReturnType<typeof createHttpClient>

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

/** 用当前内存凭证查询 Portal 身份，返回脱敏摘要。 */
export function getPortalIdentity(client: Client) {
  return request<UserProfile>(client, { url: '/api/v1/auth/portal/me' })
}
