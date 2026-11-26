import type { paths } from '@xhlj/shared'
import type { EventCreate, EventDetail, EventFilters, EventPage, EventUpdate } from './activity'
import { createHttpClient, request } from '@xhlj/shared'

type Csrf = paths['/auth/admin/csrf']['get']['responses'][200]['content']['application/json']['data']

const client = createHttpClient({ baseURL: import.meta.env.VITE_API_BASE_URL })
const eventsUrl = '/api/v1/admin/events'

/** 按筛选与页码查询场次，返回列表及分页信息。 */
export function listEvents(params: EventFilters, signal: AbortSignal) {
  return request<EventPage>(client, { url: eventsUrl, params, signal, withCredentials: true })
}

/** 按场次 ID 查询编辑表单所需的详情。 */
export function getEvent(eventId: string, signal: AbortSignal) {
  return request<EventDetail>(client, { url: `${eventsUrl}/${encodeURIComponent(eventId)}`, signal, withCredentials: true })
}

/** 保存新草稿或更新指定场次，返回后端保存的详情。 */
export function saveEvent(fields: EventCreate | EventUpdate, eventId?: string) {
  return writeEvent(eventId ? `${eventsUrl}/${encodeURIComponent(eventId)}` : eventsUrl, eventId ? 'PATCH' : 'POST', fields)
}

/** 提交发布或下线动作，返回变更后的场次。 */
export function changeEventStatus(eventId: string, action: 'publish' | 'unpublish') {
  return writeEvent(`${eventsUrl}/${encodeURIComponent(eventId)}/${action}`, 'POST')
}

/** 取得当前会话的 CSRF token，再提交场次写操作。 */
async function writeEvent(url: string, method: 'POST' | 'PATCH', data?: EventCreate | EventUpdate) {
  // 1. 读取与当前 Cookie 关联的 token。
  const { token } = await request<Csrf>(client, { url: '/api/v1/auth/admin/csrf', withCredentials: true })

  // 2. 随写请求提交 token，由后端校验身份和权限。
  return request<EventDetail>(client, { url, method, data, withCredentials: true, headers: { 'X-CSRF-TOKEN': token } })
}
