<template>
  <div class="submissions-page">
    <div class="page-header">
      <div class="title-wrap">
        <h2 class="page-title">
          <el-icon class="title-icon"><ChatLineSquare /></el-icon>
          用户投稿
        </h2>
        <p class="page-desc">展示已通过审核的用户投稿内容。</p>
      </div>
      <el-button type="primary" @click="$router.push('/submit')">
        在线投稿
      </el-button>
    </div>

    <div v-loading="loading" class="cards">
      <el-card
        v-for="item in items"
        :key="item.id"
        class="submission-card"
        shadow="hover"
        @click="openDetail(item)"
      >
        <div v-if="item.coverUrl" class="cover-wrap">
          <img :src="item.coverUrl" class="cover" alt="cover" />
        </div>
        <div class="card-body">
          <div class="card-meta">
            <el-tag type="info" size="small" effect="plain">用户投稿</el-tag>
            <span class="card-time">{{ formatDateTime(item.publishedAt || item.updatedAt || item.createdAt) }}</span>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-summary">{{ item.summary || '暂无摘要' }}</p>
          <div class="card-footer">
            <el-button type="primary" size="small" text>查看详情 →</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && items.length === 0" description="暂无投稿" class="empty" />

    <div class="pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 用户投稿列表页（UGC）。
 *
 * - 展示已发布的用户投稿内容（后端通常以 `category=UGC` 且 `status=published` 的文章为准）。
 * - 点击卡片进入文章详情页查看全文。
 *
 * 依赖接口：
 * - GET `/content/articles?category=UGC`
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChatLineSquare } from '@element-plus/icons-vue'
import api from '../api'

type Submission = {
  /** 投稿文章 ID。 */
  id: number

  /** 标题。 */
  title: string

  /** 摘要/简介（可为空）。 */
  summary?: string

  /** 正文（列表场景通常不返回/不使用）。 */
  body?: string

  /** 封面图片 URL（可为空）。 */
  coverUrl?: string | null

  /** 分类（UGC）。 */
  category?: string

  /** 状态（0-草稿，1-发布；以服务端约定为准）。 */
  status?: number

  /** 发布时间（发布后才有）。 */
  publishedAt?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

const loading = ref(false)
const items = ref<Submission[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const router = useRouter()

function formatDateTime(dateStr: string | null | undefined) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function fetchList() {
  loading.value = true
  try {
    const { data } = await api.get('/content/articles', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        category: 'UGC'
      }
    })
    if (data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
    } else {
      items.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openDetail(row: Submission) {
  if (!row?.id) return
  router.push(`/article/${row.id}`)
}

onMounted(fetchList)
</script>

<style scoped>
.submissions-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin: 6px 8px 16px;
}

.page-title {
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

.page-desc {
  margin: 6px 0 0;
  color: #6b7280;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.submission-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 14px;
  overflow: hidden;
}

.submission-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 14px 44px rgba(0, 0, 0, 0.14);
}

.cover-wrap {
  height: 180px;
  background: #f3f4f6;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-body {
  padding: 18px 18px 16px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.card-time {
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.card-title {
  margin: 10px 0 6px;
  font-size: 18px;
  color: #0f172a;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-summary {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  min-height: 62px;
}

.card-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.empty {
  margin-top: 28px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
