import type { Page } from '@playwright/test'
import { Buffer } from 'node:buffer'
import { sign } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import process from 'node:process'
import { expect, test } from '@playwright/test'

const portalAuthUrl = 'http://localhost:5173/api/v1/auth/portal/login'
const portalAccount = '138****0001'

test.describe.configure({ mode: 'serial' })

test.use({ baseURL: 'http://localhost:5173', trace: 'off' })

test('普通测试账号通过真实服务登录并返回受保护的身份页', async ({ page, context }) => {
  test.skip(process.env.PORTAL_REAL_AUTH_E2E !== 'true', '需要运行真实 Gateway、Identity、MySQL 和 Redis')

  // 1. 游客能看到公开页，进入账号页时返回登录表单。
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '西湖论剑 · 大会门户' })).toBeVisible()
  await page.goto('/account')
  await expect(page).toHaveURL(/\/login\?redirect=\/account/)

  // 2. 错误密码反馈来自真实 API，随后用同一测试账号完成登录。
  await page.getByLabel('手机号').fill('13800000001')
  await page.getByLabel('密码', { exact: true }).fill('wrong-password')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('账号或密码错误')
  await page.getByLabel('密码', { exact: true }).fill('LocalPortal-Only!2026')
  const identity = page.waitForResponse(response => response.url().endsWith('/api/v1/auth/portal/me'))
  await page.getByRole('button', { name: '登录', exact: true }).click()
  expect((await identity).status()).toBe(200)
  await expect(page).toHaveURL('/account')
  await expect(page.getByText('138****0001')).toBeVisible()

  // 3. 浏览器持有专用续期 Cookie，JavaScript 无法读取该 Cookie。
  const cookies = await context.cookies('http://localhost:5173/api/v1/auth/portal/login')
  const refresh = cookies.find(cookie => cookie.name === 'portal_refresh_token')
  expect(refresh?.httpOnly).toBe(true)
  expect(refresh?.path).toBe('/api/v1/auth/portal')
  expect(refresh?.sameSite).toBe('Lax')
})

test('重开 Portal 后恢复身份，过期时自动续期，退出后回到大会首页', async ({ page, context }) => {
  const key = await testSigningKey()
  await signIn(page)
  const restored = page.waitForResponse(response => response.url().endsWith('/refresh'))
  await page.reload()
  expect((await restored).status()).toBe(200)
  await expect(page.getByText(portalAccount)).toBeVisible()

  const reopened = await context.newPage()
  await page.close()
  await reopened.goto('/account')
  await expect(reopened.getByText(portalAccount)).toBeVisible()

  await reopened.getByRole('link', { name: '西湖论剑', exact: true }).click()
  await expireNextIdentityRequest(reopened, key)
  const expired = reopened.waitForResponse(response => response.url().endsWith('/me') && response.status() === 401)
  const refreshed = reopened.waitForResponse(response => response.url().endsWith('/refresh'))
  const retried = reopened.waitForResponse(response => response.url().endsWith('/me') && response.status() === 200)
  await reopened.getByText('我的', { exact: true }).click()
  await reopened.getByRole('link', { name: '个人概览' }).click()
  expect((await (await expired).json()).code).toBe('ACCESS_TOKEN_EXPIRED')
  expect((await refreshed).status()).toBe(200)
  expect((await retried).status()).toBe(200)
  await expect(reopened.getByText(portalAccount)).toBeVisible()

  const peer = await context.newPage()
  await peer.goto('/account')
  await expect(peer.getByText(portalAccount)).toBeVisible()

  await reopened.getByText('我的', { exact: true }).click()
  await reopened.getByRole('button', { name: '退出登录' }).click()
  await expect(reopened).toHaveURL(/5173\/$/)
  expect((await context.cookies(portalAuthUrl)).some(cookie => cookie.name === 'portal_refresh_token')).toBe(false)

  // 另一标签页持有自己的 Access Token，在下次续期失败时自行清理。
  await expect(peer.getByText(portalAccount)).toBeVisible()
  await peer.getByRole('link', { name: '西湖论剑', exact: true }).click()
  await expireNextIdentityRequest(peer, key)
  await peer.getByText('我的', { exact: true }).click()
  await peer.getByRole('link', { name: '个人概览' }).click()
  await expect(peer).toHaveURL(/\/login/)
  await expect(peer.getByRole('status')).toHaveText('登录已失效，请重新登录')
  await reopened.goto('/account')
  await expect(reopened).toHaveURL(/\/login\?redirect=\/account/)
})

