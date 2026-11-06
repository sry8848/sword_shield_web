import type { AxiosInstance } from 'axios'
import MockAdapter from 'axios-mock-adapter'
import type { ApiEnvelope } from '../types/api'
import type { Activity, ActivityCreateRequest, Registration, StatisticsOverview, User } from '../types/domain'

const ok = <T>(data: T): ApiEnvelope<T> => ({ code: 'SUCCESS', message: 'OK', data })
const fail = <T>(code: string, message: string): ApiEnvelope<T | null> => ({ code, message, data: null })

function parseBody<T>(raw?: string): T {
  if (!raw) return {} as T
  try { return JSON.parse(raw) as T }
  catch { return {} as T }
}

function seedActivities(): Activity[] {
  return [
    { id: 'ACT-1001', title: '西湖论剑大会主论坛', summary: '围绕数字安全、人工智能与产业创新，分享前沿趋势和实践成果。', startAt: '2026-11-20T09:00:00+08:00', endAt: '2026-11-20T12:00:00+08:00', venue: '杭州国际博览中心 · 主会场', speaker: '大会特邀嘉宾', status: 'PUBLISHED', registrationStatus: 'OPEN', capacity: 500, registeredCount: 268 },
    { id: 'ACT-1002', title: 'AI Agent 技术与产业应用专题', summary: '讨论 Agent 工程化、工具调用、上下文管理与真实业务落地。', startAt: '2026-11-20T14:00:00+08:00', endAt: '2026-11-20T16:30:00+08:00', venue: 'A 馆 · 2 号会议室', speaker: '技术专家代表', status: 'PUBLISHED', registrationStatus: 'OPEN', capacity: 180, registeredCount: 96 },
    { id: 'ACT-1003', title: '数据安全与隐私保护圆桌', summary: '从合规治理、数据流通和安全技术三个角度展开讨论。', startAt: '2026-11-21T10:00:00+08:00', endAt: '2026-11-21T11:30:00+08:00', venue: 'B 馆 · 圆桌会议区', speaker: '圆桌嘉宾', status: 'PUBLISHED', registrationStatus: 'FULL', capacity: 80, registeredCount: 80 },
  ]
}

function seedRegistrations(): Registration[] {
  return [
    { id: 'REG-2001', activityId: 'ACT-1003', activityTitle: '数据安全与隐私保护圆桌', attendeeName: '演示用户', ticketCode: 'XHLJ-2026-0001', status: 'REGISTERED', createdAt: '2026-11-05T10:30:00+08:00' },
    { id: 'REG-2002', activityId: 'ACT-1002', activityTitle: 'AI Agent 技术与产业应用专题', attendeeName: '林同学', ticketCode: 'XHLJ-2026-0002', status: 'CHECKED_IN', createdAt: '2026-11-04T14:20:00+08:00' },
  ]
}

