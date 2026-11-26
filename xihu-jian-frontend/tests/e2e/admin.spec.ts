import process from 'node:process'
import { expect, test } from '@playwright/test'

test.use({ baseURL: 'http://localhost:5174', trace: 'off' })

test('后台首页在会话失效时自动打开登录页', async ({ page }) => {
  await page.route('**/api/v1/auth/admin/me', route => route.fulfill({
    status: 401,
    json: { code: 'UNAUTHENTICATED', detail: '登录已失效' },
  }))
  await page.goto('/')
  await expect(page).toHaveURL(/\/login\?redirect=\//)
  await expect(page.getByRole('heading', { name: '运营后台登录' })).toBeVisible()
})

test('真实后台认证拒绝普通账号，保持独立会话，支持恢复与退出', async ({ page, context }) => {
  test.skip(process.env.ADMIN_REAL_AUTH_E2E !== 'true', '需要真实 Gateway、Identity、MySQL 和 Redis')

  await page.goto('/?from=staff')
  await expect(page.getByRole('heading', { name: '运营后台登录' })).toBeVisible()
  await page.getByLabel('手机号', { exact: true }).fill('13800000001')
  await page.getByLabel('密码', { exact: true }).fill('LocalPortal-Only!2026')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('你没有后台访问权限')

  await page.getByLabel('手机号', { exact: true }).fill('13800000002')
  await page.getByLabel('密码', { exact: true }).fill('LocalAdmin-Only!2026')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL(/5174\/\?from=staff$/)
  await page.reload()
  await expect(page.getByRole('heading', { name: '后台身份' })).toBeVisible()
  await expect(page.getByText('138****0002')).toBeVisible()

  const portal = await context.newPage()
  await portal.goto('http://localhost:5173/account')
  await portal.getByLabel('手机号', { exact: true }).fill('13800000002')
  await portal.getByLabel('密码', { exact: true }).fill('LocalAdmin-Only!2026')
  await portal.getByRole('button', { name: '登录', exact: true }).click()
  await expect(portal.getByText('138****0002')).toBeVisible()

  await page.getByText('账号', { exact: true }).click()
  await page.getByRole('button', { name: '退出登录' }).click()
  await page.getByRole('button', { name: '确认退出' }).click()
  await expect(page).toHaveURL(/5174\/login$/)
  await portal.reload()
  await expect(portal.getByText('138****0002')).toBeVisible()
})