test('两个标签页用同一个旧 Cookie 恢复，只有一个换 Cookie，换号后中止旧请求', async ({ page, context }) => {
  const key = await testSigningKey()
  await signIn(page)
  const first = await context.newPage()
  const second = await context.newPage()

  // 1. 两个浏览器请求都生成后再放行，确保它们携带同一个旧 Cookie。
  const sameCookies: string[] = []
  let releaseRefresh!: () => void
  const bothArrived = new Promise<void>((resolve) => {
    releaseRefresh = resolve
  })
  const refreshUrl = '**/api/v1/auth/portal/refresh'
  await context.route(refreshUrl, async (route) => {
    sameCookies.push(await route.request().headerValue('cookie') ?? '')
    if (sameCookies.length === 2)
      releaseRefresh()
    await bothArrived
    await route.continue()
  })
  try {
    const responses = [first, second].map(tab =>
      tab.waitForResponse(response => response.url().endsWith('/refresh')),
    )
    await Promise.all([first.goto('/account'), second.goto('/account')])
    const refreshes = await Promise.all(responses)
    expect(sameCookies[0]?.includes('portal_refresh_token=')).toBe(true)
    expect(sameCookies[1] === sameCookies[0]).toBe(true)
    expect(refreshes.map(response => response.status())).toEqual([200, 200])
    const cookieHeaders = await Promise.all(refreshes.map(response => response.headerValue('set-cookie')))
    expect(cookieHeaders.filter(Boolean)).toHaveLength(1)
    await expect(first.getByText(portalAccount)).toBeVisible()
    await expect(second.getByText(portalAccount)).toBeVisible()
  }
  finally {
    releaseRefresh()
    await context.unroute(refreshUrl)
  }

  // 2. 使用恢复后的 Cookie 再正常续期一次，然后让另一标签页切换账号。
  await first.getByRole('link', { name: '西湖论剑', exact: true }).click()
  await expireNextIdentityRequest(first, key)
  const followUp = first.waitForResponse(response => response.url().endsWith('/refresh'))
  await first.getByText('我的', { exact: true }).click()
  await first.getByRole('link', { name: '个人概览' }).click()
  expect((await followUp).status()).toBe(200)
  await expect(first.getByText(portalAccount)).toBeVisible()

  await second.getByText('我的', { exact: true }).click()
  await second.getByRole('button', { name: '退出登录' }).click()
  await expect(second).toHaveURL('http://localhost:5173/')
  await signIn(second, '13800000002', 'LocalAdmin-Only!2026')
  await expect(second.getByText('138****0002')).toBeVisible()
  await first.getByRole('link', { name: '西湖论剑', exact: true }).click()
  await expireNextIdentityRequest(first, key)
  let identityRequests = 0
  first.on('request', (request) => {
    if (request.url().endsWith('/me'))
      identityRequests++
  })
  await first.getByText('我的', { exact: true }).click()
  await first.getByRole('link', { name: '个人概览' }).click()
  await expect(first).toHaveURL(/\/login/)
  await expect(first.getByRole('status')).toHaveText('登录账号已变更，请重新登录')
  await expect(first.getByText('138****0002')).toHaveCount(0)
  expect(identityRequests).toBe(1)
})

