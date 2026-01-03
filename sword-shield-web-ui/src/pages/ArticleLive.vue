<template>
  <div class="live-page">
    <el-card v-if="loading" class="live-card" shadow="never">
      <div class="loading">加载中...</div>
    </el-card>

    <el-card v-else-if="error" class="live-card" shadow="never">
      <el-alert :title="error" type="error" show-icon />
      <div class="actions">
        <el-button @click="goBack">返回文章详情</el-button>
      </div>
    </el-card>

    <el-card v-else class="live-card" shadow="hover">
      <div class="header">
        <div class="title">{{ item?.title || '直播间' }}</div>
        <div class="status" :class="mode">
          <span class="dot" />
          <span v-if="mode === 'pre'">即将开始</span>
          <span v-else-if="mode === 'live'">直播中</span>
          <span v-else>回放</span>
        </div>
      </div>

      <div v-if="mode === 'pre'" class="pre">
        <div v-if="item?.coverUrl" class="cover">
          <img :src="item.coverUrl" alt="封面" />
        </div>
        <div class="countdown">{{ countdownText }}</div>
        <div class="hint">到点后将自动切换为“直播中”</div>
        <div class="actions">
          <el-button type="primary" @click="goBack">返回文章详情</el-button>
        </div>
      </div>

      <div v-else class="player">
        <div class="player-shell">
          <video
            v-if="renderKind === 'video'"
            ref="videoEl"
            class="video"
            :src="playUrl"
            :controls="mode === 'replay'"
            :muted="muted"
            autoplay
            :loop="mode === 'live'"
            playsinline
            preload="metadata"
            @loadedmetadata="onLoadedMetadata"
            @play="onPlay"
            @playing="onPlaying"
            @pause="onPause"
            @seeking="onSeeking"
            @ended="onEnded"
            @error="onVideoError"
          />

          <iframe
            v-else
            class="video iframe"
            :src="playUrl"
            title="直播/回放"
            allow="autoplay; fullscreen; picture-in-picture"
            allowfullscreen
            referrerpolicy="no-referrer"
          />

          <div v-if="mode === 'live'" class="live-overlay">
            <div class="live-badge">
              <span class="live-dot" />
              LIVE
            </div>
            <div class="live-time">{{ liveTimeText }}</div>
          </div>

          <div v-if="renderKind === 'video' && mode === 'live' && needsUserPlay" class="tap-to-play">
            <el-button type="danger" size="large" @click="forcePlay">
              点击开始播放
            </el-button>
            <div class="tap-hint">浏览器可能拦截了自动播放</div>
          </div>

          <div v-if="renderKind === 'video' && videoError" class="tap-to-play">
            <el-alert title="视频播放失败" type="error" show-icon :closable="false" />
            <div class="tap-hint">{{ videoError }}</div>
          </div>
        </div>

        <div class="controls-row">
          <el-button v-if="mode === 'live' && renderKind === 'video'" type="danger" @click="forceSync">
            同步到直播进度
          </el-button>
          <el-button v-if="renderKind === 'video'" @click="toggleMute">{{ muted ? '取消静音' : '静音' }}</el-button>
          <el-button v-if="playUrl" @click="openExternal">新窗口打开</el-button>
          <el-button @click="goBack">返回文章详情</el-button>
        </div>

        <div v-if="mode === 'live' && renderKind === 'video'" class="note">
          伪直播模式：进度会跟随当前时间，无法拖动进度条。
        </div>

        <div v-else-if="mode === 'live' && renderKind === 'iframe'" class="note">
          外部直播页已内嵌；如无画面/被浏览器拦截，请点“新窗口打开”。
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 资讯/文章直播/回放页（伪直播展示）。
 *
 * - 文章本身没有排期字段，这里以“进入页面时刻”作为伪直播计时基准。
 * - `mode` 来自路由 query：`?mode=pre|live|replay`，用于手动切换/调试。
 *
 * 依赖接口：
 * - GET `/content/article`：获取文章信息（含 `liveUrl` / `replayUrl`）。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

// 页面模式：pre=未开始（调试用），live=直播，replay=回放
type Mode = 'pre' | 'live' | 'replay'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)
const item = ref<any>(null)

const nowMs = ref(Date.now())
const mode = ref<Mode>('pre')
const muted = ref(true)

