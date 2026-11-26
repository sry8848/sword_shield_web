import { resolveLoginRedirect } from '@xhlj/shared'
import { expect, it } from 'vitest'

it('保留已注册站内路径的查询参数和锚点', () => {
  const destination = '/account?tab=registration#next'
  expect(resolveLoginRedirect(destination, path => path === destination)).toBe(destination)
})

it('只接受路由表确认的站内地址', () => {
  const isRegistered = (path: string) => path.startsWith('/account')
  expect(resolveLoginRedirect('/missing', isRegistered)).toBeUndefined()
  expect(resolveLoginRedirect('https://example.test/account', () => true)).toBeUndefined()
  expect(resolveLoginRedirect('//example.test/account', () => true)).toBeUndefined()
  expect(resolveLoginRedirect(['/account', '/'], () => true)).toBeUndefined()
})
