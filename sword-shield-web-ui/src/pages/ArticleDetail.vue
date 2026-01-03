<template>
  <div class="detail-container">
    <el-card v-if="item" class="detail-card">
      <div class="detail-header">
        <div class="detail-back">
          <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
        </div>
        <h1 class="detail-title">{{ item.title }}</h1>
        <div class="detail-meta">
          <el-tag :type="getCategoryType(item.category)" size="large">{{ item.category || '通用' }}</el-tag>
          <span class="publish-date">{{ formatDate(item.publishedAt) }}</span>
        </div>
      </div>

      <el-divider />

      <div class="detail-content">
        <p class="summary">{{ item.summary }}</p>
        <div class="body-content" v-html="item.body" />
      </div>

      <!-- Live & Replay Links Section -->
      <div v-if="item.liveUrl || item.replayUrl" class="media-links-section">
        <el-divider />
        <h3 class="section-heading">直播与回放</h3>
        <div class="media-links">
          <div v-if="item.liveUrl" class="media-card live-card" @click="enterLive(item.liveUrl)">
            <div class="media-card-bg"></div>
            <div class="media-card-content">
              <div class="media-icon-wrapper live-icon-wrapper">
                <el-icon class="media-icon"><VideoCameraFilled /></el-icon>
              </div>
              <div class="media-info">
                <h4>直播中</h4>
                <p>点击进入直播间观看精彩内容</p>
              </div>
              <div class="media-action">
                <span class="action-text">进入直播</span>
                <el-icon class="action-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
            <div class="live-indicator">
              <span class="live-dot"></span>
              <span>LIVE</span>
            </div>
          </div>
          
          <div v-if="item.replayUrl" class="media-card replay-card" @click="enterReplay(item.replayUrl)">
            <div class="media-card-bg"></div>
            <div class="media-card-content">
              <div class="media-icon-wrapper replay-icon-wrapper">
                <el-icon class="media-icon"><VideoPlay /></el-icon>
              </div>
              <div class="media-info">
                <h4>视频回放</h4>
                <p>观看往期精彩录播内容</p>
              </div>
              <div class="media-action">
                <span class="action-text">观看回放</span>
                <el-icon class="action-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Media Gallery Section -->
      <div v-if="relatedMedia.length > 0" class="media-gallery-section">
        <el-divider />
        <h3 class="section-heading">精彩花絮</h3>
        <div class="gallery-grid">
          <el-card 
            v-for="media in relatedMedia" 
            :key="media.id" 
            class="gallery-card"
            shadow="hover"
            @click="previewMedia(media)"
          >
            <div class="gallery-thumbnail">
              <div v-if="media.type === 'image'" class="thumbnail-image">
                <img 
                  v-if="media.thumbnailUrl" 
                  :src="media.thumbnailUrl" 
                  class="thumbnail-img"
                  :alt="media.title"
                />
                <el-icon v-else class="gallery-icon"><Picture /></el-icon>
              </div>
              <div v-else class="thumbnail-video">
                <img 
                  v-if="media.coverUrl" 
                  :src="media.coverUrl" 
                  class="thumbnail-img"
                  :alt="media.title"
                />
                <el-icon v-else class="gallery-icon"><VideoPlay /></el-icon>
                <div class="play-overlay">
                  <el-icon class="play-icon"><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="media-type-badge" :class="media.type === 'image' ? 'image-badge' : 'video-badge'">
                {{ media.type === 'image' ? '图片' : '视频' }}
              </div>
            </div>
            <div class="gallery-info">
              <h4 class="gallery-title">{{ media.title }}</h4>
              <p class="gallery-summary" v-if="media.summary">{{ media.summary }}</p>
            </div>
          </el-card>
        </div>
      </div>

      <el-divider />

      <div class="interaction-section">
        <div class="section-title">
          <h3>评论区</h3>
          <div>
            <el-button size="small" @click="like" :loading="liking" style="margin-right: 8px;">
              <span>赞同 {{ likeCount }}</span>
            </el-button>
            <el-button size="small" @click="share" :loading="sharing">
              <span>分享</span>
            </el-button>
          </div>
        </div>

        <div class="comment-form">
          <el-input 
            ref="commentInputRef"
            v-model="comment" 
            type="textarea" 
            :rows="3"
            placeholder="写下你的想法..."
            maxlength="500"
            show-word-limit
          />
          <el-button 
            type="primary" 
            @click="postComment" 
            :loading="commenting"
            style="margin-top: 12px;"
          >
            发表评论
          </el-button>
        </div>

        <div class="comments-list">
          <div v-if="comments.length === 0" class="empty-comments">
            <el-empty description="还没有评论，快来发表第一条吧！" :image-size="96">
              <el-button type="primary" @click="focusCommentBox">去评论</el-button>
            </el-empty>
          </div>
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-avatar-mark">U</div>
            <div class="comment-content">
              <div class="comment-text">{{ c.content }}</div>
              <div class="comment-actions">
                <el-button size="small" @click="likeComment(c)" :loading="c._liking">
                  赞 {{ c.likeCount ?? 0 }}
                </el-button>
              </div>
              <div class="comment-time">{{ formatDate(c.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Media Preview Dialog -->
    <el-dialog 
      v-model="mediaPreviewVisible" 
      :title="currentMedia?.title"
      width="80%"
      center
      destroy-on-close
    >
      <div class="media-preview-content">
        <img 
          v-if="currentMedia?.type === 'image'" 
          :src="mediaPreviewUrl" 
          class="preview-media-image"
          alt="预览图片"
        />
        <video 
          v-else 
          :src="mediaPreviewUrl" 
          controls 
          autoplay
          class="preview-media-video"
        ></video>
        <p v-if="currentMedia?.summary" class="preview-media-description">
          {{ currentMedia.summary }}
        </p>
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
import { ArrowLeft, Picture, VideoPlay } from '@element-plus/icons-vue'
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
  return date.toLocaleString('zh-CN')
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

function enterLive(url: string) {
  if (!url) return
  router.push(`/article/${route.params.id}/live?mode=live`)
}

function enterReplay(url: string) {
  if (!url) return
  router.push(`/article/${route.params.id}/live?mode=replay`)
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
.detail-container {
  max-width: 1240px;
  margin: 24px auto 40px;
}

.detail-card {
  border-radius: 24px;
  padding: 24px 24px 20px;
  background: #ffffff;
  border: 1px solid rgba(191, 219, 254, 0.9);
  box-shadow: 0 16px 40px rgba(148, 163, 184, 0.38);
}

.detail-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(229, 231, 235, 0.9);
}

.detail-back {
  margin-bottom: 6px;
}

.detail-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 12px 0;
  line-height: 1.35;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.publish-date {
  font-size: 13px;
  color: #6b7280;
}

.detail-content {
  margin: 20px 0 16px;
}

.summary {
  font-size: 15px;
  line-height: 1.7;
  color: #1f2937;
  font-weight: 500;
  padding: 12px 16px;
  background: linear-gradient(120deg, #eff6ff 0%, #eef2ff 100%);
  border-radius: 16px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  box-shadow: 0 10px 28px rgba(129, 140, 248, 0.22);
  margin-bottom: 24px;
}

.body-content {
  font-size: 15px;
  line-height: 1.8;
  color: #111827;
}

.body-content :deep(h1),
.body-content :deep(h2),
.body-content :deep(h3) {
  margin-top: 22px;
  margin-bottom: 12px;
  color: #0f172a;
}

.body-content :deep(p) {
  margin-bottom: 14px;
}

.body-content :deep(img) {
  max-width: 100%;
  border-radius: 12px;
  margin: 18px 0;
  box-shadow: 0 10px 26px rgba(148, 163, 184, 0.4);
}

/* Live & Replay section */
.media-links-section {
  margin: 32px 0;
}

.section-heading {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 20px;
}

.media-links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 24px;
}

.media-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.media-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.2);
}

.media-card-bg {
  position: absolute;
  inset: 0;
  transition: opacity 0.4s ease;
}

.live-card .media-card-bg {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 50%, #60a5fa 100%);
}

.replay-card .media-card-bg {
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #1e40af 100%);
}

.media-card:hover .media-card-bg {
  opacity: 0.95;
}

.media-card-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 24px;
}

