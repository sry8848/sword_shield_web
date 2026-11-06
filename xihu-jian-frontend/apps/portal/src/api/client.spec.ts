import { createApi, createHttpClient } from '@xhlj/shared'
import { installMockApi } from '@xhlj/shared/mocks'
import { afterEach, expect, it, vi } from 'vitest'

afterEach(() => {
  vi.useRealTimers()
  sessionStorage.clear()
})

it('并发访问令牌过期时仅刷新一次，并用新令牌重试', async () => {
  let accessToken: string | null = null
  let api: ReturnType<typeof createApi>
  const client = createHttpClient({
    getAccessToken: () => accessToken,
    refreshAccessToken: async () => {
      const session = await api.auth.refresh()
      accessToken = session.accessToken
      return session.accessToken
    },
  })
  api = createApi(client)
  const refresh = vi.spyOn(api.auth, 'refresh')
  installMockApi(client)

  const session = await api.auth.login({ account: 'user', password: 'user123' })
  accessToken = session.accessToken
  vi.setSystemTime(Date.now() + (session.expiresIn + 1) * 1000)

  const [profile, registrations] = await Promise.all([api.auth.me(), api.registrations.mine()])
  expect(profile.account).toBe('user')
  expect(registrations.pageInfo.total).toBe(1)
  expect(refresh).toHaveBeenCalledTimes(1)
  expect(accessToken).not.toBe(session.accessToken)
})

it('权限不足和重复报名保留 HTTP 状态、错误码及追踪信息', async () => {
  let accessToken: string | null = null
  const refresh = vi.fn()
  const client = createHttpClient({ getAccessToken: () => accessToken, refreshAccessToken: refresh })
  const api = createApi(client)
  installMockApi(client)
  accessToken = (await api.auth.login({ account: 'user', password: 'user123' })).accessToken

  await expect(api.admin.me()).rejects.toMatchObject({ status: 403, code: 'FORBIDDEN' })
  await expect(api.activities.register('ACT-1003', { attendeeName: '测试用户', phone: '13800000000' }))
    .rejects
    .toMatchObject({ status: 409, code: 'ALREADY_REGISTERED', traceId: expect.any(String), timestamp: expect.any(String) })
  expect(refresh).not.toHaveBeenCalled()
})
