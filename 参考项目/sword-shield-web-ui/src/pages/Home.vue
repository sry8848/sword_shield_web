<template>
  <div class="home-container">
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">欢迎来到 Sword Shield</h1>
        <p class="hero-subtitle">体验最新的数字安全会议和资讯</p>
      </div>
    </section>

    <div class="content-wrapper">
      <el-row :gutter="24">
        <el-col :xs="24" :md="16">
          <!-- My Registered Activities Section -->

          <div class="section-card">
            <div class="section-header">
              <h2>🔥 热门活动</h2>
              <el-button text @click="$router.push('/activities')">查看全部 →</el-button>
            </div>
            <Activities minimal />
          </div>

          <div class="section-card poster-card">
            <div class="section-header">
              <h2>🎨 生成海报</h2>
            </div>
            <el-form @submit.prevent class="poster-form">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="海报标题">
                    <el-input v-model="title" placeholder="输入标题" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="副标题">
                    <el-input v-model="subtitle" placeholder="输入副标题" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-button type="primary" @click="generatePoster" :loading="generating">
                生成海报
              </el-button>
              <div v-if="posterUrl" class="poster-preview">
                <img :src="posterUrl" alt="Generated Poster" />
              </div>
            </el-form>
          </div>
        </el-col>

        <el-col :xs="24" :md="8">
          <div class="section-card">
            <div class="section-header">
              <h2>📰 最新资讯</h2>
              <el-button text @click="$router.push('/articles')">查看全部 →</el-button>
            </div>
            <Articles minimal />
          </div>

          <div v-if="isLoggedIn" class="section-card my-activities-card">
            <div class="section-header">
              <h2>📝 我报名的活动</h2>
              <el-button text @click="loadMyActivities">刷新</el-button>
            </div>
            <div v-if="myActivitiesLoading" class="loading-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <div v-else-if="myActivities.length === 0" class="empty-state">
              <p>📋 您还没有报名任何活动</p>
              <el-button type="primary" @click="$router.push('/activities')">去报名活动</el-button>
            </div>
            <ul v-else class="my-activities-list">
              <li v-for="activity in myActivities" :key="activity.id" class="my-activity-item">
                <div class="activity-info" @click="$router.push(`/activity/${activity.id}`)">
                  <div class="activity-title">
                    <span class="activity-icon">🎉</span>
                    <span>{{ activity.title }}</span>
                  </div>
                  <div class="activity-meta">
                    <span class="meta-item">
                      <span class="icon">📍</span>
                      {{ activity.location || '待定' }}
                    </span>
                    <span class="meta-item">
                      <span class="icon">📅</span>
                      {{ formatDateTime(activity.startTime) }}
                    </span>
                    <span class="meta-item channel-tag">
                      <span class="icon">🔗</span>
                      {{ getChannelLabel(activity.registrationChannel) }}
                    </span>
                  </div>
                </div>
                <el-tag :type="getActivityStatus(activity).type" size="small">
                  {{ getActivityStatus(activity).label }}
                </el-tag>
              </li>
            </ul>
            <div v-if="myActivitiesTotal > 5" class="view-all">
              <el-button text @click="$router.push('/profile')">查看全部 ({{ myActivitiesTotal }}) →</el-button>
            </div>
          </div>


        </el-col>


      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import Activities from './Activities.vue'
import Articles from './Articles.vue'
import { ref, onMounted, computed } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const title = ref('西湖论剑')
const subtitle = ref('数字安全大会')
const posterUrl = ref('')
const generating = ref(false)

const myActivities = ref<any[]>([])
const myActivitiesTotal = ref(0)
const myActivitiesLoading = ref(false)

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

function formatDateTime(dateStr: string) {
  if (!dateStr) return '时间待定'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getActivityStatus(activity: any) {
  const now = new Date()
  const startTime = activity.startTime ? new Date(activity.startTime) : null
  const endTime = activity.endTime ? new Date(activity.endTime) : null
  
  if (endTime && now > endTime) {
    return { label: '已结束', type: 'info' }
  }
  if (startTime && now >= startTime && (!endTime || now <= endTime)) {
    return { label: '进行中', type: 'success' }
  }
  if (startTime && now < startTime) {
    return { label: '未开始', type: 'warning' }
  }
  return { label: '待定', type: '' }
}

function getChannelLabel(channel: string) {
  const channelMap: Record<string, string> = {
    'web': '网页报名',
    'app': 'APP报名',
    'onsite': '现场报名',
    'share_link': '分享链接',
    'wechat': '微信报名',
    'admin': '后台添加'
  }
  return channelMap[channel] || channel || '网页报名'
}

async function loadMyActivities() {
  if (!isLoggedIn.value) return
  
  myActivitiesLoading.value = true
  try {
    const { data } = await api.get('/activity/my-registrations', { 
      params: { page: 0, size: 5 } 
    })
    if (data.code === 0) {
      myActivities.value = data.data.items
      myActivitiesTotal.value = data.data.total
    }
  } catch (error: any) {
    if (error.response?.status !== 401) {
      console.error('Failed to load registered activities:', error)
    }
  } finally {
    myActivitiesLoading.value = false
  }
}

async function generatePoster() {
  if (!title.value.trim()) {
    ElMessage.warning('请输入海报标题')
    return
  }
  
  generating.value = true
  try {
    const params = new URLSearchParams({ title: title.value, subtitle: subtitle.value })
    const { data } = await api.post('/content/poster', params)
    if (data.code === 0) {
      posterUrl.value = data.data
      ElMessage.success('海报生成成功')
    } else {
      ElMessage.error(data.message || '生成失败')
    }
  } catch (error) {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadMyActivities()
  }
})
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  margin-bottom: 32px;
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.hero-title {
  font-size: 48px;
  font-weight: bold;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 20px;
  color: #666;
}

.content-wrapper {
  margin-top: 24px;
}

.section-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
}

.section-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.section-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.poster-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.poster-form {
  margin-top: 16px;
}

.poster-preview {
  margin-top: 24px;
  text-align: center;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.poster-preview img {
  width: 100%;
  height: auto;
  display: block;
}

.my-activities-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-left: 4px solid #ff0050;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.loading-state .el-icon {
  font-size: 24px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.empty-state p {
  font-size: 15px;
  margin-bottom: 16px;
}

.my-activities-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.my-activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  margin-bottom: 12px;
  background: white;
  border-radius: 12px;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
}

.my-activity-item:hover {
  background: rgba(102, 126, 234, 0.05);
  border-color: #ff0050;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
}

.activity-info {
  flex: 1;
  cursor: pointer;
}

.activity-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.activity-icon {
  font-size: 18px;
}

.activity-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #666;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-item .icon {
  font-size: 14px;
}

.channel-tag {
  background: rgba(102, 126, 234, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
  color: #ff0050;
  font-weight: 500;
}

.view-all {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
