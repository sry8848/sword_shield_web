<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { StatisticsOverview } from '@xhlj/shared'
const auth = useAuthStore()
const data = ref<StatisticsOverview>({ eventCount: 0, registrationCount: 0, checkedInCount: 0, checkInRate: 0 }); const loading = ref(false)
async function load() { loading.value = true; try { data.value = await api.admin.overview() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '统计加载失败') } finally { loading.value = false } }
onMounted(load)
</script>
<template><section class="admin-page"><h1 class="page-title">运营概览</h1><p class="page-description">查看大会核心运营数据和常用工作入口。</p>
  <div v-loading="loading" class="metric-grid"><div class="metric-card"><div class="metric-label">活动</div><div class="metric-value">{{ data.eventCount }}</div></div><div class="metric-card"><div class="metric-label">累计报名</div><div class="metric-value">{{ data.registrationCount }}</div></div><div class="metric-card"><div class="metric-label">已核验入场</div><div class="metric-value">{{ data.checkedInCount }}</div></div></div>
  <el-card shadow="never"><template #header><strong>常用工作入口</strong></template><div class="quick-links"><RouterLink v-if="auth.can('event:read')" to="/activities">活动管理 <span>维护活动内容和报名规则 →</span></RouterLink><RouterLink v-if="auth.can('registration:read')" to="/registrations">报名管理 <span>查看参会人员报名记录 →</span></RouterLink><RouterLink v-if="auth.can('checkin:create')" to="/checkin">现场核验 <span>核验参会凭证 →</span></RouterLink><RouterLink to="/statistics">基础统计 <span>查看报名和到场情况 →</span></RouterLink></div></el-card>
</section></template>
<style scoped>
.quick-links { display: grid; grid-template-columns: repeat(2,1fr); gap: 12px; }
.quick-links a { padding: 18px; border: 1px solid #e8edf4; border-radius: 8px; font-weight: 600; color: #1f2d3d; }
.quick-links a span { display: block; margin-top: 8px; color: #8491a3; font-size: 12px; font-weight: 400; }
.quick-links a:hover { border-color: #a9c7f5; background: #f8fbff; }
@media (max-width:650px) { .quick-links { grid-template-columns: 1fr; } }
</style>
