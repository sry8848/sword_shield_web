import { expect, test } from '@playwright/test'

test('大会门户首页可打开', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /让每一次相聚/ })).toBeVisible()
  await expect(page.getByRole('link', { name: '议程与参会' })).toBeVisible()
})

test('游客登录后提交报名、查看凭证并退出', async ({ page }) => {
  await page.goto('/activities')
  await page.getByRole('link', { name: /西湖论剑大会主论坛/ }).click()
  await page.getByRole('button', { name: '立即报名' }).click()
  await expect(page).toHaveURL(/\/login\?redirect=/)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).toHaveURL(/\/activities\/ACT-1001$/)

  await page.getByLabel('参会人姓名').fill('测试参会人')
  await page.getByLabel('手机号').fill('13800000000')
  await page.getByRole('button', { name: '立即报名' }).click()
  await expect(page).toHaveURL(/\/me\/registrations$/)

  const row = page.getByRole('row', { name: /西湖论剑大会主论坛/ })
  await row.getByRole('button', { name: '查看凭证' }).click()
  await expect(page.getByRole('dialog', { name: '入场凭证' })).toContainText('XHLJ-REG-')
  await page.getByRole('button', { name: '关闭', exact: true }).click()
  await page.getByRole('button', { name: '退出', exact: true }).click()
  await expect(page).toHaveURL('/')
  await page.goto('/me/registrations')
  await expect(page).toHaveURL(/\/login\?redirect=/)
})
