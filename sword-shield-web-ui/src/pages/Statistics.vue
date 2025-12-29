<template>
  <div class="statistics-container">
    <el-card class="header-card">
      <h1 class="page-title">
        <el-icon class="title-icon"><DataAnalysis /></el-icon>
        活动统计
      </h1>
      <p class="page-subtitle">查看各项活动的参与情况和数据分析</p>
    </el-card>

    <el-card class="stats-overview">
      <h2>总体概览</h2>
      <el-row :gutter="24" class="overview-row">
        <el-col :span="6">
          <div class="stat-box stat-box-1">
            <el-icon class="stat-icon"><Aim /></el-icon>
            <div class="stat-content">
              <div class="stat-label">活动总数</div>
              <div class="stat-value">{{ totalActivities }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box stat-box-2">
            <el-icon class="stat-icon"><UserFilled /></el-icon>
            <div class="stat-content">
              <div class="stat-label">总报名人数</div>
              <div class="stat-value">{{ totalRegistrations }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box stat-box-3">
            <el-icon class="stat-icon"><Lightning /></el-icon>
            <div class="stat-content">
              <div class="stat-label">总热度</div>
              <div class="stat-value">{{ totalHeat }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box stat-box-4">
            <el-icon class="stat-icon"><TrendCharts /></el-icon>
            <div class="stat-content">
              <div class="stat-label">平均报名率</div>
              <div class="stat-value">{{ averageRate }}%</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="activity-stats">
      <h2>活动详细统计</h2>
      <el-table :data="activities" v-loading="loading" stripe class="stats-table">
        <el-table-column prop="activityTitle" label="活动名称" min-width="200" />
        <el-table-column label="报名人数" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="success">{{ row.registrationCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名上限" width="120" align="center">
          <template #default="{ row }">
            {{ row.maxRegistrations || '无限制' }}
          </template>
        </el-table-column>
        <el-table-column label="报名率" width="120" align="center">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.registrationRate" 
              :color="getProgressColor(row.registrationRate)"
              :stroke-width="12"
            />
          </template>
        </el-table-column>
        <el-table-column prop="heat" label="热度" width="100" align="center" sortable />
        <el-table-column label="开始时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              link
              @click="viewDetail(row.activityId)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activities = ref<any[]>([])
const loading = ref(false)

const totalActivities = computed(() => activities.value.length)
const totalRegistrations = computed(() => 
  activities.value.reduce((sum, a) => sum + (a.registrationCount || 0), 0)
)
const totalHeat = computed(() => 
  activities.value.reduce((sum, a) => sum + (a.heat || 0), 0)
)
const averageRate = computed(() => {
  const validActivities = activities.value.filter(a => a.maxRegistrations > 0)
  if (validActivities.length === 0) return 0
  const sum = validActivities.reduce((s, a) => s + (a.registrationRate || 0), 0)
  return Math.round((sum / validActivities.length) * 100) / 100
})

function getProgressColor(percentage: number) {
  if (percentage >= 80) return '#f56c6c'
  if (percentage >= 50) return '#e6a23c'
  return '#67c23a'
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function viewDetail(activityId: number) {
  router.push(`/activity/${activityId}`)
}

async function loadStatistics() {
  loading.value = true
  try {
    const { data } = await api.get('/activity/statistics/all')
    if (data.code === 0) {
      activities.value = data.data
    } else {
      ElMessage.error(data.message || '加载统计数据失败')
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-container {
  max-width: 1200px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 24px;
  text-align: center;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 32px;
  color: #333;
  font-weight: 700;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #409eff;
}

.page-subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.stats-overview {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stats-overview h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.overview-row {
  margin-top: 16px;
}

.stat-box {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
}

.stat-box:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-box-1 .stat-icon {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
}

.stat-box-2 .stat-icon {
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
}

.stat-box-3 .stat-icon {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
}

.stat-box-4 .stat-icon {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.activity-stats {
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.activity-stats h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.stats-table {
  border-radius: 8px;
  overflow: hidden;
}

.stats-table :deep(.el-progress__text) {
  font-size: 12px !important;
  font-weight: 600;
}
</style>
