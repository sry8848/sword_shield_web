<template>
  <div class="home-page">
    <section class="hero-panel">
      <div class="hero-left">
        <div class="hero-badge">西湖论剑 · 会议运营平台</div>
        <h1 class="hero-title">构建会议活动的数字安全防线</h1>
        <p class="hero-subtitle">
          聚焦会议全流程数字安全管理，为主办方提供活动报名、签到管理、内容分发与数据分析的一体化能力。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/activities')">
            查看活动日程
          </el-button>
          <el-button size="large" plain @click="$router.push('/statistics')">
            浏览数据看板
          </el-button>
        </div>

        <!-- 新增：会议图片轮播 -->
        <div class="hero-carousel">
          <el-carousel 
            :interval="4000" 
            height="340px"
            :autoplay="true"
            arrow="hover"
          >
            <el-carousel-item v-for="(item, index) in carouselItems" :key="index">
              <div class="carousel-card" @click="goToActivity(item.link)">
                <img :src="item.image" :alt="item.title" class="carousel-image" />
                <div class="carousel-overlay">
                  <div class="carousel-content">
                    <span class="carousel-tag">{{ item.tag }}</span>
                    <h4>{{ item.title }}</h4>
                    <p>{{ item.desc }}</p>
                  </div>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <div class="hero-highlights">
          <div class="highlight-item">
            <div class="highlight-number">360°</div>
            <div class="highlight-label">全流程安全管理</div>
          </div>
          <div class="highlight-item">
            <div class="highlight-number">24×7</div>
            <div class="highlight-label">实时运行监测</div>
          </div>
          <div class="highlight-item">
            <div class="highlight-number">多场景</div>
            <div class="highlight-label">论坛 / 展会 / 路演</div>
          </div>
        </div>

        <!-- 新增：快捷入口 -->
        <div class="quick-access">
          <div class="quick-item" @click="$router.push('/activities')">
            <div class="quick-icon"><el-icon><Calendar /></el-icon></div>
            <span>活动报名</span>
          </div>
          <div class="quick-item" @click="$router.push('/materials')">
            <div class="quick-icon"><el-icon><Download /></el-icon></div>
            <span>资料下载</span>
          </div>
          <div class="quick-item" @click="$router.push('/media')">
            <div class="quick-icon"><el-icon><VideoCamera /></el-icon></div>
            <span>精彩回顾</span>
          </div>
          <div class="quick-item" @click="$router.push('/submit')">
            <div class="quick-icon"><el-icon><EditPen /></el-icon></div>
            <span>在线投稿</span>
          </div>
        </div>
      </div>

      <div class="hero-right">
        <div class="hero-card primary">
          <div class="card-header">
            <h3>热门活动</h3>
            <el-button text @click="$router.push('/activities')">全部活动</el-button>
          </div>
          <div class="card-body scrollable">
            <Activities minimal />
          </div>
        </div>

        <div class="hero-card">
          <div class="card-header">
            <h3>最新资讯</h3>
            <el-button text @click="$router.push('/articles')">更多资讯</el-button>
          </div>
          <div class="card-body scrollable">
            <Articles minimal />
          </div>
        </div>

        <div v-if="isLoggedIn" class="hero-card compact">
          <div class="card-header">
            <h3>我报名的活动</h3>
            <el-button text @click="loadMyActivities">刷新</el-button>
          </div>
          <div class="card-body my-activities">
            <div v-if="myActivitiesLoading" class="loading-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <div v-else-if="myActivities.length === 0" class="empty-state">
              <p>您还没有报名任何活动</p>
              <el-button type="primary" size="small" @click="$router.push('/activities')">
                去报名活动
              </el-button>
            </div>
            <ul v-else class="my-activities-list">
              <li v-for="activity in myActivities" :key="activity.id" class="my-activity-item">
                <div class="activity-info" @click="$router.push(`/activity/${activity.id}`)">
                  <div class="activity-title">
                    <span class="activity-dot" />
                    <span>{{ activity.title }}</span>
                  </div>
                  <div class="activity-meta">
                    <span class="meta-item">{{ formatDateTime(activity.startTime) }}</span>
                    <span class="meta-item channel">{{ getChannelLabel(activity.registrationChannel) }}</span>
                  </div>
                </div>
                <el-tag :type="getActivityStatus(activity).type" size="small">
                  {{ getActivityStatus(activity).label }}
                </el-tag>
              </li>
            </ul>
            <div v-if="myActivitiesTotal > 5" class="view-all">
              <el-button text size="small" @click="$router.push('/profile')">
                查看全部 ({{ myActivitiesTotal }})
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 新增：近期大会活动展示区块 -->
    <section class="conference-section">
      <div class="conference-header">
        <div class="conference-title-block">
          <div class="section-label">近期大会 · 精选活动</div>
          <h2>聚焦数字安全大会与高端论坛</h2>
          <p>
            汇总最近开展和即将举办的重点大会活动，从主论坛到分论坛，再到配套展览，
            为参会者提供一目了然的时间轴与关键信息。
          </p>
        </div>
        <div class="conference-cta">
          <el-button type="primary" size="large" @click="$router.push('/activities')">
            查看全部活动
          </el-button>
        </div>
      </div>

      <div class="conference-layout">
        <div class="conference-main">
          <ul class="conference-timeline">
            <li
              v-for="(item, index) in featuredConferences"
              :key="item.id || index"
              class="conference-item"
            >
              <div class="timeline-node">
                <div class="timeline-dot" />
                <div class="timeline-line" />
              </div>
              <div class="conference-content">
                <div class="conference-meta-row">
                  <span class="conference-tag">{{ item.type }}</span>
                  <span class="conference-status" :data-status="item.status">
                    {{ getStatusText(item.status) }}
                  </span>
                </div>
                <h3 class="conference-name">{{ item.title }}</h3>
                <p class="conference-desc">{{ item.description }}</p>
                <div class="conference-meta">
                  <span class="meta-chip">
                    <span class="meta-dot primary" />
                    {{ item.dateRange }}
                  </span>
                  <span class="meta-chip">
                    <span class="meta-dot" />
                    {{ item.location }}
                  </span>
                  <span class="meta-chip">
                    <span class="meta-dot" />
                    {{ item.scale }}
                  </span>
                </div>
              </div>
            </li>
          </ul>
        </div>

        <aside class="conference-side">
          <div class="side-card">
            <h3>会议运营概览</h3>
            <ul class="side-metrics">
              <li>
                <div class="metric-label">近 30 天累计参会人数</div>
                <div class="metric-value">8,420</div>
              </li>
              <li>
                <div class="metric-label">正在筹备的大会</div>
                <div class="metric-value metric-accent">5 场</div>
              </li>
              <li>
                <div class="metric-label">平均签到完成率</div>
                <div class="metric-value">96.3%</div>
              </li>
            </ul>
          </div>

          <div class="side-card secondary">
            <h3>推荐操作</h3>
            <ul class="side-actions">
              <li @click="$router.push('/points')">
                <div class="action-icon action-icon-primary">
                  <el-icon><Trophy /></el-icon>
                </div>
                <div class="action-text">
                  <div class="action-title">积分详情</div>
                  <div class="action-desc">查看积分余额与获取记录。</div>
                </div>
              </li>
              <li @click="$router.push('/submit')">
                <div class="action-icon action-icon-secondary">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="action-text">
                  <div class="action-title">提交演讲/成果</div>
                  <div class="action-desc">在线管理演讲稿、PPT 与成果材料。</div>
                </div>
              </li>
              <li @click="$router.push('/materials')">
                <div class="action-icon action-icon-tertiary">
                  <el-icon><Download /></el-icon>
                </div>
                <div class="action-text">
                  <div class="action-title">下载大会资料</div>
                  <div class="action-desc">统一下载会议议程、手册与活动纪要。</div>
                </div>
              </li>
            </ul>
          </div>
        </aside>
      </div>
    </section>

    <section class="poster-section">
      <div class="poster-header">
        <div>
          <h2>智能海报生成</h2>
          <p>为活动快速生成视觉统一的宣传海报，支持标题与副标题定制。</p>
        </div>
        <div class="poster-actions">
          <el-button type="primary" @click="generatePoster" :loading="generating">
            生成海报
          </el-button>
        </div>
      </div>
      <el-form @submit.prevent class="poster-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="海报标题">
              <el-input v-model="title" placeholder="请输入海报主标题，如“西湖论剑”" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="副标题">
              <el-input v-model="subtitle" placeholder="请输入副标题，如“数字安全大会”" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div v-if="posterUrl" class="poster-preview">
        <div class="poster-result">
          <el-input v-model="posterUrl" readonly />
          <div class="poster-result-actions">
            <el-button type="primary" @click="openPoster">打开海报</el-button>
            <el-button @click="copyPosterUrl">复制链接</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="feature-section">
      <div class="feature-header">
        <div>
          <h2>平台能力概览</h2>
          <p>围绕会议活动全生命周期，提供标准化却不失弹性的数字安全能力。</p>
        </div>
      </div>
      <div class="feature-grid">
        <div class="feature-card">
          <h3>活动运营</h3>
          <p>支持多类型活动的报名发布、席位管理和日程编排，满足大型会议的复杂运营需求。</p>
        </div>
        <div class="feature-card">
          <h3>安全管控</h3>
          <p>结合访问控制、行为审计与风控策略，降低敏感数据在活动流转中的泄露风险。</p>
        </div>
        <div class="feature-card">
          <h3>数据洞察</h3>
          <p>实时采集参会行为和互动数据，为主办方提供多维度的运营分析与复盘依据。</p>
        </div>
        <div class="feature-card">
          <h3>内容协同</h3>
          <p>统一管理议程内容、嘉宾资料与会议成果，支持多渠道分发与在线留存。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * 首页（用户端）。
 *
 * - 复用 `Activities` / `Articles`（minimal 模式）展示热门活动与最新资讯。
 * - 登录后展示“我报名的活动”，并提供海报生成等互动入口。
 *
 * 依赖接口（部分）：
 * - GET `/activity/list`：热门活动/轮播数据
 * - GET `/activity/my-registrations`：我的报名活动
 * - POST `/content/poster`、GET `/content/poster/jobs/:jobId`：海报生成任务
 */
