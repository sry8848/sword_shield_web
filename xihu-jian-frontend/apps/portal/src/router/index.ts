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

/** 访问受保护的页面时，把已匹配的站内目标交给登录页。 */
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useAuthStore().user)
    return { name: 'login', query: { redirect: to.fullPath } }
})
