<template>
  <div class="admin-container">
    <el-container>
      <el-aside width="250px" class="admin-sidebar">
        <div class="sidebar-header">
          <div class="logo">🛡️</div>
          <h3>西湖论剑</h3>
          <p>管理后台</p>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="activities">
            <el-icon><Calendar /></el-icon>
            <span>活动管理</span>
          </el-menu-item>
          <el-menu-item index="media">
            <el-icon><Picture /></el-icon>
            <span>媒体管理</span>
          </el-menu-item>
        </el-menu>
        <div class="sidebar-footer">
          <div class="admin-info">
            <div class="info-icon">
              <el-icon><User /></el-icon>
            </div>
            <span class="info-text">系统管理员</span>
          </div>
          <el-button link type="danger" @click="logout" class="logout-btn">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-button>
        </div>
      </el-aside>
      
      <el-container>
        <el-header class="admin-header">
          <h2>{{ pageTitle }}</h2>
        </el-header>
        
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { HomeFilled, Calendar, Picture, User, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const adminUsername = ref(localStorage.getItem('adminUsername') || '')
const adminNickname = ref(localStorage.getItem('adminNickname') || '')

const activeMenu = computed(() => {
  if (route.path.includes('/admin/activities')) return 'activities'
  if (route.path.includes('/admin/media')) return 'media'
  return 'dashboard'
})

const pageTitle = computed(() => {
  if (route.path.includes('/admin/activities')) return '活动管理'
  if (route.path.includes('/admin/media')) return '媒体资产管理'
  return '仪表盘'
})

function handleMenuSelect(index: string) {
  if (index === 'dashboard') {
    router.push('/admin/dashboard')
  } else if (index === 'activities') {
    router.push('/admin/activities')
  } else if (index === 'media') {
    router.push('/admin/media')
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminId')
    localStorage.removeItem('adminUsername')
    localStorage.removeItem('adminNickname')
    
    ElMessage.success('已退出登录')
    router.push('/admin/login')
  } catch {
    // User cancelled
  }
}
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  height: 100vh;
  background-color: #f5f7fa;
  overflow: hidden;
}

.admin-container :deep(.el-container) {
  height: 100vh;
}

.admin-sidebar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.sidebar-header {
  text-align: center;
  padding: 32px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.logo {
  font-size: 48px;
  margin-bottom: 12px;
}

.sidebar-header h3 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
}

.sidebar-header p {
  margin: 0;
  font-size: 12px;
  opacity: 0.8;
}

.sidebar-menu {
  flex: 1;
  border: none;
  background: transparent;
  padding: 16px 0;
}

.sidebar-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.9);
  margin: 4px 12px;
  border-radius: 8px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(0, 0, 0, 0.1);
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
  transition: background 0.3s;
}

.admin-info:hover {
  background: rgba(255, 255, 255, 0.15);
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.info-text {
  flex: 1;
  font-weight: 500;
}

.logout-btn {
  width: 100%;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-start;
  border-radius: 8px;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.admin-header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  padding: 0 32px;
}

.admin-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.admin-main {
  padding: 24px;
  overflow-y: auto;
  height: calc(100vh - 60px);
}
</style>