test('刷新暂时失败时保留登录态，退出时取消迟到的私有请求', async ({ page, context }) => {
  const key = await testSigningKey()
  await signIn(page)
  await page.getByRole('link', { name: '西湖论剑', exact: true }).click()
  await expireNextIdentityRequest(page, key)
  await page.route('**/api/v1/auth/portal/refresh', route => route.fulfill({
    status: 503,
    contentType: 'application/problem+json',
    json: { code: 'SERVICE_UNAVAILABLE', detail: '服务暂不可用，请稍后重试' },
  }), { times: 1 })
  await page.getByText('我的', { exact: true }).click()
  await page.getByRole('link', { name: '个人概览' }).click()
  await expect(page.getByRole('alert')).toHaveText('暂时无法读取账号，请稍后重试')
  await expect(page.getByText(portalAccount)).toBeVisible()

  // 1. 退出结果暂时无法确认时，保留身份和 Cookie 供重试。
  await page.route('**/api/v1/auth/portal/logout', route => route.fulfill({
    status: 503,
    contentType: 'application/problem+json',
    json: { code: 'AUTH_STATE_UNAVAILABLE', detail: '暂时无法确认退出结果' },
  }), { times: 1 })
  await page.getByText('我的', { exact: true }).click()
  await page.getByRole('button', { name: '退出登录' }).click()
  await expect(page.getByText('暂时无法退出，请稍后重试', { exact: true })).toBeVisible()
  await expect(page.getByText(portalAccount)).toBeVisible()
  expect((await context.cookies(portalAuthUrl)).some(cookie => cookie.name === 'portal_refresh_token')).toBe(true)

  // 2. 等退出完成后才放行真实的私有响应。
  let notifyResponse!: () => void
  let releaseResponse!: () => void
  let notifyFinished!: () => void
  const responseHeld = new Promise<void>((resolve) => {
    notifyResponse = resolve
  })
  const hold = new Promise<void>((resolve) => {
    releaseResponse = resolve
  })
  const responseFinished = new Promise<void>((resolve) => {
    notifyFinished = resolve
  })
  await page.route('**/api/v1/auth/portal/me', async (route) => {
    try {
      const response = await route.fetch()
      expect(response.status()).toBe(200)
      notifyResponse()
      await hold
      await route.fulfill({ response })
    }
    catch (error) {
      // Axios 取消旧请求后，浏览器可能已关闭拦截。
      if (!page.url().endsWith('5173/'))
        throw error
    }
    finally {
      notifyFinished()
    }
  }, { times: 1 })
  try {
    await page.getByRole('button', { name: '重试' }).click()
    await responseHeld
    const canceled = page.waitForEvent('requestfailed', request => request.url().endsWith('/me'))
    await page.getByRole('button', { name: '退出登录' }).click()
    await expect(page).toHaveURL(/5173\/$/)
    expect((await canceled).failure()?.errorText).toBe('net::ERR_ABORTED')
  }
  finally {
    releaseResponse()
  }
  await responseFinished
  await expect(page.getByText(portalAccount)).toHaveCount(0)
  expect((await context.cookies(portalAuthUrl)).some(cookie => cookie.name === 'portal_refresh_token')).toBe(false)
})

async function signIn(page: Page, phone = '13800000001', password = 'LocalPortal-Only!2026') {
  if (!page.url().endsWith('/login?redirect=/account'))
    await page.goto('/login?redirect=/account')
  await page.getByLabel('手机号').fill(phone)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL('/account')
}

/** 只读取运行本组用例的 Identity 测试实例专用签名私钥。 */
async function testSigningKey() {
  test.skip(process.env.PORTAL_REAL_AUTH_E2E !== 'true' || !process.env.PORTAL_E2E_SIGNING_KEY, '需要真实 Portal 服务和专用测试签名私钥路径 PORTAL_E2E_SIGNING_KEY')
  return readFile(process.env.PORTAL_E2E_SIGNING_KEY!, 'utf8')
}

/** 为下一次 /me 构造真实可验签的过期令牌，免去等待十分钟。 */
async function expireNextIdentityRequest(page: Page, signingKey: string) {
  await page.route('**/api/v1/auth/portal/me', async (route) => {
    const headers = route.request().headers()
    const [header, payload] = headers.authorization.slice('Bearer '.length).split('.')
    const claims = JSON.parse(Buffer.from(payload, 'base64url').toString())
    const now = Math.floor(Date.now() / 1000)
    const expiredPayload = Buffer.from(JSON.stringify({ ...claims, iat: now - 601, exp: now - 1 })).toString('base64url')
    const signedContent = `${header}.${expiredPayload}`
    const signature = sign('RSA-SHA256', Buffer.from(signedContent), signingKey).toString('base64url')
    await route.continue({ headers: { ...headers, authorization: `Bearer ${signedContent}.${signature}` } })
  }, { times: 1 })
}

test('从公开页登录后保留原地址，右上角切换为我的', async ({ page }) => {
  test.skip(process.env.PORTAL_REAL_AUTH_E2E !== 'true', '需要运行真实 Gateway、Identity、MySQL 和 Redis')

  await page.goto('/?from=conference')
  await page.getByRole('link', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL(/\/login\?redirect=/)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.getByText('请输入 11 位中国大陆手机号')).toBeVisible()
  await page.getByLabel('手机号').fill('13800000002')
  await page.getByLabel('密码', { exact: true }).fill('LocalAdmin-Only!2026')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL(/5173\/\?from=conference$/)
  await page.getByText('我的', { exact: true }).click()
  await expect(page.getByRole('link', { name: '个人概览' })).toBeVisible()
})
