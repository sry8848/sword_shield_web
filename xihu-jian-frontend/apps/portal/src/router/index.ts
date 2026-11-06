import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta { title?: string; requiresAuth?: boolean; roles?: string[] }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [{
    path: '/', component: () => import('../layouts/PortalLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../pages/HomePage.vue'), meta: { title: '首页' } },
      { path: 'activities', name: 'ActivityList', component: () => import('../pages/ActivityListPage.vue'), meta: { title: '议程与参会' } },
      { path: 'activities/:activityId', name: 'ActivityDetail', component: () => import('../pages/ActivityDetailPage.vue'), meta: { title: '活动详情' } },
      { path: 'login', name: 'Login', component: () => import('../pages/LoginPage.vue'), meta: { title: '登录' } },
      { path: 'me/registrations', name: 'MyRegistrations', component: () => import('../pages/MyRegistrationsPage.vue'), meta: { title: '我的报名', requiresAuth: true } },
    ],
  }, { path: '/:pathMatch(.*)*', redirect: '/' }],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.hydrate()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return { name: 'Login', query: { redirect: to.fullPath } }
  if (to.meta.roles?.length && !to.meta.roles.includes(auth.user?.role ?? '')) return { name: 'Home' }
  return true
})
