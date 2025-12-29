<template>
  <div class="detail-container">
    <el-card v-if="item" class="detail-card">
      <div class="detail-header">
        <div class="cover-section" v-if="item.coverUrl">
          <img :src="item.coverUrl" :alt="item.title" />
        </div>
        <h1 class="detail-title">{{ item.title }}</h1>
        <div class="detail-meta">
          <div class="meta-tag">
            <el-icon class="icon"><Location /></el-icon>
            <span>{{ item.location || '地点待定' }}</span>
          </div>
          <div class="meta-tag">
            <el-icon class="icon"><Calendar /></el-icon>
            <span>开始 {{ formatDate(item.startTime) || '待定' }}</span>
          </div>
          <div class="meta-tag">
            <el-icon class="icon"><Clock /></el-icon>
            <span>结束 {{ formatDate(item.endTime) || '待定' }}</span>
          </div>
          <div class="meta-tag" v-if="item.maxRegistrations">
            <el-icon class="icon"><UserFilled /></el-icon>
            <span>限额 {{ item.maxRegistrations }} 人</span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="detail-content">
        <p class="summary">{{ item.summary }}</p>
        <div class="action-section">
          <el-button type="primary" size="large" @click="register" :loading="registering" :disabled="!canRegister || registered">
            <span v-if="registered">
              <el-icon class="btn-icon"><CircleCheck /></el-icon>
              已报名
            </span>
            <span v-else>
              <el-icon class="btn-icon"><Tickets /></el-icon>
              立即报名
            </span>
          </el-button>
          <el-button
            type="success"
            size="large"
            @click="activityCheckin"
            :loading="checkingIn"
            :disabled="!canCheckin || checkedIn"
            style="margin-left: 12px;"
          >
            <span v-if="checkedIn">
              <el-icon class="btn-icon"><CircleCheck /></el-icon>
              已签到
            </span>
            <span v-else>
              <el-icon class="btn-icon"><DocumentChecked /></el-icon>
              活动签到
            </span>
          </el-button>
          <el-button size="large" @click="share" :loading="sharing" style="margin-left: 12px;">
            <span>
              <el-icon class="btn-icon"><Share /></el-icon>
              分享活动
            </span>
          </el-button>
          <div v-if="registerHint || checkinHint" class="checkin-hint">
            <div v-if="registerHint">{{ registerHint }}</div>
            <div v-if="checkinHint">{{ checkinHint }}</div>
          </div>
        </div>
      </div>

      <!-- Live & Replay Links Section -->
      <div v-if="item.liveUrl || item.replayUrl" class="media-links-section">
        <el-divider />
        <h3 class="section-heading">
          <el-icon class="heading-icon"><VideoPlay /></el-icon>
          直播与回放
        </h3>
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

      <el-divider />

      <div class="interaction-section">
        <div class="section-title">
          <h3>
            <el-icon class="heading-icon"><ChatDotRound /></el-icon>
            评论区
          </h3>
          <el-button size="small" @click="like" :loading="liking">
            <span>
              <el-icon class="inline-icon"><StarFilled /></el-icon>
              {{ likeCount }}
            </span>
          </el-button>
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
            <div class="comment-avatar">
              <el-icon><Avatar /></el-icon>
            </div>
            <div class="comment-content">
              <div class="comment-text">{{ c.content }}</div>
              <div class="comment-actions">
                <el-button size="small" @click="likeComment(c)" :loading="c._liking">
                  <el-icon class="inline-icon"><StarFilled /></el-icon>
                  {{ c.likeCount ?? 0 }}
                </el-button>
              </div>
              <div class="comment-time">{{ formatDate(c.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const item = ref<any>(null)
const comments = ref<any[]>([])
const comment = ref('')
const likeCount = ref(0)
const registering = ref(false)
const commenting = ref(false)
const liking = ref(false)
const sharing = ref(false)
const commentInputRef = ref<any>(null)
const checkingIn = ref(false)
const checkedIn = ref(false)
const canCheckin = ref(false)
const checkinHint = ref('')
const canRegister = ref(true)
const registerHint = ref('')
const registered = ref(false)

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

async function focusCommentBox() {
  await nextTick()
  commentInputRef.value?.focus?.()
}

function openLink(url: string) {
  if (url) {
    window.open(url, '_blank')
  }
}

function enterLive(url: string) {
  if (!url) return
  router.push(`/activity/${route.params.id}/live?mode=live`)
}

function enterReplay(url: string) {
  if (!url) return
  router.push(`/activity/${route.params.id}/live?mode=replay`)
}

async function load() {
  const { data } = await api.get('/activity/detail', { params: { id: route.params.id } })
  if (data.code === 0) {
    item.value = data.data
    computeRegisterWindow()
    computeCheckinWindow()
    await loadRegistrationStatus()
    await loadCheckinStatus()
  }
  
  // Load initial like count
  const likeCountRes = await api.get('/interaction/likes/count', { 
    params: { 
      targetType: 'activity', 
      targetId: Number(route.params.id) 
    } 
  })
  if (likeCountRes.data.code === 0) {
    likeCount.value = likeCountRes.data.data.count
  }
}

function computeCheckinWindow() {
  checkinHint.value = ''
  canCheckin.value = false
  if (!item.value) return
  const now = Date.now()
  const startMs = item.value.startTime ? new Date(item.value.startTime).getTime() : null
  const endMs = item.value.endTime ? new Date(item.value.endTime).getTime() : null

  if (startMs != null && now < startMs) {
    checkinHint.value = '活动未开始，暂不可签到'
    return
  }
  if (endMs != null && now > endMs) {
    checkinHint.value = '活动已结束，不可签到'
    return
  }
  canCheckin.value = true
}

function computeRegisterWindow() {
  registerHint.value = ''
  canRegister.value = false
  if (!item.value) return
  const now = Date.now()
  const startMs = item.value.startTime ? new Date(item.value.startTime).getTime() : null
  const endMs = item.value.endTime ? new Date(item.value.endTime).getTime() : null

  if (endMs != null && now > endMs) {
    registerHint.value = '活动已结束，报名已截止'
    return
  }
  if (startMs != null && now >= startMs) {
    registerHint.value = '活动已开始，报名已截止'
    return
  }
  canRegister.value = true
}

async function loadCheckinStatus() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const { data } = await api.get('/user/activity-checkin/status', {
      params: { activityId: Number(route.params.id) }
    })
    if (data.code === 0) {
      checkedIn.value = !!data.data.checkedIn
    }
  } catch {
    // ignore
  }
}

async function loadRegistrationStatus() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const { data } = await api.get('/activity/registration/status', {
      params: { activityId: Number(route.params.id) }
    })
    if (data.code === 0) {
      registered.value = !!data.data.registered
    }
  } catch {
    // ignore
  }
}

