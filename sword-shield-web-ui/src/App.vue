<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <span class="shield-icon">🛡️</span>
          <span class="logo-text">Sword Shield</span>
        </div>
        <nav class="nav-menu">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/activities" class="nav-link">活动</router-link>
          <router-link to="/articles" class="nav-link">资讯</router-link>
          <router-link to="/exhibitors" class="nav-link">展商</router-link>
          <router-link to="/achievements" class="nav-link">成果</router-link>
          <router-link to="/materials" class="nav-link">资料</router-link>
          <router-link to="/media" class="nav-link">媒体</router-link>
          <router-link to="/statistics" class="nav-link">统计</router-link>
          <router-link to="/submit" class="nav-link">投稿</router-link>
          <router-link to="/checkin" class="nav-link">签到</router-link>
        </nav>
        <div class="user-actions">
          <el-button 
            size="small" 
            @click="$router.push('/admin/login')" 
            class="btn-admin"
            :icon="Lock"
          >
            管理后台
          </el-button>
          <template v-if="isLoggedIn">
            <router-link to="/profile" class="btn-profile">
              <el-avatar :size="32" :src="userAvatar || undefined">{{ username?.charAt(0) || 'U' }}</el-avatar>
              <span class="username">{{ username }}</span>
            </router-link>
            <el-button size="small" @click="logout" text>退出</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
            <el-button size="small" @click="$router.push('/register')" plain>注册</el-button>
          </template>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="app-footer">
      <p>&copy; 2025 Sword Shield. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const username = ref(localStorage.getItem('username') || '')
const userAvatar = ref(localStorage.getItem('userAvatar') || '')
const token = ref(localStorage.getItem('token') || '')

const isLoggedIn = computed(() => !!token.value)

// Watch for route changes to update user info
watch(() => route.path, () => {
  username.value = localStorage.getItem('username') || ''
  userAvatar.value = localStorage.getItem('userAvatar') || ''
  token.value = localStorage.getItem('token') || ''
})

// Listen to custom login event
if (typeof window !== 'undefined') {
  window.addEventListener('user-login', () => {
    username.value = localStorage.getItem('username') || ''
    userAvatar.value = localStorage.getItem('userAvatar') || ''
    token.value = localStorage.getItem('token') || ''
  })
}

// Listen to storage events from other tabs/windows
if (typeof window !== 'undefined') {
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
  // Refresh user info on mount
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

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: linear-gradient(135deg, #000000 0%, #ff0050 50%, #00f2ea 100%);
  min-height: 100vh;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: nowrap;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.logo:hover {
  transform: scale(1.05);
}

.shield-icon {
  font-size: 32px;
}

.logo-text {
  font-size: 24px;
  font-weight: bold;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 50%, #ffffff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-menu {
  display: flex;
  gap: 16px;
  flex: 1;
  justify-content: center;
  align-items: center;
  flex-wrap: nowrap;
  overflow-x: auto;
  white-space: nowrap;
}

.nav-link {
  text-decoration: none;
  color: #333;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s;
  white-space: nowrap;
  flex-shrink: 0;
}

.nav-link:hover {
  background: #f5f5f5;
  color: #ff0050;
}

.nav-link.router-link-active {
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  color: white;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #333;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background 0.3s;
}

.btn-profile:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.btn-admin {
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  color: white;
  border: none;
  font-weight: 500;
}

.btn-admin:hover {
  opacity: 0.9;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  color: white;
}

.main-content {
  flex: 1;
  padding: 32px 16px;
}

.app-footer {
  background: rgba(255, 255, 255, 0.95);
  text-align: center;
  padding: 24px;
  color: #666;
  font-size: 14px;
}
</style>