import Activities from './Activities.vue'
import Articles from './Articles.vue'
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'
import { Loading, Trophy, Document, Download, Calendar, VideoCamera, EditPen } from '@element-plus/icons-vue'

const router = useRouter()

const title = ref('西湖论剑')
const subtitle = ref('数字安全大会')
const posterUrl = ref('')
const generating = ref(false)

const myActivities = ref<any[]>([])
const myActivitiesTotal = ref(0)
const myActivitiesLoading = ref(false)

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

// 轮播图数据 - 从后端获取热门活动
const carouselItems = ref<any[]>([])

// 默认轮播图片库 - 会议/科技/峰会主题
const defaultImages = [
  'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=400&fit=crop', // 大型会议现场
  'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=800&h=400&fit=crop', // 团队会议讨论
  'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=400&fit=crop', // 数据中心/服务器
  'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&h=400&fit=crop', // 网络安全主题
  'https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800&h=400&fit=crop'  // 科技女性刷卡
]

// 加载轮播图活动数据
async function loadCarouselActivities() {
  try {
    const { data } = await api.get('/activity/list', {
      params: { page: 0, size: 5, orderBy: 'heat' }
    })
    if (data.code === 0 && data.data.items.length > 0) {
      carouselItems.value = data.data.items.map((item: any, index: number) => ({
        id: item.id,
        title: item.title,
        desc: item.summary || '点击查看活动详情',
        tag: item.heat > 80 ? '热门活动' : item.heat > 50 ? '精选活动' : '近期活动',
        image: item.coverUrl || defaultImages[index % defaultImages.length],
        link: `/activity/${item.id}`
      }))
    }
  } catch (error) {
    console.error('Failed to load carousel activities:', error)
  }
}

