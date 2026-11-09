import { expect, test } from '@playwright/test'

test('西湖论剑 · 运营管理可以挂载应用', async ({ page }) => {
  await page.goto('http://127.0.0.1:5174/')
  await expect(page.getByRole('heading', { level: 1, name: '西湖论剑 · 运营管理' })).toBeVisible()
})
