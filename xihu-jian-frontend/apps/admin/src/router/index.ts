import type { AdminProfile } from '../features/auth/api'
import { resolveLoginRedirect } from '@xhlj/shared'
import { createRouter, createWebHistory } from 'vue-router'
import { useAdminAuth } from '../features/auth/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: readonly AdminProfile['roles'][number][]
  }
}

const defaultPage = { name: 'identity' }

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../features/auth/LoginPage.vue') },
    {
      path: '/',
      name: 'identity',
      component: () => import('../features/auth/IdentityPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('../features/auth/ForbiddenPage.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', redirect: defaultPage },
  ],
})

/** 解析后台自己的已注册业务地址，缺少可用地址时进入身份页。 */
export function adminLoginDestination(target: unknown) {
  return resolveLoginRedirect(target, path => router.resolve(path).meta.requiresAuth === true) ?? defaultPage
}

/** 所有后台业务页在显示前确认会话，随后按路由声明的角色判断访问资格。 */
router.beforeEach(async (to) => {
  const auth = useAdminAuth()

  // 1. 等待已有会话的校验结果，临时错误交给根布局呈现重试入口。
  await auth.checkIdentity()
  if (auth.sessionError)
    return

  // 2. 登录页将已有会话送回目标，业务页按身份和权限决定去向。
  if (to.name === 'login' && auth.profile)
    return adminLoginDestination(to.query.redirect)
  if (to.meta.requiresAuth && !auth.profile)
    return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.roles && !to.meta.roles.some(role => auth.profile?.roles.includes(role)))
    return { name: 'forbidden' }
})