// 新增：静态示例的近期大会活动数据，后续可接入后端接口
const featuredConferences = ref([
  {
    id: undefined,
    title: '西湖论剑 · 网络空间安全大会',
    description: '围绕数字安全基础设施、人工智能安全与关键行业防护展开高端交流。',
    type: '旗舰大会',
    status: 'ongoing',
    dateRange: '正在进行 · 今日 - 本周五',
    location: '杭州 · 国际博览中心',
    scale: '主论坛 + 6 个分论坛 + 安全展区'
  },
  {
    id: undefined,
    title: '数字政府安全治理高峰论坛',
    description: '聚焦政务云、城市大脑与公共服务平台的安全治理与合规实践。',
    type: '高峰论坛',
    status: 'upcoming',
    dateRange: '即将开始 · 下周三 09:00',
    location: '线上 + 线下联合举办',
    scale: '预计 800+ 参会嘉宾'
  },
  {
    id: undefined,
    title: '关键基础设施安全技术闭门研讨会',
    description: '面向能源、金融、电信等重点行业，聚焦关键基础设施安全防护能力建设。',
    type: '闭门研讨',
    status: 'upcoming',
    dateRange: '报名中 · 12 月下旬',
    location: '上海 · 浦东新区',
    scale: '受邀制 · 小规模深度交流'
  }
])

