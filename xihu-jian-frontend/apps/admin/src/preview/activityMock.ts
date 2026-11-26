import type { Connect, Plugin } from 'vite'
import type { EventDetail, EventPage, EventUpdate } from '../features/activity/activity'
import { activityFixtures, fixtureId } from './activityFixtures'

type Response = Parameters<Connect.NextHandleFunction>[1]

const csrf = 'activity-preview-csrf'

/** 写入与 OpenAPI 一致的成功响应或 RFC 9457 错误。 */
function respond(res: Response, status: number, body: object, isProblem = false) {
  res.statusCode = status
  res.setHeader('Content-Type', isProblem ? 'application/problem+json' : 'application/json')
  res.setHeader('Cache-Control', 'no-store')
  res.end(JSON.stringify({ ...body, traceId: crypto.randomUUID(), timestamp: new Date().toISOString() }))
}

/** 保留 HTTP 状态和业务错误码供正式客户端解析。 */
function problem(res: Response, status: number, code: string, detail: string) {
  respond(res, status, { type: 'about:blank', title: detail, status, code, detail }, true)
}

/** 从已校验的场次记录生成当前的报名状态。 */
function registrationStatus(event: EventDetail): EventDetail['registrationStatus'] {
  if (event.status === 'DRAFT')
    return 'NOT_OPEN'
  const now = Date.now()
  if (event.status !== 'PUBLISHED' || (event.registrationEndAt && Date.parse(event.registrationEndAt) <= now))
    return 'CLOSED'
  if (event.registrationStartAt && Date.parse(event.registrationStartAt) > now)
    return 'NOT_OPEN'
  return event.capacity != null && (event.registeredCount ?? 0) >= event.capacity ? 'FULL' : 'OPEN'
}

/** 应用本次提交的可编辑字段，保留详情中的其他字段。 */
function applyFields(event: EventDetail, fields: EventUpdate): EventDetail {
  const { venueName, ...editable } = fields
  const venue = venueName === undefined || venueName === event.venue?.name
    ? event.venue
    : venueName ? { id: event.id, name: venueName } : null
  const updated = { ...event, ...editable, venue }
  return { ...updated, registrationStatus: registrationStatus(updated) }
}

/** 从同一批命中结果计算当前页和总数。 */
function filterEvents(events: EventDetail[], params: URLSearchParams): EventPage {
  // 1. 按 OpenAPI 的关键词和发布状态筛选。
  const keyword = params.get('keyword')?.trim().toLowerCase()
  const status = params.get('status')
  const matches = events.filter(event => (!keyword || event.title.toLowerCase().includes(keyword)) && (!status || event.status === status))

  // 2. 按传入页码截取列表，并返回该批结果的总数。
  const page = Number(params.get('page') || 1)
  const pageSize = Number(params.get('pageSize') || 10)
  return {
    items: matches.slice((page - 1) * pageSize, page * pageSize),
    pageInfo: { page, pageSize, total: matches.length, totalPages: Math.ceil(matches.length / pageSize) },
  }
}

/** 只在本地预览服务中启用场次 Mock，处理与联调一致的 HTTP 路径。 */
export function activityMock(): Plugin {
  const events = activityFixtures()
  let nextEventIndex = 1000

  return {
    name: 'activity-preview-mock',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use('/api/v1/auth/admin/csrf', (_req, res) => {
        respond(res, 200, { code: 'SUCCESS', message: '操作成功', data: { token: csrf } })
      })

      server.middlewares.use('/api/v1/admin/events', async (req, res) => {
        const url = new URL(req.url || '/', 'http://localhost')
        const [, eventId, action] = url.pathname.split('/')
        const index = events.findIndex(event => event.id === eventId)
        const event = events[index]

        // 1. 读取列表或场次详情。
        if (eventId && !event) {
          problem(res, 404, 'RESOURCE_NOT_FOUND', '场次不存在')
          return
        }
        if (req.method === 'GET' && !action) {
          respond(res, 200, { code: 'SUCCESS', message: '操作成功', data: event ?? filterEvents(events, url.searchParams) })
          return
        }
        if (req.headers['x-csrf-token'] !== csrf) {
          problem(res, 403, 'CSRF_INVALID', '请求校验失败')
          return
        }

        // 2. 模拟发布状态变更并保留已有报名信息。
        if (req.method === 'POST' && event && (action === 'publish' || action === 'unpublish')) {
          const status = action === 'publish' ? 'PUBLISHED' : 'UNPUBLISHED'
          const canChange = action === 'publish'
            ? event.status === 'DRAFT' || event.status === 'UNPUBLISHED'
            : event.status === 'PUBLISHED'
          if (!canChange) {
            problem(res, 409, 'BUSINESS_CONFLICT', '当前状态不支持该操作，请刷新列表')
            return
          }
          const saved: EventDetail = { ...event, status }
          saved.registrationStatus = registrationStatus(saved)
          events[index] = saved
          respond(res, 200, { code: 'SUCCESS', message: '操作成功', data: saved })
          return
        }

        // 3. 读取表单并保存新草稿或更新指定场次。
        const isSaveRequest = event ? req.method === 'PATCH' && !action : req.method === 'POST'
        if (!isSaveRequest) {
          problem(res, 404, 'RESOURCE_NOT_FOUND', '操作不存在')
          return
        }
        let fields: EventUpdate
        try {
          req.setEncoding('utf8')
          let body = ''
          for await (const chunk of req)
            body += chunk
          const payload: unknown = JSON.parse(body)
          if (!payload || typeof payload !== 'object' || Array.isArray(payload))
            throw new TypeError('场次请求必须是 JSON 对象')
          fields = payload as EventUpdate
        }
        catch {
          problem(res, 400, 'INVALID_ARGUMENT', '请求格式不正确')
          return
        }
        const required = { ...event, ...fields }
        const startTime = Date.parse(required.startTime ?? '')
        const endTime = Date.parse(required.endTime ?? '')
        if (typeof required.title !== 'string' || !required.title.trim()
          || typeof required.startTime !== 'string' || !Number.isFinite(startTime)
          || typeof required.endTime !== 'string' || !Number.isFinite(endTime)) {
          problem(res, 400, 'INVALID_ARGUMENT', '请填写场次名称和起止时间')
          return
        }
        if (endTime <= startTime) {
          problem(res, 422, 'BUSINESS_RULE_VIOLATION', '场次结束时间应晚于开始时间')
          return
        }
        const draft: EventDetail = {
          id: event?.id ?? fixtureId(nextEventIndex++),
          title: required.title,
          startTime: required.startTime,
          endTime: required.endTime,
          status: 'DRAFT',
          registrationStatus: 'NOT_OPEN',
          registeredCount: 0,
          speakers: [],
        }
        const saved = applyFields(event ?? draft, fields)
        if (event)
          events[index] = saved
        else
          events.unshift(saved)
        respond(res, event ? 200 : 201, { code: 'SUCCESS', message: '操作成功', data: saved })
      })
    },
  }
}
