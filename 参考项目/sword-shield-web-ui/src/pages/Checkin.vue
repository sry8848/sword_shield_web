<template>
  <div class="checkin-container">
    <el-card class="checkin-card">
      <div class="checkin-header">
        <div class="checkin-icon">✨</div>
        <h2>每日签到</h2>
        <p class="checkin-subtitle">坚持签到，赢取积分</p>
      </div>

      <div class="points-display">
        <div class="points-badge">
          <div class="points-icon">🏆</div>
          <div class="points-info">
            <span class="points-label">当前积分</span>
            <span class="points-value">{{ points }}</span>
          </div>
        </div>
      </div>

      <div class="checkin-action">
        <el-button 
          type="primary" 
          size="large" 
          @click="checkin" 
          :loading="checking"
          class="checkin-button"
        >
          <span v-if="!checking">✅ 立即签到</span>
          <span v-else>签到中...</span>
        </el-button>
      </div>

      <div class="checkin-tips">
        <el-alert 
          title="签到提示" 
          type="info" 
          :closable="false"
          show-icon
        >
          <template #default>
            <ul class="tips-list">
              <li>每日签到可获得10积分</li>
              <li>连续签到有额外奖励</li>
              <li>积分可用于兑换奖品</li>
            </ul>
          </template>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const points = ref(0)
const checking = ref(false)

async function loadPoints() {
  const token = localStorage.getItem('token')
  if (!token) return
  
  try {
    const { data } = await api.get('/user/points')
    if (data.code === 0) {
      points.value = data.data.total
    }
  } catch (error) {
    console.error('Failed to load points:', error)
  }
}

async function checkin() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  checking.value = true
  try {
    const { data } = await api.post('/user/checkin')
    if (data.code === 0) {
      ElMessage.success('签到成功！获得10积分 🎉')
      await loadPoints()
    } else {
      ElMessage.error(data.message || '签到失败')
    }
  } catch (error) {
    ElMessage.error('签到失败，请稍后重试')
  } finally {
    checking.value = false
  }
}

onMounted(loadPoints)
</script>

<style scoped>
.checkin-container {
  max-width: 600px;
  margin: 0 auto;
}

.checkin-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.checkin-header {
  text-align: center;
  margin-bottom: 32px;
}

.checkin-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.checkin-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px 0;
}

.checkin-subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.points-display {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

.points-badge {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 40px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-radius: 16px;
  border: 2px solid #ff0050;
}

.points-icon {
  font-size: 48px;
}

.points-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.points-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.points-value {
  font-size: 36px;
  font-weight: bold;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.checkin-action {
  text-align: center;
  margin-bottom: 32px;
}

.checkin-button {
  width: 100%;
  max-width: 300px;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  border: none;
  font-size: 18px;
  font-weight: 600;
  height: 50px;
}

.checkin-tips {
  margin-top: 24px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.tips-list li {
  color: #666;
  font-size: 14px;
}
</style>

