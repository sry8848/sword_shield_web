import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../features/auth/auth'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('../features/home/HomePage.vue') },
    { path: '/login', name: 'login', component: () => import('../features/auth/LoginPage.vue') },
    {
      path: '/account',
      name: 'account',
      component: () => import('../features/auth/AccountPage.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/:pathMatch(.*)*', redirect: { name: 'home' } },
  ],
})

/** 访问受保护页面时等待会话恢复，未登录则保留已匹配的站内目标。 */
router.beforeEach(async (to, from) => {
  const auth = useAuthStore()
  const hadIdentity = !!auth.user
  const restoration = auth.initialize()
  if (!to.meta.requiresAuth && to.name !== 'login')
    return

  await restoration
  if (to.meta.requiresAuth) {
    // 首次恢复或刚登录已经读取 me。后续进入时重新读取当前身份。
    if (hadIdentity && from.name !== 'login')
      await auth.loadIdentity()
    if (!auth.user && !auth.accountError)
      return { name: 'login', query: { redirect: to.fullPath } }
  }
})
