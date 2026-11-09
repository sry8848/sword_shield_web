import { createHttpClient, request } from '@xhlj/shared'
import { expect, it, vi } from 'vitest'

it('并发访问令牌过期时仅刷新一次，并用新令牌重试', async () => {
  // 1. 用可替换的令牌回调模拟当前会话。
  let accessToken = 'expired-token'
  let completeRefresh!: (token: string) => void
  const pendingRefresh = new Promise<string>((resolve) => {
    completeRefresh = resolve
  })
  const refresh = vi.fn(async () => {
    accessToken = await pendingRefresh
    return accessToken
  })
  const client = createHttpClient({
    getAccessToken: () => accessToken,
    refreshAccessToken: refresh,
  })

  // 2. 模拟请求层边界，旧令牌收到 401，新令牌收到成功响应。
  client.defaults.adapter = vi.fn(async (config) => {
    if (config.headers.get('Authorization') === 'Bearer renewed-token') {
      return { config, status: 200, statusText: 'OK', headers: {}, data: { code: 'SUCCESS', data: 'ok' } }
    }

    throw Object.assign(new Error('Access token expired'), {
      isAxiosError: true,
      config,
      response: { status: 401, data: { code: 'ACCESS_TOKEN_EXPIRED' } },
    })
  })

  // 3. 等两个旧令牌请求都发出后，再完成刷新。
  // 让两个请求都遇到尚未完成的刷新，才能验证它们共用同一次请求。
  const responses = Promise.all([
    request(client, { url: '/first' }),
    request(client, { url: '/second' }),
  ])
  await vi.waitFor(() => expect(client.defaults.adapter).toHaveBeenCalledTimes(2))
  completeRefresh('renewed-token')

  // 4. 验证两个请求都能重试成功，并共用一次刷新。
  await expect(responses).resolves.toEqual(['ok', 'ok'])
  expect(refresh).toHaveBeenCalledExactlyOnceWith('expired-token')
  expect(client.defaults.adapter).toHaveBeenCalledTimes(4)
})

it('权限不足时保留 HTTP 状态、错误码和追踪信息', async () => {
  const refresh = vi.fn()
  const client = createHttpClient({ getAccessToken: () => 'access-token', refreshAccessToken: refresh })

  // 1. 模拟 HTTP 层收到的权限错误。
  client.defaults.adapter = async (config) => {
    throw Object.assign(new Error('Forbidden'), {
      isAxiosError: true,
      config,
      response: { status: 403, data: { code: 'FORBIDDEN', message: '权限不足', traceId: 'trace-403' } },
    })
  }

  // 2. 验证错误转换保留排查信息，权限错误不触发认证刷新。
  await expect(request(client, { url: '/protected' }))
    .rejects
    .toMatchObject({ status: 403, code: 'FORBIDDEN', traceId: 'trace-403' })
  expect(refresh).not.toHaveBeenCalled()
})
