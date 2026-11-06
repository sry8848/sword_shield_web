<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import type { StatisticsOverview } from '@xhlj/shared'
const data = ref<StatisticsOverview>({ eventCount: 0, registrationCount: 0, checkedInCount: 0, checkInRate: 0 }); const loading = ref(false)
async function load() { loading.value = true; try { data.value = await api.admin.overview() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '统计加载失败') } finally { loading.value = false } }
onMounted(load)
</script>
<template><section class="admin-page"><h1 class="page-title">基础统计</h1><p class="page-description">当前骨架仅展示 MVP 所需的基础活动、报名和到场指标。</p><div v-loading="loading" class="metric-grid"><div class="metric-card"><div class="metric-label">活动数</div><div class="metric-value">{{ data.eventCount }}</div></div><div class="metric-card"><div class="metric-label">报名记录数</div><div class="metric-value">{{ data.registrationCount }}</div></div><div class="metric-card"><div class="metric-label">已核验人数</div><div class="metric-value">{{ data.checkedInCount }}</div></div></div><el-alert type="info" :closable="false" show-icon title="统计口径提示：当前数据来自本地 Mock。真实环境需由后端明确报名、取消、核验和去重口径。" /></section></template>
