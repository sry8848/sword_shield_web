<template>
  <div class="app-shell">
    <template v-if="!isAdminRoute">
      <header v-if="!isAuthRoute" class="app-header">
        <div class="top-bar">
          <div class="top-left">
            <span class="top-label">西湖论剑 数字安全平台</span>
            <span class="top-divider" />
            <span class="top-sub">会议 · 活动 · 专家 · 成果</span>
          </div>
          <div class="top-right">
            <template v-if="isLoggedIn">
              <router-link to="/profile" class="top-user">
                <el-avatar :size="28" :src="userAvatar || undefined">
                  {{ username?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="username">{{ username }}</span>
              </router-link>
              <el-button size="small" text class="top-link" @click="logout">退出</el-button>
            </template>
            <template v-else>
              <el-button size="small" text class="top-link" @click="$router.push('/login')">登录</el-button>
              <span class="top-split">|</span>
              <el-button size="small" text class="top-link" @click="$router.push('/register')">注册</el-button>
              <el-tooltip content="仅供大会工作人员使用" placement="bottom">
                <el-button
                  size="small"
                  class="top-admin-btn"
                  :icon="Lock"
                  @click="$router.push('/admin/login')"
                >
                  管理员入口
                </el-button>
              </el-tooltip>
            </template>
          </div>
        </div>

        <div class="nav-bar">
            <div class="nav-inner">
          <div class="brand" @click="$router.push('/')">
              <div class="brand-mark">
                <el-icon class="brand-icon"><DataBoard /></el-icon>
              </div>
              <div class="brand-text">
                <div class="brand-cn">西湖论剑·数字安全</div>
                <div class="brand-en">SWORD SHIELD CYBER SECURITY</div>
              </div>
            </div>

            <nav class="nav-menu">
              <router-link to="/" class="nav-link">首页</router-link>
              <router-link to="/activities" class="nav-link">活动日程</router-link>
              <router-link to="/articles" class="nav-link">新闻资讯</router-link>
              <router-link to="/exhibitors" class="nav-link">参展单位</router-link>
              <router-link to="/achievements" class="nav-link">成果展示</router-link>
              <router-link to="/materials" class="nav-link">资料下载</router-link>
              <router-link to="/media" class="nav-link">媒体中心</router-link>
              <router-link to="/statistics" class="nav-link">数据看板</router-link>
              <router-link to="/submissions" class="nav-link">用户投稿</router-link>
              <router-link to="/points" class="nav-link">积分详情</router-link>
            </nav>
          </div>
        </div>
      </header>

      <main class="app-main" :class="{ 'auth-main': isAuthRoute }">
        <router-view />
      </main>

      <footer v-if="!isAuthRoute" class="app-footer">
        <div class="footer-inner">
          <div class="footer-left">
            <div class="footer-title">西湖论剑 数字安全平台</div>
            <div class="footer-desc">为会议活动提供一站式数字安全支撑与智能运营能力。</div>
          </div>
          <div class="footer-right">
            <span>© 2025 西湖论剑. All rights reserved.</span>
          </div>
        </div>
      </footer>
    </template>
    <template v-else>
      <!-- Admin routes: render only admin layout from admin components -->
      <router-view />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { DataBoard, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const username = ref(localStorage.getItem('username') || '')
const userAvatar = ref(localStorage.getItem('userAvatar') || '')
const token = ref(localStorage.getItem('token') || '')

const isLoggedIn = computed(() => !!token.value)
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isAuthRoute = computed(() => route.path === '/login' || route.path === '/register')

watch(
  () => route.path,
  () => {
    username.value = localStorage.getItem('username') || ''
    userAvatar.value = localStorage.getItem('userAvatar') || ''
    token.value = localStorage.getItem('token') || ''
  }
)

if (typeof window !== 'undefined') {
  window.addEventListener('user-login', () => {
    username.value = localStorage.getItem('username') || ''
    userAvatar.value = localStorage.getItem('userAvatar') || ''
    token.value = localStorage.getItem('token') || ''
  })

  window.addEventListener('storage', (e) => {
    if (e.key === 'username' || e.key === 'userAvatar' || e.key === 'token') {
      username.value = localStorage.getItem('username') || ''
      userAvatar.value = localStorage.getItem('userAvatar') || ''
      token.value = localStorage.getItem('token') || ''
    }
  })
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('userAvatar')
  username.value = ''
  userAvatar.value = ''
  token.value = ''
  router.push('/login')
}

onMounted(() => {
  username.value = localStorage.getItem('username') || ''
  userAvatar.value = localStorage.getItem('userAvatar') || ''
  token.value = localStorage.getItem('token') || ''
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Segoe UI', sans-serif;
  background: #f3f6fb;
  color: #1f2933;
}

.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at top, #e3eeff 0, #f3f6fb 45%, #eef2f7 100%);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  backdrop-filter: blur(16px);
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.top-bar {
  max-width: 1240px;
  margin: 0 auto;
  padding: 6px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.top-label {
  color: #1f2933;
}

.top-sub {
  color: #9ca3af;
}

.top-divider {
  width: 1px;
  height: 14px;
  background: linear-gradient(to bottom, transparent, #d1d5db, transparent);
}

.top-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-link {
  color: #2563eb;
  padding: 0 4px;
}

.top-link:hover {
  color: #1d4ed8;
}

.top-split {
  color: #d1d5db;
}

.top-admin-btn {
  margin-left: 4px;
  border-color: rgba(37, 99, 235, 0.35);
  color: #1d4ed8;
}

.top-admin-btn:hover {
  background: rgba(37, 99, 235, 0.06);
}

.top-user {
  display: flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  color: #111827;
}

.top-user .username {
  max-width: 120px;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.nav-bar {
  border-top: 1px solid rgba(15, 23, 42, 0.03);
}

.nav-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 10px 24px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: radial-gradient(circle at 30% 0, #2563eb, #60a5fa 35%, #e5f0ff 100%);
  box-shadow: 0 0 0 1px rgba(191, 219, 254, 0.8), 0 10px 20px rgba(15, 23, 42, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f9fafb;
}

.brand-icon {
  font-size: 20px;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-cn {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #111827;
}

.brand-en {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #9ca3af;
}

.nav-menu {
  display: flex;
  flex: 1;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.nav-link {
  position: relative;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 14px;
  text-decoration: none;
  color: #4b5563;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 3px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(to right, #2563eb, #38bdf8);
  transform-origin: center;
  transform: scaleX(0);
  opacity: 0;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.nav-link:hover {
  color: #1d4ed8;
  background-color: rgba(219, 234, 254, 0.6);
}

.nav-link.router-link-active {
  color: #1d4ed8;
  background-color: rgba(219, 234, 254, 0.9);
}

.nav-link.router-link-active::after {
  transform: scaleX(1);
  opacity: 1;
}

.app-main {
  flex: 1;
  padding: 24px 16px 40px;
}

.app-main.auth-main {
  padding: 0;
}

.app-footer {
  border-top: 1px solid rgba(15, 23, 42, 0.05);
  background: #ffffff;
}

.footer-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 12px;
  color: #6b7280;
}

.footer-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.footer-desc {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .nav-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .nav-menu {
    justify-content: flex-start;
    row-gap: 6px;
  }

  .app-main {
    padding: 16px 12px 32px;
  }

  .top-bar {
    padding-inline: 12px;
  }

  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
