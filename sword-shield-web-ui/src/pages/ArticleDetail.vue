<template>
  <div class="article-page">
    <div class="article-container">
      
      <!-- Navigation / Breadcrumb -->
      <div class="nav-header">
        <el-button link :icon="ArrowLeft" @click="goBack" class="back-link">
          返回列表
        </el-button>
      </div>

      <el-card v-if="item" class="article-card" shadow="never">
        
        <!-- Article Header -->
        <header class="article-header">
          <div class="article-meta-top">
             <el-tag :type="getCategoryType(item.category)" effect="light" round size="small">
               {{ item.category || '通用' }}
             </el-tag>
             <span class="publish-date">{{ formatDate(item.publishedAt) }}</span>
          </div>
          <h1 class="article-title">{{ item.title }}</h1>
        </header>

        <!-- Summary / Lead -->
        <div class="article-summary" v-if="item.summary">
          <p>{{ item.summary }}</p>
        </div>

        <el-divider border-style="dashed" />

        <!-- Main Body -->
        <div class="article-body">
          <div class="typo-content" v-html="item.body"></div>
        </div>

        <!-- Action Bar (Like/Share) -->
        <div class="article-actions">
           <el-button-group>
            <el-button 
              type="primary" 
              plain 
              round 
              size="large"
              :icon="likeCount > 0 ? StarFilled : Star"
              @click="like" 
              :loading="liking"
            >
              {{ likeCount > 0 ? `赞同 ${likeCount}` : '点赞' }}
            </el-button>
            <el-button 
              round 
              size="large"
              :icon="Share"
              @click="share" 
              :loading="sharing"
            >
              分享
            </el-button>
          </el-button-group>
        </div>
      </el-card>

      <!-- Media Gallery Section -->
      <div v-if="relatedMedia.length > 0" class="section-container media-section">
        <h3 class="section-heading">
          <span class="icon-wrapper"><el-icon><VideoCamera /></el-icon></span>
          精彩花絮
        </h3>
        <div class="gallery-grid">
          <div 
            v-for="media in relatedMedia" 
            :key="media.id" 
            class="media-card"
            @click="previewMedia(media)"
          >
            <div class="media-cover">
              <img 
                v-if="media.type === 'image' && media.thumbnailUrl" 
                :src="media.thumbnailUrl" 
                class="cover-img"
                :alt="media.title"
              />
              <img 
                v-else-if="media.type === 'video' && media.coverUrl" 
                :src="media.coverUrl" 
                class="cover-img"
                :alt="media.title"
              />
              <div class="placeholder-icon" v-else>
                 <el-icon v-if="media.type === 'image'"><Picture /></el-icon>
                 <el-icon v-else><VideoPlay /></el-icon>
              </div>
              
              <div class="media-badge" :class="media.type">
                {{ media.type === 'image' ? '图片' : '视频' }}
              </div>
              
              <div class="play-overlay" v-if="media.type === 'video'">
                 <el-icon><VideoPlay /></el-icon>
              </div>
            </div>
            <div class="media-info">
              <h4 class="media-title" :title="media.title">{{ media.title }}</h4>
            </div>
          </div>
        </div>
      </div>

      <!-- Comments Section -->
      <div class="section-container comments-section">
        <div class="section-title">
          <h3>评论 {{ comments.length > 0 ? `(${comments.length})` : '' }}</h3>
        </div>

        <div class="comment-input-area">
          <el-input 
            ref="commentInputRef"
            v-model="comment" 
            type="textarea" 
            :rows="3"
            placeholder="写下你的想法..."
            maxlength="500"
            show-word-limit
            resize="none"
          />
          <div class="input-actions">
            <el-button 
              type="primary" 
              @click="postComment" 
              :loading="commenting"
              :disabled="!comment.trim()"
            >
              发表评论
            </el-button>
          </div>
        </div>

        <div class="comments-list">
          <div v-if="comments.length === 0" class="empty-state">
            <el-empty description="暂无评论，来坐沙发！" :image-size="80" />
          </div>
          
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-avatar">
              {{ 'U' }}
            </div>
            <div class="comment-main">
              <div class="comment-header">
                <span class="user-name">用户_{{ c.userId?.substring(0, 6) || '匿名' }}</span>
                <span class="time-ago">{{ formatDate(c.createdAt) }}</span>
              </div>
              <div class="comment-body">
                {{ c.content }}
              </div>
              <div class="comment-footer">
                <el-button 
                  link 
                  size="small" 
                  @click="likeComment(c)" 
                  :loading="c._liking"
                  :type="c.isLiked ? 'primary' : 'default'"
                >
                  <el-icon style="margin-right: 4px"><component :is="c.isLiked ? StarFilled : Star" /></el-icon>
                   {{ c.likeCount > 0 ? c.likeCount : '赞' }}
                </el-button>
                <el-button link size="small">回复</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Media Preview Dialog -->
    <el-dialog 
      v-model="mediaPreviewVisible" 
      :title="currentMedia?.title"
      width="800px"
      align-center
      destroy-on-close
      class="media-preview-dialog"
    >
      <div class="preview-container">
        <img 
          v-if="currentMedia?.type === 'image'" 
          :src="mediaPreviewUrl" 
          class="preview-img"
          alt="预览"
        />
        <video 
          v-else 
          :src="mediaPreviewUrl" 
          controls 
          autoplay
          class="preview-video"
        ></video>
        <div v-if="currentMedia?.summary" class="preview-desc">
          {{ currentMedia.summary }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 资讯/文章详情页。
 *
 * - 展示文章正文与元信息（分类、发布时间等）。
 * - 支持评论/点赞/分享互动，并展示关联媒体资源（图片/视频）与下载/预览能力。
 * - 支持生成“分享海报”（后端异步任务：创建任务 → 轮询状态 → 打开结果 URL）。
 *
 * 依赖接口（部分）：
 * - GET `/content/article`
 * - GET `/content/media/by-article`、GET `/content/media`
 * - GET `/file/presigned`（下载/预览）
 * - GET `/interaction/comments`、POST `/interaction/comment`
 * - POST `/interaction/like`、GET `/interaction/likes/count`、POST `/interaction/share`
 * - POST `/content/poster`、GET `/content/poster/jobs/:jobId`
 */
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Picture, VideoPlay, VideoCamera, Star, StarFilled, Share } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const item = ref<any>(null)
const comments = ref<any[]>([])
const comment = ref('')
const likeCount = ref(0)
const commenting = ref(false)
const liking = ref(false)
const sharing = ref(false)
const commentInputRef = ref<any>(null)