const videoEl = ref<HTMLVideoElement | null>(null)
const initialSeekDone = ref(false)
const lastLiveSecond = ref<number | null>(null)
const needsUserPlay = ref(false)
const videoError = ref<string | null>(null)
const sessionStartMs = ref<number>(Date.now())

const liveUrl = computed(() => item.value?.liveUrl || '')
const replayUrl = computed(() => item.value?.replayUrl || '')

const modeOverride = computed<Mode | null>(() => {
  const raw = Array.isArray(route.query.mode) ? route.query.mode[0] : route.query.mode
  if (raw === 'live' || raw === 'replay' || raw === 'pre') return raw
  return null
})

const startMs = computed(() => {
  // Article 没有活动时间字段，伪直播用“进入页面时刻”作为开始基准
  return sessionStartMs.value
})

const endMs = computed(() => null as number | null)

const expectedSeconds = computed(() => {
  return Math.max(0, Math.floor((nowMs.value - startMs.value) / 1000))
})

const countdownText = computed(() => {
  // Article 伪直播不做真实排期，pre 仅用于手动调试
  return '即将开始'
})

const liveTimeText = computed(() => {
  const seconds = expectedSeconds.value
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const sec = seconds % 60
  const hh = String(hours).padStart(2, '0')
  const mm = String(minutes).padStart(2, '0')
  const ss = String(sec).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
})

function computeMode() {
  if (modeOverride.value === 'replay') {
    mode.value = 'replay'
    return
  }
  if (modeOverride.value === 'live') {
    mode.value = 'live'
    return
  }
  if (!liveUrl.value && !replayUrl.value) {
    mode.value = 'replay'
    return
  }
  // 默认：有直播就当直播，否则当回放
  mode.value = liveUrl.value ? 'live' : 'replay'
}

const videoSrc = computed(() => {
  if (modeOverride.value === 'replay') return replayUrl.value || liveUrl.value
  if (modeOverride.value === 'live') return liveUrl.value || replayUrl.value
  if (mode.value === 'replay') return replayUrl.value || liveUrl.value
  return liveUrl.value || replayUrl.value
})

function isLikelyVideoUrl(url: string) {
  const u = url.toLowerCase()
  return u.includes('.mp4') || u.includes('.m3u8') || u.includes('.webm') || u.includes('.ogg')
}

const renderKind = computed<'video' | 'iframe'>(() => {
  const url = videoSrc.value
  if (!url) return 'video'
  return isLikelyVideoUrl(url) ? 'video' : 'iframe'
})

const playUrl = computed(() => videoSrc.value)

function clampExpectedToDuration(rawExpected: number) {
  const el = videoEl.value
  if (!el) return rawExpected
  const duration = el.duration
  if (!Number.isFinite(duration) || duration <= 0) return rawExpected
  if (rawExpected >= duration) {
    const mod = rawExpected % Math.floor(duration)
    return Math.max(0, Math.min(mod, Math.max(0, Math.floor(duration - 1))))
  }
  return Math.max(0, Math.min(rawExpected, Math.max(0, Math.floor(duration - 1))))
}

function applyPseudoLivePosition() {
  const el = videoEl.value
  if (!el) return
  if (renderKind.value !== 'video') return
  if (mode.value !== 'live') return
  const target = clampExpectedToDuration(expectedSeconds.value)
  try {
    el.currentTime = target
  } catch {
    // ignore
  }
}

function tryAutoPlay() {
  const el = videoEl.value
  if (!el) return
  if (renderKind.value !== 'video') return
  const p = el.play()
  if (p && typeof (p as Promise<any>).catch === 'function') {
    ;(p as Promise<any>)
      .then(() => {
        needsUserPlay.value = false
        videoError.value = null
      })
      .catch(() => {
        needsUserPlay.value = true
      })
  }
}

function forcePlay() {
  needsUserPlay.value = false
  tryAutoPlay()
}

function forceSync() {
  applyPseudoLivePosition()
  tryAutoPlay()
}

function toggleMute() {
  muted.value = !muted.value
}

function openExternal() {
  if (!playUrl.value) return
  window.open(playUrl.value, '_blank')
}

function goBack() {
  router.push(`/article/${route.params.id}`)
}

function onLoadedMetadata() {
  initialSeekDone.value = false
  videoError.value = null
  if (mode.value === 'live') {
    applyPseudoLivePosition()
    initialSeekDone.value = true
    tryAutoPlay()
    return
  }
  if (mode.value === 'replay' && lastLiveSecond.value != null) {
    const target = clampExpectedToDuration(lastLiveSecond.value)
    try {
      videoEl.value!.currentTime = target
    } catch {
      // ignore
    } finally {
      lastLiveSecond.value = null
    }
  }
}

