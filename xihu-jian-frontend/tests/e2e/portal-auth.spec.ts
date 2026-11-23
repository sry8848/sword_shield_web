import process from 'node:process'
import { expect, test } from '@playwright/test'

test.use({ baseURL: 'http://localhost:5173', trace: 'off' })

test('普通测试账号通过真实服务登录并返回受保护的身份页', async ({ page, context }) => {
  test.skip(process.env.PORTAL_REAL_AUTH_E2E !== 'true', '需要运行真实 Gateway、Identity、MySQL 和 Redis')

  // 1. 游客能看到公开页，进入账号页时返回登录表单。
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '西湖论剑 · 大会门户' })).toBeVisible()
  await page.getByRole('link', { name: '我的账号' }).click()
  await expect(page).toHaveURL(/\/login\?redirect=\/account/)

  // 2. 错误密码反馈来自真实 API，随后用同一测试账号完成登录。
  await page.getByLabel('手机号').fill('13800000001')
  await page.getByLabel('密码').fill('wrong-password')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('账号或密码错误')
  await page.getByLabel('密码').fill('LocalPortal-Only!2026')
  const identity = page.waitForResponse(response => response.url().endsWith('/api/v1/auth/portal/me'))
  await page.getByRole('button', { name: '登录', exact: true }).click()
  expect((await identity).status()).toBe(200)
  await expect(page).toHaveURL(/\/account$/)
  await expect(page.getByText('138****0001')).toBeVisible()

  // 3. 浏览器持有专用续期 Cookie，JavaScript 无法读取该 Cookie。
  const cookies = await context.cookies('http://localhost:5173/api/v1/auth/portal/login')
  const refresh = cookies.find(cookie => cookie.name === 'portal_refresh_token')
  expect(refresh?.httpOnly).toBe(true)
  expect(refresh?.path).toBe('/api/v1/auth/portal')
  expect(refresh?.sameSite).toBe('Lax')
})
