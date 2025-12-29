<template>
  <div class="admin-container">
    <el-container>
      <el-aside width="250px" class="admin-sidebar">
        <div class="sidebar-header">
          <div class="logo-mark">
            <el-icon><DataBoard /></el-icon>
          </div>
          <div class="sidebar-brand-text">
            <h3>西湖论剑</h3>
            <p>管理后台 · 西湖论剑</p>
          </div>
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
          <el-menu-item index="articles">
            <el-icon><Reading /></el-icon>
            <span>资讯管理</span>
          </el-menu-item>
          <el-menu-item index="submissions">
            <el-icon><EditPen /></el-icon>
            <span>在线投稿</span>
          </el-menu-item>
          <el-menu-item index="media">
            <el-icon><Picture /></el-icon>
            <span>媒体管理</span>
          </el-menu-item>
          <el-menu-item index="materials">
            <el-icon><Document /></el-icon>
            <span>资料管理</span>
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
import { HomeFilled, Calendar, Picture, Document, Reading, EditPen, User, SwitchButton, DataBoard } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const adminUsername = ref(localStorage.getItem('adminUsername') || '')
const adminNickname = ref(localStorage.getItem('adminNickname') || '')

const activeMenu = computed(() => {
  if (route.path.includes('/admin/dashboard/activities')) return 'activities'
  if (route.path.includes('/admin/dashboard/articles')) return 'articles'
  if (route.path.includes('/admin/dashboard/submissions')) return 'submissions'
  if (route.path.includes('/admin/dashboard/media')) return 'media'
  if (route.path.includes('/admin/dashboard/materials')) return 'materials'
  return 'dashboard'
})

const pageTitle = computed(() => {
  if (route.path.includes('/admin/dashboard/activities')) return '活动管理'
  if (route.path.includes('/admin/dashboard/articles')) return '资讯管理'
  if (route.path.includes('/admin/dashboard/submissions')) return '在线投稿'
  if (route.path.includes('/admin/dashboard/media')) return '媒体资产管理'
  if (route.path.includes('/admin/dashboard/materials')) return '资料管理'
  return '仪表盘'
})

function handleMenuSelect(index: string) {
  if (index === 'dashboard') {
    router.push('/admin/dashboard')
  } else if (index === 'activities') {
    router.push('/admin/dashboard/activities')
  } else if (index === 'articles') {
    router.push('/admin/dashboard/articles')
  } else if (index === 'submissions') {
    router.push('/admin/dashboard/submissions')
  } else if (index === 'media') {
    router.push('/admin/dashboard/media')
  } else if (index === 'materials') {
    router.push('/admin/dashboard/materials')
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
  background: radial-gradient(circle at top, #e3eeff 0, #f3f6fb 45%, #eef2f7 100%);
  overflow: hidden;
}

.admin-container :deep(.el-container) {
  height: 100vh;
}

.admin-sidebar {
  background: #f5f9ff;
  color: #111827;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(191, 219, 254, 0.9);
  box-shadow: 2px 0 10px rgba(148, 163, 184, 0.2);
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(209, 213, 219, 0.7);
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.sidebar-brand-text h3 {
  margin: 0 0 2px 0;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-brand-text p {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
}

.sidebar-menu {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #111827;
  margin: 4px 10px;
  border-radius: 999px;
  height: 40px;
  line-height: 40px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #e0ecff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #2563eb;
  color: #ffffff;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 16px;
}

.sidebar-footer {
  margin-top: auto;
  padding: 10px 14px 16px;
  border-top: 1px solid rgba(209, 213, 219, 0.9);
  background: #eef2ff;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  margin-bottom: 6px;
  background: #e5edff;
  border-radius: 12px;
  font-size: 13px;
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
  color: #b91c1c;
  font-size: 13px;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
  border-radius: 999px;
}

.logout-btn:hover {
  background: rgba(248, 113, 113, 0.08);
}

.admin-header {
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 1px 6px rgba(148, 163, 184, 0.25);
  display: flex;
  align-items: center;
  padding: 0 32px;
  border-bottom: 1px solid rgba(209, 213, 219, 0.8);
}

.admin-header h2 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.admin-main {
  padding: 20px 24px 22px;
  overflow-y: auto;
  height: calc(100vh - 60px);
  background: transparent;
}
</style>
