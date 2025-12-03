import { createRouter, createWebHistory } from 'vue-router'
import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import Login from './pages/Login.vue'
import Register from './pages/Register.vue'
import Home from './pages/Home.vue'
import Activities from './pages/Activities.vue'
import ActivityDetail from './pages/ActivityDetail.vue'
import Articles from './pages/Articles.vue'
import ArticleDetail from './pages/ArticleDetail.vue'
import Profile from './pages/Profile.vue'
import Checkin from './pages/Checkin.vue'
import Statistics from './pages/Statistics.vue'
import UGCSubmit from './pages/UGCSubmit.vue'
import AdminLogin from './pages/admin/AdminLogin.vue'
import AdminDashboard from './pages/admin/AdminDashboard.vue'
import AdminHome from './pages/admin/AdminHome.vue'
import AdminActivities from './pages/admin/AdminActivities.vue'
import AdminMedia from './pages/admin/AdminMedia.vue'
import Exhibitors from './pages/Exhibitors.vue'
import Achievements from './pages/Achievements.vue'
import Materials from './pages/Materials.vue'
import Media from './pages/Media.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/activities', component: Activities },
  { path: '/activity/:id', component: ActivityDetail },
  { path: '/articles', component: Articles },
  { path: '/article/:id', component: ArticleDetail },
  { path: '/profile', component: Profile },
  { path: '/checkin', component: Checkin },
  { path: '/statistics', component: Statistics },
  { path: '/exhibitors', component: Exhibitors },
  { path: '/achievements', component: Achievements },
  { path: '/materials', component: Materials },
  { path: '/media', component: Media },
  { path: '/submit', component: UGCSubmit },
  { path: '/admin/login', component: AdminLogin },
  {
    path: '/admin/dashboard',
    component: AdminDashboard,
    children: [
      { path: '', component: AdminHome },
      { path: '/admin/activities', component: AdminActivities },
      { path: '/admin/media', component: AdminMedia }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  // Admin routes authentication
  if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
    const adminToken = localStorage.getItem('adminToken')
    if (!adminToken) return next('/admin/login')
  }
  
  // Public routes authentication
  const token = localStorage.getItem('token')
  const action = Array.isArray(to.query.action) ? to.query.action[0] : (to.query.action as string | undefined)
  const requiresAuth = ['/profile'].includes(to.path) || (to.path.startsWith('/activity') && action === 'register')
  if (requiresAuth && !token) return next('/login')
  
  next()
})

export default router

