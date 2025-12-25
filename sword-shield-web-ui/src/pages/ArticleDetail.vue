<template>
  <div class="detail-container">
    <el-card v-if="item" class="detail-card">
      <div class="detail-header">
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
          <el-card v-if="item.liveUrl" class="media-card live-card" shadow="hover">
            <div class="media-icon live-dot" />
            <div class="media-info">
              <h4>直播链接</h4>
              <p>点击进入直播间</p>
            </div>
            <el-button type="danger" @click="enterLive(item.liveUrl)" round>
              进入直播 →
            </el-button>
          </el-card>
          
          <el-card v-if="item.replayUrl" class="media-card replay-card" shadow="hover">
            <div class="media-icon replay-icon" />
            <div class="media-info">
              <h4>回放链接</h4>
              <p>观看录播回放</p>
            </div>
            <el-button type="primary" @click="enterReplay(item.replayUrl)" round>
              观看回放 →
            </el-button>
          </el-card>
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
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, VideoPlay } from '@element-plus/icons-vue'
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
  sharing.value = true
  try {
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
    const poster = await api.post('/content/poster', params, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    const posterUrl = poster.data?.data
    if (posterUrl) {
      window.open(posterUrl, '_blank')
    }
  } catch (error) {
    ElMessage.error('分享失败，请稍后重试')
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
  margin: 24px 0 8px;
}

.section-heading {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 18px;
}

.media-links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.media-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 18px 16px;
  border-radius: 18px;
  border: 1px solid rgba(209, 213, 219, 0.9);
  background: #f9fafb;
  transition: all 0.22s ease;
  cursor: pointer;
}

.media-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px rgba(148, 163, 184, 0.5);
  border-color: rgba(59, 130, 246, 0.95);
  background: #eff6ff;
}

.live-card {
  border-left: 4px solid #22c55e;
}

.replay-card {
  border-left: 4px solid #3b82f6;
}

.media-icon {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-icon.live-dot {
  background: radial-gradient(circle at 25% 0, #bbf7d0, #16a34a 55%, #14532d 100%);
}

.media-icon.replay-icon {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
}

.media-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #111827;
}

.media-info p {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
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
