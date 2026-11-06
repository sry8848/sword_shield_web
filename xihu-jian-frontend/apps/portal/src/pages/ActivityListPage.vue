<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import type { Activity } from '@xhlj/shared'
import ActivityCard from '../components/ActivityCard.vue'
const keyword = ref('')
const activities = ref<Activity[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { activities.value = (await api.activities.list({ keyword: keyword.value, page: 1, pageSize: 50 })).records }
  catch (err) { error.value = err instanceof Error ? err.message : '活动加载失败' }
  finally { loading.value = false }
}
onMounted(load)
</script>
<template>
  <section class="page-container list-page"><div class="intro"><span class="section-kicker">AGENDA & ATTENDANCE</span><h1>议程与参会</h1><p>浏览大会场次，了解活动时间、地点与报名状态。</p></div>
    <div class="filter-bar surface-card"><el-input v-model="keyword" placeholder="搜索活动名称、主题或会场" clearable @keyup.enter="load" /><el-button type="primary" @click="load">搜索活动</el-button></div>
    <div v-if="loading" class="state">正在加载活动…</div><el-alert v-else-if="error" :title="error" type="error" :closable="false" show-icon /><el-empty v-else-if="activities.length === 0" description="没有找到符合条件的活动" /><div v-else class="activity-grid"><ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" /></div>
  </section>
</template>
<style scoped>
.list-page { min-height: 65vh; padding-top: 44px; } .intro { margin-bottom: 28px; } .section-kicker { color: #4388df; font-size: 11px; letter-spacing: 2px; font-weight: 700; }
h1 { margin: 12px 0 8px; font-size: 32px; } .intro p { margin: 0; color: #66758c; }
.filter-bar { display: flex; gap: 12px; padding: 16px; margin-bottom: 22px; } .filter-bar .el-input { max-width: 420px; }
.activity-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 18px; } .state { padding: 60px; text-align: center; color: #8290a5; }
@media (max-width:900px) { .activity-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width:600px) { .activity-grid { grid-template-columns: 1fr; } .filter-bar { flex-direction: column; } .filter-bar .el-input { max-width: none; } }
</style>
