import { expect, test } from '@playwright/test'

test('西湖论剑 · 大会门户可以挂载应用', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: '西湖论剑 · 大会门户' })).toBeVisible()
})