const relatedMedia = ref<any[]>([])
const mediaPreviewVisible = ref(false)
const mediaPreviewUrl = ref('')
const currentMedia = ref<any>(null)

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
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

function openLink(url: string) {
  if (url) {
    window.open(url, '_blank')
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  const fallback = item.value?.category === 'UGC' ? '/submissions' : '/articles'
  router.push(fallback)
}

async function focusCommentBox() {
  await nextTick()
  commentInputRef.value?.focus?.()
}


async function load() {
  const { data } = await api.get('/content/article', { params: { id: route.params.id } })
  if (data.code === 0) item.value = data.data
  
  // Load initial like count
  const likeCountRes = await api.get('/interaction/likes/count', { 
    params: { 
      targetType: 'article', 
      targetId: Number(route.params.id) 
    } 
  })
  if (likeCountRes.data.code === 0) {
    likeCount.value = likeCountRes.data.data.count
  }
  
  // Load related media
  await loadRelatedMedia()
}

async function loadRelatedMedia() {
  try {
    const { data } = await api.get('/content/media/by-article', {
      params: { articleId: Number(route.params.id) }
    })
    if (data.code === 0) {
      relatedMedia.value = data.data
      
      // Load thumbnail URLs for all media
      await Promise.all(relatedMedia.value.map(async (media: any) => {
        try {
          const { data: urlData } = await api.get('/file/presigned', {
            params: {
              objectName: media.objectName,
              expireSeconds: 3600
            }
          })
          if (urlData.code === 0) {
            media.thumbnailUrl = urlData.data
          }
        } catch (error) {
          console.error(`Failed to load thumbnail for ${media.id}`, error)
        }
      }))
    }
  } catch (error) {
    console.error('Failed to load related media', error)
  }
}

async function previewMedia(media: any) {
  try {
    const { data } = await api.get('/file/presigned', {
      params: {
        objectName: media.objectName,
        expireSeconds: 600
      }
    })
    
    if (data.code === 0) {
      mediaPreviewUrl.value = data.data
      currentMedia.value = media
      mediaPreviewVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取预览链接失败')
  }
}

async function loadComments() {
  const { data } = await api.get('/interaction/comments', { 
    params: { 
      targetType: 'article', 
      targetId: Number(route.params.id), 
      page: 0, 
      size: 10 
    } 
  })
  if (data.code === 0) {
    comments.value = data.data.items
    await Promise.all(comments.value.map(async (c: any) => {
      const { data: cnt } = await api.get('/interaction/likes/count', { params: { targetType: 'comment', targetId: c.id } })
      c.likeCount = cnt?.data?.count ?? 0
      c._liking = false
    }))
  }
}

async function postComment() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!comment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  commenting.value = true
  try {
    const { data } = await api.post('/interaction/comment', { 
      targetType: 'article', 
      targetId: Number(route.params.id), 
      content: comment.value 
    })
    if (data.code === 0) {
      comment.value = ''
      ElMessage.success('评论成功')
      await loadComments()
    } else {
      ElMessage.error(data.message || '评论失败')
    }
  } catch (error) {
    ElMessage.error('评论失败，请稍后重试')
  } finally {
    commenting.value = false
  }
}

async function like() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // Prevent multiple simultaneous clicks
  if (liking.value) return
  
  liking.value = true
  try {
    const { data } = await api.post('/interaction/like', { 
      targetType: 'article', 
      targetId: Number(route.params.id) 
    })
    if (data.code === 0) {
      likeCount.value = data.data.count
      ElMessage.success('点赞成功')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    liking.value = false
  }
}

async function share() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const previewWindow = window.open('', '_blank')
  sharing.value = true
  try {
    if (previewWindow) {
      previewWindow.document.title = '海报生成中...'
      previewWindow.document.body.innerHTML = '<div style="font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; padding: 28px; color: #0f172a;"><h2 style="margin: 0 0 10px;">海报生成中...</h2><p style="margin: 0; color: #475569;">请稍候，生成完成后会自动跳转。</p></div>'
    }

    const url = window.location.href
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(url)
    }
    const { data } = await api.post('/interaction/share', {
      targetType: 'article',
      targetId: Number(route.params.id),
      channel: 'link'
    })
    if (data.code === 0) {
      ElMessage.success('已复制链接，分享成功')
    } else {
      ElMessage.error(data.message || '分享失败')
    }
    // Generate poster
    const nickname = localStorage.getItem('username') || '参会者'
    const params = new URLSearchParams()
    params.set('title', item.value?.title || '西湖论剑·资讯')
    params.set('subtitle', `来自 ${nickname} 的分享`)
    ElMessage.info('海报生成中，请稍候...')
    const posterJob = await api.post('/content/poster', params, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    if (posterJob.data.code !== 0) {
      throw new Error(posterJob.data.message || '海报任务提交失败')
    }
    const jobId = posterJob.data?.data?.jobId
    if (!jobId) {
      throw new Error('海报任务返回 jobId 为空')
    }

    const start = Date.now()
    let posterUrl = ''
    while (Date.now() - start < 30_000) {
      const { data: statusRes } = await api.get(`/content/poster/jobs/${jobId}`)
      if (statusRes.code !== 0) {
        throw new Error(statusRes.message || '查询海报任务失败')
      }
      const status = statusRes?.data?.status
      if (status === 'SUCCESS') {
        posterUrl = statusRes?.data?.url || ''
        break
      }
      if (status === 'FAILED') {
        throw new Error(statusRes?.data?.errorMessage || '海报生成失败')
      }
      await new Promise((r) => setTimeout(r, 1000))
    }
    if (!posterUrl) {
      throw new Error('海报生成超时，请稍后重试')
    }

    if (previewWindow) {
      previewWindow.location.href = posterUrl
    } else {
      window.open(posterUrl, '_blank')
    }
  } catch (error: any) {
    if (previewWindow) {
      previewWindow.document.title = '海报生成失败'
      previewWindow.document.body.innerHTML = '<div style="font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; padding: 28px; color: #0f172a;"><h2 style="margin: 0 0 10px;">海报生成失败</h2><p style="margin: 0; color: #475569;">请返回页面重试。</p></div>'
    }
    ElMessage.error(error?.message || '分享失败，请稍后重试')
  } finally {
    sharing.value = false
  }
}