export function installMockApi(client: AxiosInstance) {
  const mock = new MockAdapter(client, { delayResponse: 250, onNoMatch: 'passthrough' })
  const activities = seedActivities()
  const registrations = seedRegistrations()
  const demoUser: User = { id: 'USER-001', username: 'user', displayName: '演示用户', role: 'USER' }
  const adminUser: User = { id: 'ADMIN-001', username: 'admin', displayName: '大会运营', role: 'ADMIN' }

  mock.onPost('/api/auth/login').reply((config) => {
    const body = parseBody<{ username?: string, password?: string }>(config.data)
    if (!body.username || !body.password) return [200, fail('VALIDATION_ERROR', '请输入用户名和密码')]
    const isAdmin = body.username === 'admin'
    return [200, ok({ accessToken: isAdmin ? 'mock-admin-token' : 'mock-user-token', user: isAdmin ? adminUser : { ...demoUser, username: body.username } })]
  })
  mock.onGet('/api/auth/me').reply((config) => {
    const token = config.headers?.Authorization
    if (token === 'Bearer mock-admin-token') return [200, ok(adminUser)]
    if (token === 'Bearer mock-user-token') return [200, ok(demoUser)]
    return [200, fail('UNAUTHENTICATED', '请先登录')]
  })
  mock.onGet('/api/activities').reply((config) => {
    const params = (config.params ?? {}) as { keyword?: string, page?: number, pageSize?: number }
    const keyword = (params.keyword ?? '').trim().toLowerCase()
    const filtered = activities.filter(item => item.status === 'PUBLISHED' && (!keyword || `${item.title} ${item.summary} ${item.venue}`.toLowerCase().includes(keyword)))
    const page = Number(params.page ?? 1)
    const pageSize = Number(params.pageSize ?? 10)
    const start = (page - 1) * pageSize
    return [200, ok({ records: filtered.slice(start, start + pageSize), total: filtered.length, page, pageSize })]
  })
  mock.onGet(/\/api\/activities\/[^/]+$/).reply((config) => {
    const id = config.url?.split('/').pop()
    const activity = activities.find(item => item.id === id)
    return activity ? [200, ok(activity)] : [200, fail('NOT_FOUND', '活动不存在')]
  })
  mock.onPost(/\/api\/activities\/[^/]+\/registrations$/).reply((config) => {
    const activityId = config.url?.split('/').at(-2)
    const activity = activities.find(item => item.id === activityId)
    if (!activity) return [200, fail('NOT_FOUND', '活动不存在')]
    if (activity.registrationStatus !== 'OPEN') return [200, fail('REGISTRATION_CLOSED', '当前活动暂不接受报名')]
    const existed = registrations.find(item => item.activityId === activityId && item.attendeeName === '演示用户')
    if (existed) return [200, fail('DUPLICATE_REGISTRATION', '你已经报名过该活动')]
    const registration: Registration = {
      id: `REG-${Date.now()}`, activityId: activity.id, activityTitle: activity.title,
      attendeeName: '演示用户', ticketCode: `XHLJ-${Date.now()}`, status: 'REGISTERED', createdAt: new Date().toISOString(),
    }
    registrations.unshift(registration)
    activity.registeredCount += 1
    return [200, ok(registration)]
  })
  mock.onGet('/api/me/registrations').reply((config) => {
    if (!config.headers?.Authorization) return [200, fail('UNAUTHENTICATED', '请先登录')]
    return [200, ok(registrations.filter(item => item.attendeeName === '演示用户'))]
  })
  mock.onGet('/api/admin/activities').reply(() => [200, ok({ records: activities, total: activities.length, page: 1, pageSize: 100 })])
  mock.onPost('/api/admin/activities').reply((config) => {
    const body = parseBody<ActivityCreateRequest>(config.data)
    if (!body.title || !body.startAt || !body.venue) return [200, fail('VALIDATION_ERROR', '活动名称、时间和地点不能为空')]
    const activity: Activity = {
      id: `ACT-${Date.now()}`, title: body.title, summary: body.summary, startAt: body.startAt,
      venue: body.venue, speaker: body.speaker, status: 'DRAFT', registrationStatus: 'NOT_OPEN',
      capacity: body.capacity, registeredCount: 0,
    }
    activities.unshift(activity)
    return [200, ok(activity)]
  })
  mock.onPut(/\/api\/admin\/activities\/[^/]+$/).reply((config) => {
    const id = config.url?.split('/').pop()
    const activity = activities.find(item => item.id === id)
    if (!activity) return [200, fail('NOT_FOUND', '活动不存在')]
    Object.assign(activity, parseBody<ActivityCreateRequest>(config.data))
    return [200, ok(activity)]
  })
  mock.onPost(/\/api\/admin\/activities\/[^/]+\/publish$/).reply((config) => {
    const id = config.url?.split('/').at(-2)
    const activity = activities.find(item => item.id === id)
    if (!activity) return [200, fail('NOT_FOUND', '活动不存在')]
    activity.status = 'PUBLISHED'
    activity.registrationStatus = 'OPEN'
    return [200, ok(activity)]
  })
  mock.onGet('/api/admin/registrations').reply(() => [200, ok(registrations)])
  mock.onPost('/api/admin/check-ins').reply((config) => {
    const body = parseBody<{ ticketCode?: string }>(config.data)
    const registration = registrations.find(item => item.ticketCode === body.ticketCode)
    if (!registration) return [200, fail('INVALID_TICKET', '未找到有效凭证')]
    if (registration.status === 'CHECKED_IN') return [200, fail('ALREADY_CHECKED_IN', '该凭证已经核验过')]
    if (registration.status !== 'REGISTERED') return [200, fail('INVALID_STATUS', '当前凭证状态不可核验')]
    registration.status = 'CHECKED_IN'
    return [200, ok(registration)]
  })
  mock.onGet('/api/admin/statistics/overview').reply(() => {
    const result: StatisticsOverview = {
      activityCount: activities.filter(item => item.status === 'PUBLISHED').length,
      registrationCount: registrations.length,
      checkedInCount: registrations.filter(item => item.status === 'CHECKED_IN').length,
    }
    return [200, ok(result)]
  })
  return () => mock.restore()
}
