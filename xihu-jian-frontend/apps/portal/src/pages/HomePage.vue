<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import type { Activity } from '@xhlj/shared'
import ActivityCard from '../components/ActivityCard.vue'
const activities = ref<Activity[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { activities.value = (await api.activities.list({ page: 1, pageSize: 3 })).items }
  catch (err) { error.value = err instanceof Error ? err.message : '活动加载失败' }
  finally { loading.value = false }
}
onMounted(load)
</script>
<template>
  <section class="hero"><div class="page-container hero-inner"><div class="hero-copy">
    <span class="hero-kicker">WEST LAKE SWORD CONFERENCE</span>
    <h1>让每一次相聚<br /><em>都产生新的可能</em></h1>
    <p>汇聚前沿技术、产业实践与创新思想，探索数字时代的安全与发展。</p>
    <div class="hero-actions"><RouterLink to="/activities"><el-button type="primary" size="large" round>浏览大会议程</el-button></RouterLink><a href="#about"><el-button size="large" plain round>了解大会</el-button></a></div>
    <div class="hero-meta"><span>2026 · 11月</span><i></i><span>中国 · 杭州</span></div>
  </div><div class="hero-panel"><div class="panel-label">CONFERENCE 2026</div><div class="panel-number">2026</div><div class="panel-line"></div><div class="panel-bottom"><span>连接技术与产业</span><span>杭州 · 中国</span></div></div></div></section>
  <section id="about" class="page-container section about-section"><div class="about-copy"><span class="section-kicker">ABOUT THE CONFERENCE</span><h2>了解大会</h2><p>西湖论剑大会聚焦数字安全、人工智能和产业创新，汇集行业专家、企业代表与技术实践者，共同交流技术趋势与落地经验。</p></div><div class="about-stats"><div><strong>前沿</strong><span>技术趋势</span></div><div><strong>开放</strong><span>产业交流</span></div><div><strong>实践</strong><span>成果分享</span></div></div></section>
  <section id="activities" class="section activities-section"><div class="page-container"><div class="section-heading"><div><span class="section-kicker">AGENDA & EVENTS</span><h2>近期活动</h2><p>查看大会重点场次，选择感兴趣的活动。</p></div><RouterLink class="more-link" to="/activities">查看全部议程 →</RouterLink></div>
    <div v-if="loading" class="state-box">正在加载活动…</div><el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" /><div v-else class="activity-grid"><ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" /></div>
  </div></section>
</template>
<style scoped>
.hero { position: relative; overflow: hidden; background: linear-gradient(112deg,#092b63 0%,#1552a7 55%,#2c83d9 100%); color: white; }
.hero::before { content: ""; position: absolute; inset: 0; opacity: .13; background-image: linear-gradient(120deg,transparent 35%,rgba(255,255,255,.3) 35.2%,transparent 35.5%),linear-gradient(30deg,transparent 60%,rgba(255,255,255,.25) 60.2%,transparent 60.5%); background-size: 180px 180px; }
.hero-inner { min-height: 440px; display: flex; align-items: center; justify-content: space-between; gap: 50px; position: relative; z-index: 1; padding-top: 55px; padding-bottom: 55px; }
.hero-copy { max-width: 600px; } .hero-kicker,.section-kicker { font-size: 11px; letter-spacing: 2px; font-weight: 700; color: #9ecbff; }
.hero h1 { font-size: clamp(36px,5vw,58px); line-height: 1.22; letter-spacing: 1px; margin: 22px 0 18px; } .hero h1 em { font-style: normal; color: #a9d5ff; }
.hero-copy p { color: #d5e6ff; line-height: 1.8; max-width: 510px; } .hero-actions { display: flex; gap: 12px; margin-top: 28px; }
.hero-meta { display: flex; align-items: center; gap: 12px; margin-top: 30px; color: #c4dcf7; font-size: 12px; } .hero-meta i { width: 4px; height: 4px; border-radius: 50%; background: #8fc5ff; }
.hero-panel { width: 340px; min-width: 280px; padding: 30px; border: 1px solid rgba(255,255,255,.28); border-radius: 22px; background: linear-gradient(145deg,rgba(255,255,255,.16),rgba(255,255,255,.04)); box-shadow: 0 30px 80px rgba(0,0,0,.15); transform: rotate(2deg); }
.panel-label { color: #b9d9ff; letter-spacing: 2px; font-size: 11px; } .panel-number { font-size: 92px; line-height: 1.2; font-weight: 800; letter-spacing: -7px; margin: 32px 0; }
.panel-line { height: 1px; background: rgba(255,255,255,.35); } .panel-bottom { display: flex; justify-content: space-between; gap: 12px; margin-top: 15px; color: #c5def9; font-size: 11px; }
.about-section { display: flex; align-items: center; justify-content: space-between; gap: 50px; } .about-copy { max-width: 620px; } .about-copy .section-kicker,.activities-section .section-kicker { color: #4388df; }
.about-copy h2 { font-size: 28px; margin: 12px 0; } .about-copy p { color: #66758c; line-height: 1.9; }
.about-stats { display: flex; gap: 30px; } .about-stats div { display: grid; gap: 6px; text-align: center; } .about-stats strong { color: #155eef; font-size: 24px; } .about-stats span { color: #8290a5; font-size: 12px; }
.activities-section { background: #f0f5fb; } .activity-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 18px; } .more-link { color: #155eef; font-size: 14px; font-weight: 600; white-space: nowrap; }
.state-box { padding: 50px; text-align: center; color: #8290a5; background: white; border-radius: 14px; }
@media (max-width:850px) { .hero-panel { display: none; } .hero-inner { min-height: 400px; } .about-section { align-items: flex-start; flex-direction: column; gap: 24px; } .activity-grid { grid-template-columns: 1fr 1fr; } }
@media (max-width:600px) { .activity-grid { grid-template-columns: 1fr; } .about-stats { width: 100%; justify-content: space-between; } }
</style>