function onPlay() {
  videoError.value = null
  if (mode.value !== 'live') return
  if (!initialSeekDone.value) {
    applyPseudoLivePosition()
    initialSeekDone.value = true
  }
}

function onPlaying() {
  videoError.value = null
  needsUserPlay.value = false
}

function onPause() {
  if (renderKind.value !== 'video') return
  if (mode.value !== 'live') return
  const el = videoEl.value
  if (!el) return
  if (el.ended) return
  needsUserPlay.value = true
}

function onEnded() {
  if (renderKind.value !== 'video') return
  if (mode.value === 'live') {
    ElMessage.info('视频已播完，等待切换到回放模式')
  }
}

function onSeeking() {
  if (renderKind.value !== 'video') return
  if (mode.value !== 'live') return
  applyPseudoLivePosition()
}

function onVideoError() {
  const el = videoEl.value
  const code = el?.error?.code
  videoError.value = `无法播放该视频（错误码 ${code ?? 'unknown'}）。可能是地址不可访问、CORS、或格式不受支持。`
}

let ticker: number | null = null
let resyncer: number | null = null

async function load() {
  loading.value = true
  error.value = null
  try {
    const { data } = await api.get('/content/article', { params: { id: route.params.id } })
    if (data.code !== 0) throw new Error(data.message || '加载失败')
    item.value = data.data
    if (!item.value?.liveUrl && !item.value?.replayUrl) {
      throw new Error('未配置直播/回放地址')
    }
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch([liveUrl, replayUrl, nowMs, modeOverride], computeMode, { immediate: true })

watch(mode, (next, prev) => {
  if (prev === 'live' && next === 'replay') {
    lastLiveSecond.value = expectedSeconds.value
  }
  if (next === 'live') {
    forceSync()
  }
})

onMounted(async () => {
  sessionStartMs.value = Date.now()
  await load()

  ticker = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)

  resyncer = window.setInterval(() => {
    if (renderKind.value !== 'video') return
    if (mode.value !== 'live') return
    const el = videoEl.value
    if (!el) return
    if (el.paused) {
      tryAutoPlay()
      return
    }
    const target = clampExpectedToDuration(expectedSeconds.value)
    const drift = Math.abs(el.currentTime - target)
    if (drift >= 2) {
      try {
        el.currentTime = target
      } catch {
        // ignore
      }
    }
  }, 5000)
})

onBeforeUnmount(() => {
  if (ticker) window.clearInterval(ticker)
  if (resyncer) window.clearInterval(resyncer)
})
</script>

<style scoped>
.live-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px 16px;
}

.live-card {
  border-radius: 14px;
}

.loading {
  padding: 28px 0;
  text-align: center;
  color: #666;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.status .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #999;
}

.status.live .dot {
  background: #ff3b30;
  box-shadow: 0 0 0 4px rgba(255, 59, 48, 0.18);
}

.status.pre .dot {
  background: #ff9500;
  box-shadow: 0 0 0 4px rgba(255, 149, 0, 0.18);
}

.status.replay .dot {
  background: #34c759;
  box-shadow: 0 0 0 4px rgba(52, 199, 89, 0.18);
}

.pre {
  padding: 16px 0 8px;
  text-align: center;
}

.cover {
  width: 100%;
  max-width: 760px;
  margin: 0 auto 16px;
  overflow: hidden;
  border-radius: 12px;
  background: #f5f5f7;
}

.cover img {
  width: 100%;
  height: 240px;
  object-fit: cover;
  display: block;
}

.countdown {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 1px;
}

.hint {
  margin-top: 8px;
  color: #666;
}

.player {
  margin-top: 10px;
}

.player-shell {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: #000;
}

.video {
  width: 100%;
  height: 520px;
  display: block;
  background: #000;
}

.iframe {
  border: 0;
}

.live-overlay {
  position: absolute;
  left: 12px;
  top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  pointer-events: none;
}

.tap-to-play {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.55);
}

.tap-hint {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.6px;
}

.live-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ff3b30;
  box-shadow: 0 0 0 4px rgba(255, 59, 48, 0.22);
}

.live-time {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-weight: 700;
}

.controls-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.note {
  margin-top: 10px;
  color: #666;
  font-size: 13px;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}
</style>
