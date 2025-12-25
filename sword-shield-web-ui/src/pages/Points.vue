<template>
  <div class="points-container">
    <el-card class="points-card">
      <div class="points-header">
        <el-icon class="points-icon"><Trophy /></el-icon>
        <div>
          <h2>积分</h2>
          <p class="points-subtitle">活动签到获得积分，明细实时更新</p>
        </div>
      </div>

      <div v-if="!isLoggedIn" class="points-empty">
        <el-empty description="请先登录查看积分">
          <el-button type="primary" @click="router.push('/login')">去登录</el-button>
        </el-empty>
      </div>

      <template v-else>
        <div class="points-display">
          <div class="points-badge">
            <div class="points-info">
              <span class="points-label">当前积分</span>
              <span class="points-value">{{ totalPoints }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="txn-section">
          <div class="txn-header">
            <h3>积分明细</h3>
            <el-button size="small" @click="reload" :loading="loading">刷新</el-button>
          </div>

          <el-table :data="txns" v-loading="loading" style="width: 100%;" empty-text="暂无明细">
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="note" label="说明" min-width="200" />
            <el-table-column prop="type" label="类型" width="160">
              <template #default="{ row }">{{ formatType(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="points" label="积分" width="100" align="right" />
          </el-table>

          <div class="txn-pagination">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="size"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              :total="total"
              @size-change="reload"
              @current-change="reload"
            />
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const totalPoints = ref(0)
const txns = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

async function loadPoints() {
  const { data } = await api.get('/user/points')
  if (data.code === 0) {
    totalPoints.value = data.data.total ?? 0
  }
}

async function loadTxns() {
  const { data } = await api.get('/user/points/txns', {
    params: { page: page.value - 1, size: size.value }
  })
  if (data.code === 0) {
    txns.value = data.data.items ?? []
    total.value = data.data.total ?? 0
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatType(type: string) {
  if (type === 'activity_checkin') return '活动签到'
  if (type === 'daily_checkin') return '每日签到'
  return type || '-'
}

async function reload() {
  if (!isLoggedIn.value) return
  loading.value = true
  try {
    await Promise.all([loadPoints(), loadTxns()])
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.points-container {
  max-width: 720px;
  margin: 0 auto;
}

.points-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.points-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.points-icon {
  font-size: 44px;
}

.points-header h2 {
  font-size: 22px;
  color: #333;
  margin: 0;
}

.points-subtitle {
  color: #666;
  font-size: 14px;
  margin: 6px 0 0 0;
}

.points-display {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

.points-badge {
  display: flex;
  align-items: center;
  padding: 22px 44px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-radius: 16px;
  border: 2px solid #ff0050;
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

.txn-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.txn-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.txn-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.points-empty {
  padding: 20px 0 6px;
}
</style>

