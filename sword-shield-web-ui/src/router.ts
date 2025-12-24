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
import AdminMaterials from './pages/admin/AdminMaterials.vue'
import Exhibitors from './pages/Exhibitors.vue'
import Achievements from './pages/Achievements.vue'
import Materials from './pages/Materials.vue'
import Media from './pages/Media.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/register', component: Register, meta: { public: true } },
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
      { path: 'activities', component: AdminActivities },
      { path: 'media', component: AdminMedia },
      { path: 'materials', component: AdminMaterials }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

type JwtPayload = {
  exp?: number
}

function getJwtExpiryMs(token: string): number | null {
  try {
    const payloadPart = token.split('.')[1]
    if (!payloadPart) return null
    const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const payload = JSON.parse(atob(padded)) as JwtPayload
    if (typeof payload.exp !== 'number') return null
    return payload.exp * 1000
  } catch {
    return null
  }
}

function isJwtExpired(token: string): boolean {
  const expMs = getJwtExpiryMs(token)
  if (!expMs) return true
  return Date.now() >= expMs
}

function clearUserAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('userAvatar')
}

function clearAdminAuth() {
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminId')
  localStorage.removeItem('adminUsername')
  localStorage.removeItem('adminNickname')
}

router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  // Admin routes authentication with deep link support
  if (to.path.startsWith('/admin')) {
    if (to.path !== '/admin/login') {
      const adminToken = localStorage.getItem('adminToken')
      if (!adminToken || isJwtExpired(adminToken)) {
        if (adminToken) {
          clearAdminAuth()
        }
        return next({
          path: '/admin/login',
          query: { redirect: to.fullPath }
        })
      }
    }
    return next()
  }

  const isPublic = to.matched.some((record) => record.meta.public)
  const token = localStorage.getItem('token')
  const tokenExpired = token ? isJwtExpired(token) : false
  if (token && tokenExpired) {
    clearUserAuth()
  }
  const action = Array.isArray(to.query.action) ? to.query.action[0] : (to.query.action as string | undefined)
  const requiresActionAuth = to.path.startsWith('/activity') && action === 'register'
  if ((!isPublic || requiresActionAuth) && (!token || tokenExpired)) {
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  next()
})

export default router
