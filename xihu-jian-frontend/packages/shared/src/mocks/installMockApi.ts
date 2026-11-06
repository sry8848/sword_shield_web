import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import type { ApiEnvelope, ApiErrorResponse, PageQuery, PageResult } from '../types/api'
import type {
  ActivityCreateRequest,
  ActivityDetail,
  ActivityUpdateRequest,
  AdminRole,
  CheckinRecord,
  CheckinRequest,
  Credential,
  LoginRequest,
  LoginResult,
  Registration,
  RegistrationDetail,
  RegistrationRequest,
  StatisticsOverview,
  User,
} from '../types/domain'
import MockAdapter from 'axios-mock-adapter'

type MockReply = [number, ApiEnvelope<unknown> | ApiErrorResponse]
type MockRegistration = RegistrationDetail & { userId: string, credential: Credential }
type MockSession = LoginResult & { role?: AdminRole, expiresAt: number }

const sessionKey = 'xhlj.mock-session'
const accessLifetime = 7200
const apiPrefix = '/api/v1'
const rolePermissions: Record<AdminRole, string[]> = {
  SUPER_ADMIN: ['event:read', 'event:create', 'event:update', 'event:publish', 'registration:read', 'checkin:create', 'checkin:read', 'statistics:read'],
  EVENT_OPERATOR: ['event:read', 'event:create', 'event:update', 'event:publish', 'registration:read'],
  CHECKIN_STAFF: ['checkin:create', 'checkin:read'],
  DATA_OPERATOR: ['statistics:read'],
}

const demoAccounts = [
  { id: 'USER-001', account: 'user', displayName: '演示用户', password: 'user123' },
  { id: 'ADMIN-001', account: 'admin', displayName: '大会管理员', password: 'admin123', role: 'SUPER_ADMIN' },
  { id: 'CHECKIN-001', account: 'checkin', displayName: '现场核验员', password: 'checkin123', role: 'CHECKIN_STAFF' },
] as const

