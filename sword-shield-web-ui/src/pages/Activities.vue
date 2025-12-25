<template>
  <div class="activities-container">
    <div class="activities-header" v-if="!minimal">
      <h2>
        <el-icon class="title-icon"><Calendar /></el-icon>
        活动列表
      </h2>
      <el-select v-model="orderBy" @change="fetchList" size="large" style="width:160px">
        <el-option label="按时间" value="time" />
        <el-option label="按热度" value="heat" />
      </el-select>
    </div>
    
    <div v-if="!minimal" class="activities-grid">
      <el-card 
        v-for="item in items" 
        :key="item.id" 
        class="activity-card"
        shadow="hover"
        @click="$router.push(`/activity/${item.id}`)"
      >
        <div class="card-cover" v-if="item.coverUrl">
          <img :src="item.coverUrl" :alt="item.title" />
        </div>
        <div class="card-content">
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-summary">{{ item.summary || '暂无描述' }}</p>
          <div class="card-meta">
            <div class="meta-item">
              <el-icon class="meta-icon"><Location /></el-icon>
              <span>{{ item.location || '待定' }}</span>
            </div>
            <div class="meta-item">
              <el-icon class="meta-icon"><Calendar /></el-icon>
              <span>{{ formatDate(item.startTime) }}</span>
            </div>
          </div>
          <div class="card-footer">
            <el-tag v-if="item.heat" type="danger" size="small">
              <el-icon class="heat-icon"><Lightning /></el-icon>
              {{ item.heat }}
            </el-tag>
            <el-button type="primary" size="small" text>查看详情 →</el-button>
          </div>
        </div>
      </el-card>
    </div>
    
    <ul v-else class="minimal-list">
      <li v-for="item in items" :key="item.id" class="minimal-item">
        <a @click.prevent="$router.push(`/activity/${item.id}`)" class="minimal-link">
          <span class="dot"></span>
          <span class="title">{{ item.title }}</span>
          <el-tag v-if="item.heat && item.heat > 50" type="danger" size="small" effect="plain">
            <el-icon class="heat-icon"><Lightning /></el-icon>
          </el-tag>
        </a>
      </li>
    </ul>

    <div v-if="!minimal && total > pageSize" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, defineProps } from 'vue'
import api from '../api'

defineProps<{ minimal?: boolean }>()

const items = ref<any[]>([])
const orderBy = ref('time')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

function formatDate(dateStr: string) {
  if (!dateStr) return '时间待定'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

async function fetchList() {
  const { data } = await api.get('/activity/list', { 
    params: { 
      page: currentPage.value - 1, 
      size: pageSize.value, 
      orderBy: orderBy.value 
    } 
  })
  if (data.code === 0) {
    items.value = data.data.items
    total.value = data.data.total
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(fetchList)
</script>

<style scoped>
.activities-container {
  max-width: 1200px;
  margin: 0 auto;
}

.activities-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
}

.activities-header h2 {
  font-size: 28px;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #409eff;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.activity-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 12px;
  overflow: hidden;
}

.activity-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.card-cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  padding: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-summary {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #888;
}

.meta-icon {
  font-size: 14px;
}

.heat-icon {
  margin-right: 4px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.minimal-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.minimal-item {
  margin-bottom: 12px;
}

.minimal-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  text-decoration: none;
  color: #333;
  transition: all 0.3s;
  cursor: pointer;
}

.minimal-link:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #ff0050;
}

.minimal-link .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff0050;
  flex-shrink: 0;
}

.minimal-link .title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 20px 0;
}
</style>