async function likeComment(c: any) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // Prevent multiple simultaneous clicks
  if (c._liking) return
  
  c._liking = true
  try {
    const { data } = await api.post('/interaction/like', { targetType: 'comment', targetId: c.id })
    if (data.code === 0) {
      c.likeCount = data.data.count
      ElMessage.success('点赞成功')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    c._liking = false
  }
}

onMounted(async () => {
  await load()
  await loadComments()
})
</script>

<style scoped>
.article-page {
  min-height: 100vh;
  background-color: #f3f4f6;
  padding: 24px 16px;
  display: flex;
  justify-content: center;
}

.article-container {
  width: 100%;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.nav-header {
  margin-bottom: -8px;
}
.back-link {
  font-size: 14px;
  color: #606266;
}
.back-link:hover {
  color: #409eff;
}

/* Article Card */
.article-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  overflow: visible; /* To allow sticky elements if needed */
}

/* Article Header */
.article-header {
  text-align: center;
  margin-bottom: 24px;
  padding-top: 10px;
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.4;
  margin: 16px 0;
}

.article-meta-top {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #6b7280;
}

/* Summary */
.article-summary {
  background-color: #f8fafc;
  border-left: 4px solid #409eff;
  padding: 16px 20px;
  border-radius: 4px;
  margin-bottom: 30px;
  font-size: 16px;
  color: #475569;
  line-height: 1.6;
  font-style: italic;
}

