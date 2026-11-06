import { expect, test } from '@playwright/test'

const adminUrl = 'http://127.0.0.1:5174'

test('管理员新建并发布场次，查看报名详情', async ({ page }) => {
  await page.goto(`${adminUrl}/activities`)
  await page.getByRole('button', { name: '登录管理后台' }).click()
  await page.getByRole('button', { name: '新建活动' }).click()

  const dialog = page.getByRole('dialog', { name: '新建活动' })
  await dialog.getByLabel('活动名称').fill('契约测试场次')
  await dialog.getByLabel('开始时间').fill('2027-04-01T09:00:00+08:00')
  await dialog.getByLabel('结束时间').fill('2027-04-01T10:00:00+08:00')
  await dialog.getByLabel('活动地点').fill('测试会场')
  await dialog.getByRole('button', { name: '保存草稿' }).click()

  const event = page.getByRole('row', { name: /契约测试场次/ })
  await expect(event).toContainText('草稿')
  await event.getByRole('button', { name: '发布' }).click()
  await expect(event).toContainText('已发布')

  await page.getByRole('menuitem', { name: '报名管理' }).click()
  await page.getByRole('row', { name: /REG-2001/ }).getByRole('button', { name: '查看详情' }).click()
  await expect(page.getByRole('dialog', { name: '报名详情' })).toContainText('XHLJ-2026-0001')
})

test('核验员只看到授权菜单，重复核验被拒绝', async ({ page }) => {
  await page.goto(`${adminUrl}/login`)
  await page.getByLabel('管理员账号').fill('checkin')
  await page.getByLabel('密码').fill('checkin123')
  await page.getByRole('button', { name: '登录管理后台' }).click()
  await expect(page).toHaveURL(/\/checkin$/)
  await expect(page.getByRole('menuitem', { name: '活动管理' })).toHaveCount(0)

  const submit = page.getByRole('button', { name: '确认核验' })
  await submit.click()
  await expect(page.getByText('该凭证已完成现场核验')).toBeVisible()
  await submit.click()
  await expect(page.getByText('该凭证已核验', { exact: true })).toBeVisible()

  await page.goto(`${adminUrl}/activities`)
  await expect(page).toHaveURL(/\/checkin$/)
})
