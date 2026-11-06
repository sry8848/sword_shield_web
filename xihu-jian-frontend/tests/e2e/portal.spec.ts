import { expect, test } from '@playwright/test'

test('大会门户首页可打开', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /让每一次相聚/ })).toBeVisible()
  await expect(page.getByRole('link', { name: '议程与参会' })).toBeVisible()
})
