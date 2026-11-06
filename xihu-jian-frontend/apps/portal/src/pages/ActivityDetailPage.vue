<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { formatDateTime, type Activity } from '@xhlj/shared'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'
const route = useRoute(); const router = useRouter(); const auth = useAuthStore()
const activity = ref<Activity | null>(null); const loading = ref(false); const submitting = ref(false)
async function load() {
  loading.value = true
  try { activity.value = await api.activities.detail(String(route.params.activityId)) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '活动加载失败') }
  finally { loading.value = false }
}
async function register() {
  if (!auth.isAuthenticated) { router.push({ name: 'Login', query: { redirect: route.fullPath } }); return }
  submitting.value = true
  try { await api.activities.register(String(route.params.activityId)); ElMessage.success('报名成功，可在“我的报名”中查看凭证'); router.push('/me/registrations') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '报名失败') }
  finally { submitting.value = false }
}
onMounted(load)
</script>
<template>
  <section class="page-container detail-page"><div v-if="loading" class="state">正在加载活动详情…</div><el-empty v-else-if="!activity" description="活动不存在或暂不可访问" /><template v-else>
    <RouterLink class="back-link" to="/activities">← 返回全部场次</RouterLink>
    <article class="detail-card surface-card"><div class="detail-main"><el-tag :type="activity.registrationStatus === 'OPEN' ? 'success' : 'info'">{{ activity.registrationStatus === 'OPEN' ? '报名开放中' : activity.registrationStatus === 'FULL' ? '名额已满' : '暂不可报名' }}</el-tag><h1>{{ activity.title }}</h1><p class="summary">{{ activity.summary }}</p>
      <div class="facts"><div><span>活动时间</span><strong>{{ formatDateTime(activity.startAt) }}</strong></div><div><span>活动地点</span><strong>{{ activity.venue }}</strong></div><div><span>主讲嘉宾</span><strong>{{ activity.speaker || '大会嘉宾' }}</strong></div><div><span>报名情况</span><strong>{{ activity.registeredCount }} / {{ activity.capacity }}</strong></div></div>
    </div><aside class="register-panel"><span class="panel-caption">REGISTRATION</span><h3>参与本场活动</h3><p>提交报名后，可在个人中心查看报名状态和入场凭证。</p><el-button type="primary" size="large" :loading="submitting" :disabled="activity.registrationStatus !== 'OPEN'" @click="register">{{ activity.registrationStatus === 'OPEN' ? '立即报名' : '暂不可报名' }}</el-button><RouterLink to="/me/registrations" class="my-link">查看我的报名</RouterLink></aside></article>
  </template></section>
</template>
<style scoped>
.detail-page { min-height: 65vh; padding-top: 38px; } .back-link { display: inline-block; color: #155eef; margin-bottom: 18px; font-size: 13px; }
.detail-card { display: grid; grid-template-columns: 1fr 290px; gap: 36px; padding: 36px; } h1 { margin: 18px 0 12px; font-size: 30px; }
.summary { color: #66758c; line-height: 1.8; } .facts { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; margin-top: 32px; }
.facts div { display: grid; gap: 8px; } .facts span { color: #8290a5; font-size: 12px; } .facts strong { font-size: 14px; font-weight: 600; }
.register-panel { align-self: start; padding: 24px; border-radius: 14px; background: #f2f7ff; display: grid; gap: 14px; } .panel-caption { color: #4388df; font-size: 10px; letter-spacing: 2px; font-weight: 700; }
.register-panel h3 { margin: 0; } .register-panel p { color: #66758c; font-size: 13px; line-height: 1.7; margin: 0; } .my-link { text-align: center; color: #155eef; font-size: 13px; }
.state { padding: 80px; text-align: center; color: #8290a5; }
@media (max-width:760px) { .detail-card { grid-template-columns: 1fr; padding: 22px; } .facts { grid-template-columns: 1fr; } }
</style>