/* Body Content */
.article-body {
  margin-bottom: 40px;
}
.typo-content {
  color: #334155;
  font-size: 17px;
  line-height: 1.8;
  word-wrap: break-word;
}
/* Deep selector for v-html content styles */
.typo-content :deep(p) {
  margin-bottom: 1.5em;
}
.typo-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
  margin: 20px auto;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
}
.typo-content :deep(h2), .typo-content :deep(h3) {
  color: #1e293b;
  font-weight: 600;
  margin-top: 2em;
  margin-bottom: 1em;
}

/* Actions */
.article-actions {
  display: flex;
  justify-content: center;
  margin: 40px 0 10px;
}

/* Section Common Styles */
.section-container {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.section-heading, .section-title h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.icon-wrapper {
  color: #409eff;
  display: flex;
  align-items: center;
}

/* Gallery Grid */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.media-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  background: #fff;
  border: 1px solid #f3f4f6;
}
.media-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.media-cover {
  position: relative;
  aspect-ratio: 16/9;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder-icon {
  font-size: 40px;
  color: #cbd5e1;
}

.media-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  text-transform: uppercase;
}
.media-badge.image { background: rgba(59, 130, 246, 0.9); }
.media-badge.video { background: rgba(239, 68, 68, 0.9); }

.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.play-overlay .el-icon {
  font-size: 32px;
  color: white;
}
.media-card:hover .play-overlay {
  opacity: 1;
}

.media-info {
  padding: 12px;
}
.media-title {
  margin: 0;
  font-size: 14px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Comments */
.comment-input-area {
  margin-bottom: 30px;
}
.input-actions {
  margin-top: 12px;
  text-align: right;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 16px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  background: #f9fafb;
  padding: 16px;
  border-radius: 12px;
  border-bottom-left-radius: 2px; /* Chat bubble style */
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}
.user-name {
  font-weight: 600;
  color: #374151;
}
.time-ago {
  color: #9ca3af;
}

.comment-body {
  font-size: 14px;
  color: #1f2937;
  line-height: 1.6;
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.comment-footer {
  display: flex;
  gap: 12px;
}

/* Responsive */
@media (max-width: 640px) {
  .article-title {
    font-size: 22px;
  }
  .article-card, .section-container {
    padding: 16px;
    border-radius: 0;
  }
  .article-page {
    padding: 0;
  }
  .gallery-grid {
    grid-template-columns: 1fr;
  }
}
</style>