.media-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.live-icon-wrapper {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
}

.replay-icon-wrapper {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
}

.media-icon {
  font-size: 32px;
  color: #ffffff;
}

.media-info {
  flex: 1;
  min-width: 0;
}

.media-info h4 {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 6px 0;
}

.media-info p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.media-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  border-radius: 24px;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.media-card:hover .media-action {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(4px);
}

.action-text {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  white-space: nowrap;
}

.action-arrow {
  font-size: 16px;
  color: #ffffff;
  transition: transform 0.3s ease;
}

.media-card:hover .action-arrow {
  transform: translateX(4px);
}

.live-indicator {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(239, 68, 68, 0.9);
  border-radius: 16px;
  font-size: 11px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 1px;
}

.live-dot {
  width: 6px;
  height: 6px;
  background: #ffffff;
  border-radius: 50%;
  animation: livePulse 1.5s ease-in-out infinite;
}

@keyframes livePulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.8); }
}

/* Media Gallery */
.media-gallery-section {
  margin: 28px 0 12px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.gallery-card {
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(209, 213, 219, 0.9);
  box-shadow: 0 8px 22px rgba(148, 163, 184, 0.4);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.gallery-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(148, 163, 184, 0.6);
  border-color: rgba(59, 130, 246, 0.95);
}

.gallery-thumbnail {
  height: 170px;
  position: relative;
  overflow: hidden;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e0f2fe 0%, #e0e7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
}

.thumbnail-video {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.gallery-icon {
  font-size: 40px;
  color: rgba(255, 255, 255, 0.8);
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 52px;
  height: 52px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
}

.gallery-card:hover .play-overlay {
  background: #ffffff;
  transform: translate(-50%, -50%) scale(1.06);
}

.play-icon {
  font-size: 22px;
  color: #2563eb;
}

.media-type-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #ffffff;
}

.image-badge {
  background: rgba(34, 197, 94, 0.9);
}

.video-badge {
  background: rgba(59, 130, 246, 0.9);
}

.gallery-info {
  padding: 10px 12px 12px;
}

.gallery-title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gallery-summary {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
  line-height: 1.4;
}

/* Media preview */
.media-preview-content {
  text-align: center;
}

.preview-media-image {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 12px;
}

.preview-media-video {
  width: 100%;
  max-height: 70vh;
  border-radius: 12px;
}

.preview-media-description {
  margin-top: 18px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.7;
  text-align: left;
}

/* Interaction / comments */
.interaction-section {
  margin-top: 20px;
  padding-top: 12px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title h3 {
  font-size: 18px;
  color: #0f172a;
  margin: 0;
}

.comment-form {
  margin-bottom: 24px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.empty-comments {
  text-align: center;
  padding: 18px 10px 8px;
}

.comment-item {
  display: flex;
  padding: 16px 14px;
  background: #f9fafb;
  border-radius: 16px;
  border: 1px solid rgba(209, 213, 219, 0.9);
}

.comment-avatar-mark {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: #e0ecff;
  color: #1d4ed8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  margin-right: 10px;
}

.comment-content {
  flex: 1;
}

.comment-text {
  font-size: 14px;
  color: #111827;
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-actions {
  margin-bottom: 6px;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

@media (max-width: 768px) {
  .detail-card {
    border-radius: 18px;
    padding: 18px 14px 16px;
  }

  .detail-title {
    font-size: 22px;
  }

  .media-links {
    grid-template-columns: minmax(0, 1fr);
  }

  .gallery-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