/** 安装当前骨架使用的 API Mock，返回卸载函数。 */
export function installMockApi(client: AxiosInstance) {
  const mock = new MockAdapter(client, { delayResponse: 250 })
  const activities = seedActivities()
  const registrations = seedRegistrations()
  const storedSession = sessionStorage.getItem(sessionKey)
  // Axios Mock 无法写入 HttpOnly Cookie。仅在演示环境保存可恢复的模拟会话。
  let session: MockSession | null = storedSession ? JSON.parse(storedSession) as MockSession : null
  let sequence = 0

  /** 为每个 Mock 响应提供独立的追踪标识和时间。 */
  function metadata() {
    return { traceId: `mock-${Date.now()}-${++sequence}`, timestamp: new Date().toISOString() }
  }

  const ok = <T>(data: T, status = 200): MockReply => [status, { code: 'SUCCESS', message: '操作成功', data, ...metadata() }]
  const fail = (status: number, code: string, message: string): MockReply => [status, { code, message, ...metadata() }]
  const unauthenticated = () => fail(401, 'UNAUTHENTICATED', '令牌缺失或无效')
  const notFound = (message: string) => fail(404, 'RESOURCE_NOT_FOUND', message)
  const invalid = (message: string) => fail(400, 'INVALID_ARGUMENT', message)

  /** 为演示账号签发新的访问令牌，刷新令牌由 Mock 会话隐式表示。 */
  function establishSession(user: User, role?: AdminRole) {
    const accessToken = `mock-access-${Date.now()}-${++sequence}`
    session = { accessToken, tokenType: 'Bearer', expiresIn: accessLifetime, expiresAt: Date.now() + accessLifetime * 1000, user, role }
    sessionStorage.setItem(sessionKey, JSON.stringify(session))
    return ok({ accessToken, tokenType: 'Bearer', expiresIn: accessLifetime, user } satisfies LoginResult)
  }

  /** 包装受保护的处理器，先鉴权再调用对应业务操作。 */
  function protect(reply: (config: AxiosRequestConfig, user: MockSession) => MockReply, permission?: string) {
    return (config: AxiosRequestConfig): MockReply => {
      if (!session || config.headers?.Authorization !== `Bearer ${session.accessToken}`)
        return unauthenticated()
      if (session.expiresAt <= Date.now())
        return fail(401, 'ACCESS_TOKEN_EXPIRED', '访问令牌已过期')
      if (permission && (!session.role || !rolePermissions[session.role].includes(permission)))
        return fail(403, 'FORBIDDEN', '没有执行该操作的权限')
      return reply(config, session)
    }
  }

  // 1. 登录建立会话，刷新和退出使用模拟 Cookie 会话，后台权限单独查询。
  mock.onPost(`${apiPrefix}/auth/login`).reply((config) => {
    const { account, password } = body<LoginRequest>(config)
    if (!account || !password)
      return invalid('请输入账号和密码')
    const demo = demoAccounts.find(item => item.account === account && item.password === password)
    if (!demo)
      return fail(401, 'UNAUTHENTICATED', '账号或密码错误')
    const { id, displayName } = demo
    return establishSession({ id, account, displayName }, 'role' in demo ? demo.role : undefined)
  })
  mock.onPost(`${apiPrefix}/auth/refresh`).reply(config =>
    config.withCredentials && session ? establishSession(session.user, session.role) : unauthenticated(),
  )
  mock.onPost(`${apiPrefix}/auth/logout`).reply((config) => {
    if (!config.withCredentials || !session)
      return unauthenticated()
    session = null
    sessionStorage.removeItem(sessionKey)
    return ok({ loggedOut: true })
  })
  mock.onGet(`${apiPrefix}/auth/me`).reply(protect((_config, current) => ok(current.user)))
  mock.onGet(`${apiPrefix}/admin/me`).reply(protect((_config, current) => {
    if (!current.role)
      return fail(403, 'FORBIDDEN', '当前账号没有后台权限')
    return ok({ user: current.user, roles: [current.role], permissions: rolePermissions[current.role] })
  }))

  // 2. 门户读取已发布的场次，登录后提交报名或查询本人的记录和凭证。
  mock.onGet(`${apiPrefix}/public/events`).reply((config) => {
    const { keyword = '', ...query } = (config.params ?? {}) as PageQuery & { keyword?: string }
    const published = activities.filter(event =>
      event.status === 'PUBLISHED' && `${event.title} ${event.summary ?? ''} ${event.venue?.name ?? ''}`.toLowerCase().includes(keyword.trim().toLowerCase()),
    )
    return ok(paginate(published, query))
  })
  mock.onGet(/\/api\/v1\/public\/events\/[^/]+$/).reply((config) => {
    const event = activities.find(item => item.id === pathId(config) && item.status === 'PUBLISHED')
    return event ? ok(event) : notFound('场次不存在')
  })
  mock.onPost(/\/api\/v1\/events\/[^/]+\/registrations$/).reply(protect((config, current) => {
    const payload = body<RegistrationRequest>(config)
    if (!payload.attendeeName?.trim() || !payload.phone?.trim())
      return invalid('参会人姓名和手机号必填')
    const event = activities.find(item => item.id === pathId(config, 2))
    if (!event)
      return notFound('场次不存在')
    if (registrations.some(item => item.eventId === event.id && item.userId === current.user.id)) {
      return fail(409, 'ALREADY_REGISTERED', '你已报名该场次')
    }
    if (event.registrationStatus === 'FULL')
      return fail(409, 'EVENT_FULL', '该场次报名名额已满')
    if (event.status !== 'PUBLISHED' || event.registrationStatus !== 'OPEN')
      return fail(422, 'REGISTRATION_CLOSED', '当前场次未开放报名')

    const id = `REG-${Date.now()}-${++sequence}`
    const registration: MockRegistration = {
      ...payload,
      id,
      userId: current.user.id,
      eventId: event.id,
      eventTitle: event.title,
      registeredAt: new Date().toISOString(),
      status: 'REGISTERED',
      credentialStatus: 'VALID',
      credential: { credentialNo: `XHLJ-${id}`, qrCodeContent: `mock-qr-${id}`, status: 'VALID' },
    }
    registrations.unshift(registration)
    event.registeredCount = (event.registeredCount ?? 0) + 1
    if (event.capacity != null && event.registeredCount >= event.capacity)
      event.registrationStatus = 'FULL'
    return ok(registrationDetail(registration), 201)
  }))
  mock.onGet(`${apiPrefix}/me/registrations`).reply(protect((config, current) => {
    const mine = registrations.filter(item => item.userId === current.user.id)
    return ok(paginate(mine.map(registrationSummary), config.params))
  }))
  mock.onGet(/\/api\/v1\/me\/registrations\/[^/]+\/credential$/).reply(protect((config, current) => {
    const record = registrations.find(item => item.id === pathId(config, 2) && item.userId === current.user.id)
    return record ? ok({ registrationId: record.id, eventId: record.eventId, credential: record.credential }) : notFound('凭证不存在')
  }))

  // 3. 后台按权限查询、创建和发布场次，PATCH 只更新提交的字段。
  mock.onGet(`${apiPrefix}/admin/events`).reply(protect(config => ok(paginate(activities, config.params)), 'event:read'))
  mock.onPost(`${apiPrefix}/admin/events`).reply(protect((config) => {
    const payload = body<ActivityCreateRequest>(config)
    if (!payload.title?.trim() || !payload.startTime || !payload.endTime)
      return invalid('场次名称、开始时间和结束时间必填')
    if (!validEventTimes(payload.startTime, payload.endTime))
      return fail(422, 'BUSINESS_RULE_VIOLATION', '结束时间应晚于开始时间')
    const event: ActivityDetail = {
      ...eventFields(payload, activities),
      id: `ACT-${Date.now()}-${++sequence}`,
      title: payload.title,
      startTime: payload.startTime,
      endTime: payload.endTime,
      status: 'DRAFT',
      registrationStatus: 'NOT_OPEN',
      registeredCount: 0,
    }
    activities.unshift(event)
    return ok(event, 201)
  }, 'event:create'))
  mock.onPatch(/\/api\/v1\/admin\/events\/[^/]+$/).reply(protect((config) => {
    const event = activities.find(item => item.id === pathId(config))
    if (!event)
      return notFound('场次不存在')
    const payload = body<ActivityUpdateRequest>(config)
    if (!validEventTimes(payload.startTime ?? event.startTime, payload.endTime ?? event.endTime)) {
      return fail(422, 'BUSINESS_RULE_VIOLATION', '结束时间应晚于开始时间')
    }
    Object.assign(event, eventFields(payload, activities))
    return ok(event)
  }, 'event:update'))
  mock.onPost(/\/api\/v1\/admin\/events\/[^/]+\/publish$/).reply(protect((config) => {
    const event = activities.find(item => item.id === pathId(config, 2))
    if (!event)
      return notFound('场次不存在')
    event.status = 'PUBLISHED'
    const now = Date.now()
    if (event.registrationStartAt && Date.parse(event.registrationStartAt) > now)
      event.registrationStatus = 'NOT_OPEN'
    else if (event.registrationEndAt && Date.parse(event.registrationEndAt) <= now)
      event.registrationStatus = 'CLOSED'
    else event.registrationStatus = 'OPEN'
    return ok(event)
  }, 'event:publish'))

  // 4. 后台区分报名摘要和详情，核验后同步更新报名、凭证和统计状态。
  mock.onGet(`${apiPrefix}/admin/registrations`).reply(protect(config =>
    ok(paginate(registrations.map(registrationSummary), config.params)), 'registration:read'))
  mock.onGet(/\/api\/v1\/admin\/registrations\/[^/]+$/).reply(protect((config) => {
    const record = registrations.find(item => item.id === pathId(config))
    return record ? ok(registrationDetail(record)) : notFound('报名记录不存在')
  }, 'registration:read'))
  mock.onPost(`${apiPrefix}/admin/check-ins`).reply(protect((config, current) => {
    const { eventId, credentialCode, inputMode } = body<CheckinRequest>(config)
    if (!eventId || !credentialCode || !['MANUAL_INPUT', 'QR_SCAN'].includes(inputMode))
      return invalid('请输入场次 ID、凭证和核验方式')
    const event = activities.find(item => item.id === eventId)
    const record = registrations.find(item => item.eventId === eventId && credentialCode
      === (inputMode === 'QR_SCAN' ? item.credential.qrCodeContent : item.credential.credentialNo))
    if (!event || !record)
      return notFound('该场次不存在匹配的凭证')
    if (record.status === 'CHECKED_IN')
      return fail(409, 'ALREADY_CHECKED_IN', '该凭证已核验')
    if (Date.parse(event.startTime) > Date.now())
      return fail(422, 'EVENT_NOT_STARTED', '该场次尚未开始')
    if (record.status !== 'REGISTERED' || record.credential.status !== 'VALID') {
      return fail(422, 'BUSINESS_RULE_VIOLATION', '凭证不满足核验条件')
    }
    record.status = 'CHECKED_IN'
    record.credentialStatus = record.credential.status = 'CHECKED_IN'
    const checkin: CheckinRecord = {
      id: `CHK-${Date.now()}-${++sequence}`,
      eventId,
      eventTitle: event.title,
      registrationId: record.id,
      credentialNo: record.credential.credentialNo,
      attendeeName: record.attendeeName,
      organization: record.organization,
      checkedInAt: new Date().toISOString(),
      operatorId: current.user.id,
      operatorName: current.user.displayName,
      inputMode,
    }
    return ok(checkin, 201)
  }, 'checkin:create'))
  mock.onGet(`${apiPrefix}/admin/statistics/overview`).reply(protect(() => {
    const registrationCount = registrations.filter(item => item.status !== 'CANCELLED').length
    const checkedInCount = registrations.filter(item => item.status === 'CHECKED_IN').length
    return ok({
      eventCount: activities.length,
      registrationCount,
      checkedInCount,
      checkInRate: registrationCount ? checkedInCount / registrationCount : 0,
      generatedAt: new Date().toISOString(),
    } satisfies StatisticsOverview)
  }, 'statistics:read'))

  // 未接入的接口返回契约错误，避免演示请求意外发送给真实后端。
  mock.onAny().reply(() => notFound('Mock 接口不存在'))
  return () => mock.restore()
}