function getStatusText(status: string) {
  switch (status) {
    case 'ongoing':
      return '进行中'
    case 'upcoming':
      return '即将开始'
    case 'finished':
      return '已结束'
    default:
      return '进行中'
  }
}

function formatDateTime(dateStr: string) {
  if (!dateStr) return '时间待定'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getActivityStatus(activity: any) {
  const now = new Date()
  const startTime = activity.startTime ? new Date(activity.startTime) : null
  const endTime = activity.endTime ? new Date(activity.endTime) : null

  if (endTime && now > endTime) {
    return { label: '已结束', type: 'info' }
  }
  if (startTime && now >= startTime && (!endTime || now <= endTime)) {
    return { label: '进行中', type: 'success' }
  }
  if (startTime && now < startTime) {
    return { label: '未开始', type: 'warning' }
  }
  return { label: '待定', type: '' }
}

function getChannelLabel(channel: string) {
  const channelMap: Record<string, string> = {
    web: '网页报名',
    app: 'APP报名',
    onsite: '现场报名',
    share_link: '分享链接',
    wechat: '微信报名',
    admin: '后台添加'
  }
  return channelMap[channel] || channel || '网页报名'
}

async function loadMyActivities() {
  if (!isLoggedIn.value) return

  myActivitiesLoading.value = true
  try {
    const { data } = await api.get('/activity/my-registrations', {
      params: { page: 0, size: 5 }
    })
    if (data.code === 0) {
      myActivities.value = data.data.items
      myActivitiesTotal.value = data.data.total
    }
  } catch (error: any) {
    if (error.response?.status !== 401) {
      console.error('Failed to load registered activities:', error)
    }
  } finally {
    myActivitiesLoading.value = false
  }
}

async function generatePoster() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后生成海报')
    router.push('/login')
    return
  }
  if (!title.value.trim()) {
    ElMessage.warning('请输入海报标题')
    return
  }

  generating.value = true
  try {
    const params = new URLSearchParams({ title: title.value, subtitle: subtitle.value })
    posterUrl.value = ''
    ElMessage.info('海报生成中，请稍候...')

    const { data: createRes } = await api.post('/content/poster', params)
    if (createRes.code !== 0) {
      throw new Error(createRes.message || '海报任务提交失败')
    }
    const jobId = createRes?.data?.jobId
    if (!jobId) {
      throw new Error('海报任务返回 jobId 为空')
    }

    const start = Date.now()
    while (Date.now() - start < 30_000) {
      const { data: statusRes } = await api.get(`/content/poster/jobs/${jobId}`)
      if (statusRes.code !== 0) {
        throw new Error(statusRes.message || '查询海报任务失败')
      }
      const status = statusRes?.data?.status
      if (status === 'SUCCESS') {
        posterUrl.value = statusRes?.data?.url || ''
        break
      }
      if (status === 'FAILED') {
        throw new Error(statusRes?.data?.errorMessage || '海报生成失败')
      }
      await new Promise((r) => setTimeout(r, 1000))
    }
    if (!posterUrl.value) {
      throw new Error('海报生成超时，请稍后重试')
    }
    ElMessage.success('海报生成成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function openPoster() {
  if (!posterUrl.value) return
  window.open(posterUrl.value, '_blank')
}

async function copyPosterUrl() {
  if (!posterUrl.value) return
  try {
    await navigator.clipboard.writeText(posterUrl.value)
    ElMessage.success('已复制链接')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 轮播图点击跳转
function goToActivity(link: string) {
  if (link) {
    router.push(link)
  }
}

onMounted(() => {
  loadCarouselActivities()
  if (isLoggedIn.value) {
    loadMyActivities()
  }
})
</script>

<style scoped>
.home-page {
  max-width: 1240px;
  margin: 0 auto 40px;
  color: #111827;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 6.5fr) minmax(0, 5.5fr);
  gap: 32px;
  align-items: stretch;
}

.hero-left {
  padding: 28px 24px;
  border-radius: 24px;
  background: #f5f9ff;
  box-shadow: 0 14px 35px rgba(148, 163, 184, 0.3);
  border: 1px solid rgba(191, 219, 254, 0.9);
  display: flex;
  flex-direction: column;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #1d4ed8;
  background: #e0ecff;
  border: 1px solid rgba(147, 197, 253, 0.9);
}

.hero-title {
  margin-top: 18px;
  font-size: 32px;
  line-height: 1.3;
  font-weight: 700;
  color: #0f172a;
}

.hero-subtitle {
  margin-top: 16px;
  font-size: 14px;
  line-height: 1.7;
  color: #4b5563;
}

.hero-actions {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

/* 新增：轮播样式 */
.hero-carousel {
  margin-top: 20px;
  margin-bottom: 20px;
}

.hero-carousel :deep(.el-carousel__container) {
  height: 340px;
}

.hero-carousel :deep(.el-carousel__indicators--outside) {
  margin-top: 8px;
}

.hero-carousel :deep(.el-carousel__indicator--horizontal .el-carousel__button) {
  width: 24px;
  height: 4px;
  border-radius: 2px;
  background: #cbd5e1;
}

.hero-carousel :deep(.el-carousel__indicator--horizontal.is-active .el-carousel__button) {
  background: #2563eb;
}

.hero-carousel :deep(.el-carousel__arrow) {
  background-color: rgba(255, 255, 255, 0.9);
  color: #1f2937;
  width: 32px;
  height: 32px;
}

.hero-carousel :deep(.el-carousel__arrow:hover) {
  background-color: #ffffff;
}

.carousel-card {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.2);
  cursor: pointer;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-card:hover .carousel-image {
  transform: scale(1.05);
}

.carousel-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(15, 23, 42, 0.85) 0%, rgba(15, 23, 42, 0.3) 50%, transparent 100%);
  display: flex;
  align-items: flex-end;
  padding: 20px;
}

.carousel-content {
  color: #ffffff;
}

.carousel-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  background: rgba(59, 130, 246, 0.9);
  margin-bottom: 8px;
}

.carousel-content h4 {
  margin: 0 0 6px 0;
  font-size: 18px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.carousel-content p {
  margin: 0;
  font-size: 13px;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 新增：快捷入口样式 */
.quick-access {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 8px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(209, 213, 219, 0.8);
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-item:hover {
  border-color: #2563eb;
  background: #eff6ff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.15);
}

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 20px;
}

.quick-item span {
  font-size: 12px;
  color: #374151;
  font-weight: 500;
}

.hero-highlights {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.highlight-item {
  min-width: 120px;
}

.highlight-number {
  font-size: 18px;
  font-weight: 600;
  color: #1d4ed8;
}

.highlight-label {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.hero-right {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-card {
  border-radius: 18px;
  padding: 16px 16px 12px;
  background: #ffffff;
  border: 1px solid rgba(209, 213, 219, 0.9);
  box-shadow: 0 8px 22px rgba(148, 163, 184, 0.32);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  transform-origin: center;
}

.hero-card.primary {
  border-color: rgba(59, 130, 246, 0.85);
}

.hero-card.compact {
  padding-bottom: 10px;
}

.hero-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(148, 163, 184, 0.5);
  border-color: rgba(59, 130, 246, 0.9);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-header h3 {
  font-size: 14px;
  color: #111827;
}

.card-body.scrollable {
  max-height: 240px;
  overflow: hidden;
}

.card-body.scrollable:hover {
  overflow-y: auto;
}

.my-activities {
  min-height: 80px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px 12px;
  color: #6b7280;
  font-size: 13px;
}

.my-activities-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.my-activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 10px 8px;
  border-radius: 12px;
  background: #f9fafb;
  border: 1px solid rgba(209, 213, 219, 0.9);
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease,
    background-color 0.18s ease;
}

.my-activity-item:hover {
  border-color: rgba(59, 130, 246, 0.9);
  background-color: #eff6ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(148, 163, 184, 0.45);
}

.activity-info {
  flex: 1;
}

.activity-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #111827;
}

.activity-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 0, #22c55e, #16a34a 45%, #bbf7d0 100%);
}

.activity-meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #6b7280;
}

.activity-meta .channel {
  padding: 2px 8px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #1d4ed8;
}

.view-all {
  margin-top: 8px;
  text-align: right;
}

.poster-section {
  margin-top: 32px;
  padding: 20px 20px 18px;
  border-radius: 20px;
  background: #f5f9ff;
  border: 1px solid rgba(191, 219, 254, 0.9);
  box-shadow: 0 16px 38px rgba(148, 163, 184, 0.32);
}

.poster-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.poster-header h2 {
  font-size: 18px;
  color: #111827;
}

.poster-header p {
  margin-top: 4px;
  font-size: 13px;
  color: #4b5563;
}

.poster-actions {
  flex-shrink: 0;
}

.poster-form {
  margin-top: 8px;
}

.poster-preview {
  margin-top: 16px;
  padding: 14px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid rgba(209, 213, 219, 0.9);
  box-shadow: 0 12px 30px rgba(148, 163, 184, 0.45);
}

.poster-result {
  display: grid;
  gap: 12px;
}

.poster-result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.feature-section {
  margin-top: 32px;
  padding: 20px 20px 24px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(209, 213, 219, 0.9);
  box-shadow: 0 12px 32px rgba(148, 163, 184, 0.28);
}

.feature-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.feature-header h2 {
  font-size: 18px;
  color: #111827;
}

.feature-header p {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.feature-card {
  padding: 14px 14px 12px;
  border-radius: 14px;
  background: #f9fafb;
  border: 1px solid rgba(229, 231, 235, 0.9);
  transition: background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease,
    border-color 0.18s ease;
}

.feature-card:hover {
  background: #eff6ff;
  border-color: rgba(59, 130, 246, 0.8);
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(148, 163, 184, 0.35);
}

.feature-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.feature-card p {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

/* 新增：近期大会活动区块样式 */
.conference-section {
  margin-top: 32px;
  padding: 22px 20px 24px;
  border-radius: 22px;
  background: radial-gradient(circle at top left, #e0ecff 0, #ffffff 40%, #eef2ff 100%);
  border: 1px solid rgba(191, 219, 254, 0.9);
  box-shadow: 0 18px 42px rgba(148, 163, 184, 0.38);
}

.conference-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  margin-bottom: 18px;
}

.section-label {
  display: inline-flex;
  padding: 3px 10px;
  font-size: 11px;
  border-radius: 999px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #1d4ed8;
  background: rgba(191, 219, 254, 0.8);
  border: 1px solid rgba(147, 197, 253, 0.9);
}

.conference-header h2 {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.conference-header p {
  margin-top: 6px;
  font-size: 13px;
  color: #4b5563;
  max-width: 520px;
}

.conference-cta {
  flex-shrink: 0;
}

.conference-layout {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(0, 5fr);
  gap: 20px;
}

.conference-main {
  padding: 16px 18px 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(209, 213, 219, 0.9);
}

.conference-timeline {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.conference-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 10px 10px 8px;
  border-radius: 14px;
  background: #f9fafb;
  border: 1px solid rgba(229, 231, 235, 0.9);
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease,
    transform 0.18s ease;
}

.conference-item:hover {
  background: #eff6ff;
  border-color: rgba(59, 130, 246, 0.9);
  box-shadow: 0 12px 26px rgba(148, 163, 184, 0.42);
  transform: translateY(-3px);
}

.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 4px;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: conic-gradient(from 160deg, #1d4ed8, #22c55e, #0ea5e9, #1d4ed8);
  box-shadow: 0 0 0 3px rgba(191, 219, 254, 0.9);
}

.timeline-line {
  flex: 1;
  width: 2px;
  margin-top: 4px;
  background: linear-gradient(to bottom, rgba(191, 219, 254, 0.9), rgba(209, 213, 219, 0));
}

.conference-content {
  min-width: 0;
}

.conference-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.conference-tag {
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #1d4ed8;
}

.conference-status {
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.9);
  color: #4b5563;
}

.conference-status[data-status='ongoing'] {
  background: #dcfce7;
  border-color: #22c55e;
  color: #166534;
}

.conference-status[data-status='upcoming'] {
  background: #fef9c3;
  border-color: #facc15;
  color: #854d0e;
}

.conference-status[data-status='finished'] {
  background: #e5e7eb;
  border-color: #9ca3af;
  color: #374151;
}

.conference-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.conference-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

.conference-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #4b5563;
  background: rgba(243, 244, 246, 0.95);
}

.meta-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #cbd5f5;
}

.meta-dot.primary {
  background: #2563eb;
}

.conference-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card {
  padding: 14px 14px 12px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.96);
  color: #e5e7eb;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.6);
}

.side-card.secondary {
  background: #f9fafb;
  color: #111827;
  box-shadow: 0 10px 26px rgba(148, 163, 184, 0.45);
}

.side-card h3 {
  font-size: 14px;
  margin-bottom: 10px;
}

.side-metrics {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-label {
  font-size: 12px;
  color: #9ca3af;
}

.metric-value {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 600;
}

.metric-accent {
  color: #60a5fa;
}

.side-actions {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-actions li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.side-actions li:hover {
  background: #eff6ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(148, 163, 184, 0.45);
}

.action-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #6b7280;
  flex-shrink: 0;
}

.action-icon-primary {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #ffffff;
}

.action-icon-secondary {
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
  color: #ffffff;
}

.action-icon-tertiary {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  color: #ffffff;
}

.action-text {
  flex: 1;
  min-width: 0;
}

.action-title {
  font-size: 13px;
  font-weight: 500;
}

.action-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 960px) {
  .hero-panel {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-left {
    min-height: auto;
  }

  .hero-right {
    order: -1;
  }

  .conference-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .quick-access {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 640px) {
  .hero-left {
    padding: 22px 18px;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-carousel {
    min-height: 200px;
  }

  .quick-access {
    grid-template-columns: repeat(2, 1fr);
  }

  .conference-section {
    padding-inline: 16px;
  }

  .conference-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .conference-cta {
    width: 100%;
    display: flex;
    justify-content: flex-start;
  }

  .conference-side {
    flex-direction: column;
  }

  .poster-section,
  .feature-section {
    padding-inline: 16px;
  }

  .feature-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
