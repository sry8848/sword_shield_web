import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
declare module 'vue-router' { interface RouteMeta { title?: string; requiresAuth?: boolean; roles?: string[] } }
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'AdminLogin', component: () => import('../pages/LoginPage.vue'), meta: { title: '后台登录' } },
    { path: '/', component: () => import('../layouts/AdminLayout.vue'), meta: { requiresAuth: true, roles: ['ADMIN', 'OPS', 'CHECKIN'] }, children: [
      { path: '', name: 'AdminDashboard', component: () => import('../pages/DashboardPage.vue'), meta: { title: '运营概览' } },
      { path: 'activities', name: 'AdminActivities', component: () => import('../pages/ActivityManagementPage.vue'), meta: { title: '活动管理' } },
      { path: 'registrations', name: 'AdminRegistrations', component: () => import('../pages/RegistrationManagementPage.vue'), meta: { title: '报名管理' } },
      { path: 'checkin', name: 'AdminCheckin', component: () => import('../pages/CheckinPage.vue'), meta: { title: '现场核验' } },
      { path: 'statistics', name: 'AdminStatistics', component: () => import('../pages/StatisticsPage.vue'), meta: { title: '基础统计' } },
    ] },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.hydrate()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return { name: 'AdminLogin', query: { redirect: to.fullPath } }
  if (to.meta.roles?.length && !to.meta.roles.includes(auth.user?.role ?? '')) return { name: 'AdminLogin' }
  if (to.name === 'AdminLogin' && auth.isAuthenticated && auth.user?.role === 'ADMIN') return { name: 'AdminDashboard' }
  return true
})
