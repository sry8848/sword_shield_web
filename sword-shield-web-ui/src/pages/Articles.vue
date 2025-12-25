<template>
  <div class="articles-container">
    <h2 v-if="!minimal" class="articles-title">
      <el-icon class="title-icon"><Reading /></el-icon>
      资讯列表
    </h2>
    
    <div v-if="!minimal" class="articles-grid">
      <el-card 
        v-for="item in items" 
        :key="item.id" 
        class="article-card"
        shadow="hover"
        @click="$router.push(`/article/${item.id}`)"
      >
        <div class="card-header">
          <el-tag :type="getCategoryType(item.category)" size="small">{{ item.category || '通用' }}</el-tag>
          <span class="publish-date">{{ formatDate(item.publishedAt) }}</span>
        </div>
        <h3 class="article-title">{{ item.title }}</h3>
        <p class="article-summary">{{ item.summary || '暂无摘要' }}</p>
        <div class="card-footer">
          <el-button type="primary" size="small" text>阅读全文 →</el-button>
        </div>
      </el-card>
    </div>
    
    <ul v-else class="minimal-list">
      <li v-for="item in items" :key="item.id" class="minimal-item">
        <a @click.prevent="$router.push(`/article/${item.id}`)" class="minimal-link">
          <span class="dot"></span>
          <span class="title">{{ item.title }}</span>
          <el-tag :type="getCategoryType(item.category)" size="small" effect="plain">{{ item.category }}</el-tag>
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
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

function formatDate(dateStr: string) {
  if (!dateStr) return '日期未知'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function getCategoryType(category: string) {
  const types: Record<string, any> = {
    '技术': 'primary',
    '安全': 'danger',
    '行业': 'success',
    '通知': 'warning'
  }
  return types[category] || 'info'
}

async function fetchList() {
  const { data } = await api.get('/content/articles', { 
    params: { 
      page: currentPage.value - 1, 
      size: pageSize.value 
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
.articles-container {
  max-width: 1200px;
  margin: 0 auto;
}

.articles-title {
  font-size: 28px;
  color: #333;
  margin: 0 0 24px 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #409eff;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.article-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 12px;
  padding: 20px;
}

.article-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.publish-date {
  font-size: 13px;
  color: #999;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-summary {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.card-footer {
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

