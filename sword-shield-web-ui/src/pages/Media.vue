<template>
  <div class="media-page">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon class="title-icon"><Film /></el-icon>
        大会精彩瞬间
      </h1>
      <p class="page-subtitle">浏览我们的图片和视频花絮</p>
    </div>

    <!-- Filter Tabs -->
    <el-card class="filter-card">
      <el-radio-group v-model="filterType" @change="loadMedia(1)" size="large">
        <el-radio-button label="">
          <el-icon class="tab-icon"><Grid /></el-icon>
          全部
        </el-radio-button>
        <el-radio-button label="image">
          <el-icon class="tab-icon"><Picture /></el-icon>
          图片
        </el-radio-button>
        <el-radio-button label="video">
          <el-icon class="tab-icon"><VideoCamera /></el-icon>
          视频
        </el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- Media Grid -->
    <div class="media-grid" v-loading="loading">
      <div v-if="items.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Camera /></el-icon>
        <p>暂无媒体内容</p>
      </div>
      
      <el-card 
        v-for="item in items" 
        :key="item.id" 
        class="media-card"
        shadow="hover"
        @click="openPreview(item)"
      >
        <div class="media-thumbnail">
          <div v-if="item.type === 'image'" class="thumbnail-image">
            <img v-if="getThumbSrc(item)" :src="getThumbSrc(item)" class="thumbnail-cover" alt="缩略图" />
            <el-icon v-else class="media-icon"><Picture /></el-icon>
            <div class="media-type-badge image-badge">
              <el-icon class="badge-icon"><Picture /></el-icon>
              图片
            </div>
          </div>
          <div v-else class="thumbnail-video">
            <el-icon class="media-icon"><VideoPlay /></el-icon>
            <div class="media-type-badge video-badge">
              <el-icon class="badge-icon"><VideoCamera /></el-icon>
              视频
            </div>
          </div>
        </div>
        <div class="media-info">
          <h3 class="media-title">{{ item.title }}</h3>
          <p class="media-summary" v-if="item.summary">{{ item.summary }}</p>
          <div class="media-footer">
            <span class="media-date">
              <el-icon class="date-icon"><Calendar /></el-icon>
              {{ formatDate(item.createdAt) }}
            </span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        @current-change="loadMedia"
      />
    </div>

    <!-- Preview Dialog -->
    <el-dialog 
      v-model="previewVisible" 
      :title="currentMedia?.title"
      width="80%"
      center
      destroy-on-close
    >
      <div class="preview-container">
        <img 
          v-if="currentMedia?.type === 'image'" 
          :src="previewUrl" 
          class="preview-image"
          alt="预览图片"
        />
        <video 
          v-else 
          :src="previewUrl" 
          controls 
          autoplay
          class="preview-video"
        ></video>
        <p v-if="currentMedia?.summary" class="preview-description">
          {{ currentMedia.summary }}
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 媒体中心页。
 *
 * - 展示图片/视频等媒体资产，支持按类型筛选与预览。
 * - 通过文件服务的预签名 URL 获取可访问的临时链接。
 *
 * 依赖接口：
 * - GET `/content/media`：媒体列表（可按 type 过滤）
 * - GET `/file/presigned`：获取预览链接
 *
 * 数据字段主要对应后端 `MediaAssetDto`：`id/title/summary/type/objectName/coverUrl/articleId/createdAt`。
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Camera, Film, Grid, Picture, VideoCamera, VideoPlay } from '@element-plus/icons-vue'
import api from '../api'

const items = ref<any[]>([])
const total = ref(0)
const pageSize = ref(12)
const filterType = ref('')
const loading = ref(false)

const previewVisible = ref(false)
const previewUrl = ref('')
const currentMedia = ref<any>(null)
const thumbUrlMap = ref<Record<string, string>>({})

function isProbablyUrl(value: any) {
  if (!value || typeof value !== 'string') return false
  return /^https?:\/\//i.test(value) || value.startsWith('/') || value.startsWith('data:') || value.startsWith('blob:')
}

function resolveObjectName(item: any) {
  return item?.objectName || item?.coverUrl || ''
}

function getThumbSrc(item: any) {
  if (!item) return ''
  const cached = thumbUrlMap.value[String(item.id)]
  if (cached) return cached
  if (isProbablyUrl(item.coverUrl)) return item.coverUrl
  return ''
}

async function ensureThumbUrls(list: any[]) {
  const imageItems = (list || []).filter((x) => x?.type === 'image')
  const targets = imageItems.filter((x) => {
    const id = String(x.id)
    if (thumbUrlMap.value[id]) return false
    if (isProbablyUrl(x.coverUrl)) return false
    return !!resolveObjectName(x)
  })

  await Promise.allSettled(
    targets.map(async (item) => {
      const objectName = resolveObjectName(item)
      const { data } = await api.get('/file/presigned', { params: { objectName, expireSeconds: 600 } })
      if (data.code === 0 && data.data) {
        thumbUrlMap.value[String(item.id)] = data.data
      }
    })
  )
}

async function loadMedia(page = 1) {
  loading.value = true
  try {
    const params: any = {
      page: page - 1,
      size: pageSize.value
    }
    
    if (filterType.value) {
      params.type = filterType.value
    }
    
    const { data } = await api.get('/content/media', { params })
    
    if (data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
      ensureThumbUrls(items.value)
    }
  } catch (error) {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function openPreview(item: any) {
  previewUrl.value = ''
  currentMedia.value = item
  try {
    if (item?.type === 'image' && isProbablyUrl(item.coverUrl)) {
      previewUrl.value = item.coverUrl
      previewVisible.value = true
      return
    }

    const objectName = resolveObjectName(item)
    if (!objectName) {
      ElMessage.error('缺少文件标识，无法预览')
      return
    }

    const { data } = await api.get('/file/presigned', { params: { objectName, expireSeconds: 600 } })
    if (data.code === 0 && data.data) {
      previewUrl.value = data.data
      previewVisible.value = true
    } else {
      ElMessage.error(data.message || '获取预览链接失败')
    }
  } catch (error) {
    ElMessage.error('获取预览链接失败')
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadMedia(1)
})
</script>

<style scoped>
.media-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 42px;
  font-weight: bold;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  color: #2563eb;
  -webkit-text-fill-color: initial;
}

.tab-icon {
  margin-right: 6px;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
}

.filter-card {
  margin-bottom: 24px;
  text-align: center;
  border-radius: 12px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
  min-height: 300px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.media-card {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.media-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.media-thumbnail {
  height: 200px;
  position: relative;
  overflow: hidden;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #dbeafe 0%, #e0f2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-video {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-icon {
  font-size: 64px;
  color: rgba(255, 255, 255, 0.8);
}

.media-type-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  display: inline-flex;
  align-items: center;
}

.badge-icon {
  margin-right: 4px;
}

.image-badge {
  background: rgba(67, 160, 71, 0.9);
}

.video-badge {
  background: rgba(229, 57, 53, 0.9);
}

.media-info {
  padding: 16px;
}

.media-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-summary {
  font-size: 13px;
  color: #666;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.media-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.media-date {
  font-size: 12px;
  color: #999;
  display: inline-flex;
  align-items: center;
}

.date-icon {
  margin-right: 4px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.preview-container {
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-video {
  width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-description {
  margin-top: 20px;
  color: #666;
  font-size: 14px;
  line-height: 1.8;
  text-align: left;
}
</style>
