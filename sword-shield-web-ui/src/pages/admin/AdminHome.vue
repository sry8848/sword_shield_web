<template>
  <div class="admin-home">
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <div class="stat-card stat-card-1">
          <div class="stat-icon">
            <el-icon :size="40"><Calendar /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">总活动数</div>
            <div class="stat-value">{{ stats.totalActivities }}</div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card stat-card-2">
          <div class="stat-icon">
            <el-icon :size="40"><SuccessFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">进行中</div>
            <div class="stat-value">{{ stats.activeActivities }}</div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card stat-card-3">
          <div class="stat-icon">
            <el-icon :size="40"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">总报名数</div>
            <div class="stat-value">{{ stats.totalRegistrations }}</div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card stat-card-4">
          <div class="stat-icon">
            <el-icon :size="40"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">总热度</div>
            <div class="stat-value">{{ stats.totalHeat }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>欢迎，{{ adminNickname || adminUsername }}！</h2>
        </div>
      </template>
      <div class="welcome-content">
        <p class="welcome-text">这里是西湖论剑后台管理系统。您可以通过左侧菜单管理活动信息。</p>
        <el-button
          type="primary"
          size="large"
          @click="$router.push('/admin/dashboard/activities')"
          class="action-btn"
        >
          <el-icon><Calendar /></el-icon>
          开始管理活动
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import adminApi from '../../adminApi'
import { Calendar, SuccessFilled, User, TrendCharts } from '@element-plus/icons-vue'

const adminUsername = ref(localStorage.getItem('adminUsername') || '')
const adminNickname = ref(localStorage.getItem('adminNickname') || '')

const stats = ref({
  totalActivities: 0,
  activeActivities: 0,
  totalRegistrations: 0,
  totalHeat: 0
})

async function loadStats() {
  try {
    const { data } = await adminApi.get('/admin/activities')
    
    if (data.code === 0) {
      const activities = data.data
      stats.value.totalActivities = activities.length
      stats.value.activeActivities = activities.filter((a: any) => a.status === 1).length
      stats.value.totalRegistrations = activities.reduce((sum: number, a: any) => sum + (a.currentRegistrations || 0), 0)
      stats.value.totalHeat = activities.reduce((sum: number, a: any) => sum + (a.heat || 0), 0)
    }
  } catch (error) {
    console.error('Failed to load stats', error)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.admin-home {
  padding: 4px 0 8px;
  animation: fadeIn 0.25s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 16px 14px;
  box-shadow: 0 4px 12px rgba(148, 163, 184, 0.25);
  border: 1px solid rgba(209, 213, 219, 0.9);
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 90px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #1d4ed8;
  background: #e0ecff;
}

/* 用不同边框/浅色区分四个卡片，整体保持扁平 */
.stat-card-1 .stat-icon {
  color: #1d4ed8;
  background: #e0ecff;
}

.stat-card-2 .stat-icon {
  color: #15803d;
  background: #dcfce7;
}

.stat-card-3 .stat-icon {
  color: #0369a1;
  background: #e0f2fe;
}

.stat-card-4 .stat-icon {
  color: #b45309;
  background: #ffedd5;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.welcome-card {
  border-radius: 18px;
  box-shadow: 0 4px 12px rgba(148, 163, 184, 0.3);
  border: 1px solid rgba(209, 213, 219, 0.9);
  background: #ffffff;
}

.welcome-card :deep(.el-card__header) {
  border-bottom: none;
  padding: 18px 20px 0;
}

.welcome-card :deep(.el-card__body) {
  padding: 4px 20px 18px;
}

.card-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 600;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.welcome-text {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
  font-size: 14px;
  flex: 1;
}

.action-btn {
  background: #2563eb;
  border: none;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 22px;
  height: auto;
}

.action-btn:hover {
  filter: brightness(1.02);
}

@media (max-width: 1024px) {
  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