/** 读取 Axios 序列化后的 JSON 请求体。 */
function body<T>(config: AxiosRequestConfig): T {
  return typeof config.data === 'string' ? JSON.parse(config.data) as T : config.data ?? {}
}

/** 提取并解码接口路径中的资源 ID。 */
function pathId(config: AxiosRequestConfig, offset = 1) {
  return decodeURIComponent(config.url!.split('/').at(-offset)!)
}

/** 按契约返回当前页数据和总记录数。 */
function paginate<T>(items: T[], { page = 1, pageSize = 10 }: PageQuery = {}): PageResult<T> {
  const total = items.length
  return {
    items: items.slice((page - 1) * pageSize, page * pageSize),
    pageInfo: { page, pageSize, total, totalPages: Math.ceil(total / pageSize) },
  }
}

/** 构造不含参会人资料和完整凭证的报名摘要。 */
function registrationSummary({ id, eventId, eventTitle, registeredAt, status, credentialStatus }: MockRegistration): Registration {
  return { id, eventId, eventTitle, registeredAt, status, credentialStatus }
}

/** 生成报名详情，内部归属字段 userId 仅用于 Mock 权限校验。 */
function registrationDetail(record: MockRegistration): RegistrationDetail {
  const { attendeeName, phone, organization, position, remark, credential } = record
  return { ...registrationSummary(record), attendeeName, phone, organization, position, remark, credential }
}