async function register() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!canRegister.value) {
    ElMessage.warning(registerHint.value || '当前不可报名')
    return
  }
  if (registered.value) return
  
  registering.value = true
  try {
    const { data } = await api.post('/activity/register', { 
      activityId: Number(route.params.id), 
      channel: 'web' 
    })
    if (data.code === 0) {
      registered.value = true
      ElMessage.success('报名成功！')
    } else {
      if (typeof data.message === 'string' && data.message.includes('已报名')) {
        registered.value = true
      }
      ElMessage.error(data.message || '报名失败')
    }
  } catch (error) {
    ElMessage.error('报名失败，请稍后重试')
  } finally {
    registering.value = false
  }
}

async function activityCheckin() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!canCheckin.value) {
    ElMessage.warning(checkinHint.value || '当前不可签到')
    return
  }
  if (checkedIn.value) return

  checkingIn.value = true
  try {
    const { data } = await api.post('/user/activity-checkin', { activityId: Number(route.params.id) })
    if (data.code === 0) {
      checkedIn.value = true
      ElMessage.success('签到成功！')
    } else {
      ElMessage.error(data.message || '签到失败')
    }
  } catch (error) {
    ElMessage.error('签到失败，请稍后重试')
  } finally {
    checkingIn.value = false
  }
}

async function loadComments() {
  const { data } = await api.get('/interaction/comments', { 
    params: { 
      targetType: 'activity', 
      targetId: Number(route.params.id), 
      page: 0, 
      size: 10 
    } 
  })
  if (data.code === 0) {
    comments.value = data.data.items
    // fetch like counts per comment
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
      targetType: 'activity', 
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
      targetType: 'activity', 
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
    // 仅复制当前页面链接到剪贴板
    ElMessage.success('链接已复制')

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
  max-width: 800px;
  margin: 0 auto;
}

.detail-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.detail-header {
  margin-bottom: 24px;
}

.cover-section {
  width: 100%;
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
}

.cover-section img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.meta-tag .icon {
  font-size: 16px;
}

.detail-content {
  margin: 24px 0;
}

.summary {
  font-size: 16px;
  line-height: 1.8;
  color: #666;
  margin-bottom: 24px;
}

.action-section {
  text-align: center;
  padding: 24px 0;
}

.checkin-hint {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

/* Media links styling */
.media-links-section {
  margin: 32px 0;
}

.section-heading {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
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
}

.media-card:hover .media-action {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(4px);
}

.action-text {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
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
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 1px;
}

.live-dot {
  width: 8px;
  height: 8px;
  background: #ffffff;
  border-radius: 50%;
  animation: livePulse 1.5s ease-in-out infinite;
}

@keyframes livePulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.8); }
}

.interaction-section {
  margin-top: 24px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title h3 {
  font-size: 20px;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-form {
  margin-bottom: 32px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-comments {
  text-align: center;
  padding: 18px 10px 8px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 12px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin: 8px 0;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.heading-icon {
  color: #409eff;
}

.btn-icon {
  margin-right: 6px;
}

.inline-icon {
  margin-right: 6px;
}
</style>
