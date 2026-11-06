<script setup lang="ts">
import type { Activity } from '@xhlj/shared'
import { formatDateTime } from '@xhlj/shared'
defineProps<{ activity: Activity }>()
</script>
<template>
  <RouterLink class="activity-card surface-card" :to="`/activities/${activity.id}`">
    <div class="card-top"><span class="eyebrow">大会活动</span><el-tag :type="activity.registrationStatus === 'OPEN' ? 'success' : 'info'" effect="light">{{ activity.registrationStatus === 'OPEN' ? '报名开放中' : activity.registrationStatus === 'FULL' ? '名额已满' : '暂未开放' }}</el-tag></div>
    <h3>{{ activity.title }}</h3><p class="summary">{{ activity.summary }}</p>
    <div class="meta"><span>{{ formatDateTime(activity.startAt) }}</span><span>{{ activity.venue }}</span></div>
    <div class="card-bottom"><span>已报名 {{ activity.registeredCount }} / {{ activity.capacity }}</span><span class="detail-link">查看详情 →</span></div>
  </RouterLink>
</template>
<style scoped>
.activity-card { display: flex; flex-direction: column; min-height: 245px; padding: 22px; transition: transform .2s,box-shadow .2s; }
.activity-card:hover { transform: translateY(-3px); box-shadow: 0 16px 36px rgba(20,50,90,.11); }
.card-top,.card-bottom { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { color: #155eef; font-size: 12px; font-weight: 700; letter-spacing: 1px; }
h3 { margin: 18px 0 8px; font-size: 18px; color: #172b4d; }
.summary { color: #66758c; font-size: 13px; line-height: 1.7; margin: 0 0 18px; flex: 1; }
.meta { display: grid; gap: 7px; color: #7b8798; font-size: 12px; }
.card-bottom { border-top: 1px solid #edf0f5; padding-top: 14px; margin-top: 18px; color: #7b8798; font-size: 12px; }
.detail-link { color: #155eef; font-weight: 600; }
</style>
