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
        <el-button type="primary" size="large" @click="$router.push('/admin/activities')" class="action-btn">
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
    const { data } = await adminApi.get('/activities')
    
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
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 120px;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card-1 .stat-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-card-2 .stat-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-card-3 .stat-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.stat-card-4 .stat-icon {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.stat-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  line-height: 1;
}

.welcome-card {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.welcome-card :deep(.el-card__header) {
  border-bottom: none;
  padding: 24px 24px 0;
}

.welcome-card :deep(.el-card__body) {
  padding: 0 24px 24px;
}

.card-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.welcome-text {
  margin: 0;
  color: #666;
  line-height: 1.8;
  font-size: 15px;
  flex: 1;
}

.action-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  padding: 12px 32px;
  height: auto;
}

.action-btn:hover {
  opacity: 0.9;
}
</style>
