import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta { title?: string, requiresAuth?: boolean, permission?: string }
}

const adminPages = [
  { path: '', name: 'AdminDashboard', component: () => import('../pages/DashboardPage.vue'), meta: { title: '运营概览', permission: 'statistics:read' } },
  { path: 'activities', name: 'AdminActivities', component: () => import('../pages/ActivityManagementPage.vue'), meta: { title: '活动管理', permission: 'event:read' } },
  { path: 'registrations', name: 'AdminRegistrations', component: () => import('../pages/RegistrationManagementPage.vue'), meta: { title: '报名管理', permission: 'registration:read' } },
  { path: 'checkin', name: 'AdminCheckin', component: () => import('../pages/CheckinPage.vue'), meta: { title: '现场核验', permission: 'checkin:create' } },
  { path: 'statistics', name: 'AdminStatistics', component: () => import('../pages/StatisticsPage.vue'), meta: { title: '基础统计', permission: 'statistics:read' } },
]

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'AdminLogin', component: () => import('../pages/LoginPage.vue'), meta: { title: '后台登录' } },
    { path: '/', component: () => import('../layouts/AdminLayout.vue'), meta: { requiresAuth: true }, children: adminPages },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  // 1. 恢复会话，受保护页面需要已登录的后台身份。
  const auth = useAuthStore()
  try {
    await auth.hydrate()
  }
  catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '后台会话恢复失败')
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated)
    return { name: 'AdminLogin', query: { redirect: to.fullPath } }

  // 2. 根据后端权限决定可访问页面。没有概览权限的工作人员直接进入第一个授权页面。
  const firstPage = adminPages.find(page => auth.can(page.meta.permission))
  if (to.name === 'AdminLogin' && firstPage)
    return { name: firstPage.name }
  if (to.meta.permission && !auth.can(to.meta.permission)) {
    if (to.name !== 'AdminDashboard' || !firstPage)
      ElMessage.error('当前账号没有访问该页面的权限')
    return { name: firstPage?.name ?? 'AdminLogin' }
  }
  return true
})
