<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from '../api/client'
import type { Activity } from '@xhlj/shared'
import ActivityCard from '../components/ActivityCard.vue'
const keyword = ref('')
const page = ref(1)
const pageSize = 12
const total = ref(0)
const activities = ref<Activity[]>([])
const loading = ref(false)
const error = ref('')
// 只有持有当前编号的请求可以更新列表状态。
let requestVersion = 0

/** 根据当前关键词和页码加载公开场次，并更新列表状态。 */
async function load() {
  // 1. 标记本次请求为最新请求，并进入加载状态。
  const version = ++requestVersion
  loading.value = true
  error.value = ''

  // 2. 读取场次，仅用最新请求的结果更新页面。
  try {
    const result = await api.activities.list({ keyword: keyword.value, page: page.value, pageSize })
    if (version !== requestVersion)
      return
    activities.value = result.items
    total.value = result.pageInfo.total
  }
  catch (err) {
    if (version === requestVersion)
      error.value = err instanceof Error ? err.message : '活动加载失败'
  }
  finally {
    if (version === requestVersion)
      loading.value = false
  }
}
/** 搜索新关键词时从第一页开始。 */
function search() {
  page.value = 1
  return load()
}
onMounted(load)
onBeforeUnmount(() => {
  // 1. 离开页面时，使仍在途中的请求失效。
  requestVersion++
})
</script>
<template>
  <section class="page-container list-page"><div class="intro"><span class="section-kicker">AGENDA & ATTENDANCE</span><h1>议程与参会</h1><p>浏览大会场次，了解活动时间、地点与报名状态。</p></div>
    <div class="filter-bar surface-card"><el-input v-model="keyword" placeholder="搜索活动名称、主题或会场" clearable @keyup.enter="search" /><el-button type="primary" @click="search">搜索活动</el-button></div>
    <div v-if="loading" class="state">正在加载活动…</div><el-alert v-else-if="error" :title="error" type="error" :closable="false" show-icon /><el-empty v-else-if="activities.length === 0" description="没有找到符合条件的活动" /><div v-else class="activity-grid"><ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" /></div>
    <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @update:current-page="page = $event; load()" />
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
