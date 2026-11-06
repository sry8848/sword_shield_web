<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataBoard, Calendar, Tickets, Checked, TrendCharts } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
const route = useRoute(); const router = useRouter(); const auth = useAuthStore()
const activeMenu = computed(() => route.path)
function logout() { auth.logout(); router.push('/login') }
</script>
<template>
  <div class="admin-shell"><aside class="sidebar">
    <RouterLink class="brand" to="/"><span class="brand-mark">西</span><span><strong>西湖论剑</strong><small>运营管理平台</small></span></RouterLink>
    <div class="menu-caption">工作空间</div>
    <el-menu :default-active="activeMenu" router class="side-menu">
      <el-menu-item index="/"><el-icon><DataBoard /></el-icon><span>运营概览</span></el-menu-item>
      <el-menu-item index="/activities"><el-icon><Calendar /></el-icon><span>活动管理</span></el-menu-item>
      <el-menu-item index="/registrations"><el-icon><Tickets /></el-icon><span>报名管理</span></el-menu-item>
      <el-menu-item index="/checkin"><el-icon><Checked /></el-icon><span>现场核验</span></el-menu-item>
      <el-menu-item index="/statistics"><el-icon><TrendCharts /></el-icon><span>基础统计</span></el-menu-item>
    </el-menu>
    <div class="sidebar-bottom">MVP · 管理端骨架</div>
  </aside><div class="main-area"><header class="topbar"><div class="breadcrumb">{{ route.meta.title || '运营管理' }}</div><div class="top-actions"><span class="env-tag">本地演示</span><span class="operator">{{ auth.user?.displayName || '运营人员' }}</span><el-button text @click="logout">退出</el-button></div></header><main class="content-area"><RouterView /></main></div></div>
</template>
<style scoped>
.admin-shell { min-height: 100vh; display: flex; background: #f4f6fa; }
.sidebar { width: 228px; flex: 0 0 228px; background: #102b54; color: white; display: flex; flex-direction: column; }
.brand { height: 76px; padding: 0 20px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid rgba(255,255,255,.1); }
.brand-mark { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 10px; background: #246ce0; font-size: 20px; font-weight: 800; }
.brand strong,.brand small { display: block; } .brand strong { font-size: 14px; } .brand small { color: #9db5d5; font-size: 10px; margin-top: 4px; }
.menu-caption { padding: 24px 20px 10px; color: #7894ba; font-size: 11px; letter-spacing: 1px; }
.side-menu { border: 0; background: transparent; flex: 1; }
.side-menu :deep(.el-menu-item) { color: #c4d3e7; margin: 4px 10px; border-radius: 7px; height: 44px; }
.side-menu :deep(.el-menu-item:hover) { background: rgba(255,255,255,.08); color: white; }
.side-menu :deep(.el-menu-item.is-active) { background: #1b57a7; color: white; }
.sidebar-bottom { padding: 18px 20px; color: #7894ba; font-size: 11px; border-top: 1px solid rgba(255,255,255,.1); }
.main-area { min-width: 0; flex: 1; } .topbar { height: 64px; background: white; border-bottom: 1px solid #e8edf4; display: flex; justify-content: space-between; align-items: center; padding: 0 26px; }
.breadcrumb { font-size: 14px; font-weight: 600; } .top-actions { display: flex; align-items: center; gap: 16px; color: #68778b; font-size: 13px; }
.env-tag { padding: 4px 8px; border-radius: 4px; background: #eff6ff; color: #2764b3; font-size: 11px; } .content-area { min-width: 0; }
@media (max-width:760px) { .sidebar { width: 64px; flex-basis: 64px; } .brand { justify-content: center; padding: 0; } .brand > span:last-child,.menu-caption,.sidebar-bottom,.side-menu :deep(.el-menu-item span) { display: none; } .side-menu :deep(.el-menu-item) { justify-content: center; padding: 0 !important; margin: 5px; } .side-menu :deep(.el-menu-item .el-icon) { margin: 0; } .topbar { padding: 0 14px; } .operator { display: none; } }
</style>