/** 校验两个场次时间都可解析且结束时间晚于开始时间。 */
function validEventTimes(startTime: string, endTime: string) {
  return Date.parse(startTime) < Date.parse(endTime)
}

/** 将创建或更新请求中的会场、讲者字段映射成响应对象。 */
function eventFields(payload: ActivityUpdateRequest, activities: ActivityDetail[]): Partial<ActivityDetail> {
  const { venueId, venueName, speakerIds, ...fields } = payload
  const event: Partial<ActivityDetail> = fields
  // PATCH 中缺省表示保持原值，显式 null 表示清除会场。
  if (venueId !== undefined || venueName !== undefined) {
    event.venue = activities.find(item => venueId && item.venue?.id === venueId)?.venue
      ?? (venueName ? { id: venueId ?? `mock-venue-${venueName}`, name: venueName } : null)
  }
  if (speakerIds !== undefined) {
    const speakers = new Map(activities.flatMap(item => item.speakers ?? []).map(speaker => [speaker.id, speaker]))
    event.speakers = speakerIds.flatMap(id => speakers.get(id) ?? [])
  }
  return event
}

/** 生成门户场次数据和一场可立即核验的场次。 */
function seedActivities(): ActivityDetail[] {
  return [
    { id: 'ACT-1001', title: '西湖论剑大会主论坛', summary: '围绕数字安全、人工智能与产业创新，分享前沿趋势和实践成果。', startTime: '2026-11-20T09:00:00+08:00', endTime: '2026-11-20T12:00:00+08:00', venue: { id: 'VENUE-1', name: '杭州国际博览中心 · 主会场' }, speakers: [{ id: 'SPK-1', name: '大会特邀嘉宾' }], status: 'PUBLISHED', registrationStatus: 'OPEN', capacity: 500, registeredCount: 268 },
    { id: 'ACT-1002', title: 'AI Agent 技术与产业应用专题', summary: '讨论 Agent 工程化、工具调用、上下文管理与真实业务落地。', startTime: '2026-11-20T14:00:00+08:00', endTime: '2026-11-20T16:30:00+08:00', venue: { id: 'VENUE-2', name: 'A 馆 · 2 号会议室' }, speakers: [{ id: 'SPK-2', name: '技术专家代表' }], status: 'PUBLISHED', registrationStatus: 'OPEN', capacity: 180, registeredCount: 96 },
    { id: 'ACT-1003', title: '数据安全与隐私保护圆桌', summary: '从合规治理、数据流通和安全技术三个角度展开讨论。', startTime: new Date(Date.now() - 3_600_000).toISOString(), endTime: new Date(Date.now() + 3_600_000).toISOString(), venue: { id: 'VENUE-3', name: 'B 馆 · 圆桌嘉宾区' }, speakers: [{ id: 'SPK-3', name: '圆桌嘉宾' }], status: 'PUBLISHED', registrationStatus: 'FULL', capacity: 80, registeredCount: 80 },
  ]
}

/** 生成可核验和已核验两种演示凭证。 */
function seedRegistrations(): MockRegistration[] {
  return [
    { id: 'REG-2001', userId: 'USER-001', eventId: 'ACT-1003', eventTitle: '数据安全与隐私保护圆桌', attendeeName: '演示用户', status: 'REGISTERED', registeredAt: '2026-11-05T10:30:00+08:00', credentialStatus: 'VALID', credential: { credentialNo: 'XHLJ-2026-0001', qrCodeContent: 'mock-qr-2001', status: 'VALID' } },
    { id: 'REG-2002', userId: 'USER-002', eventId: 'ACT-1002', eventTitle: 'AI Agent 技术与产业应用专题', attendeeName: '林同学', status: 'CHECKED_IN', registeredAt: '2026-11-04T14:20:00+08:00', credentialStatus: 'CHECKED_IN', credential: { credentialNo: 'XHLJ-2026-0002', qrCodeContent: 'mock-qr-2002', status: 'CHECKED_IN' } },
  ]
}
