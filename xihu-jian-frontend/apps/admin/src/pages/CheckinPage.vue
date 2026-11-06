<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import type { CheckinRecord } from '@xhlj/shared'
const mockMode = import.meta.env.VITE_USE_MOCK === 'true'
const eventId = ref(mockMode ? 'ACT-1003' : '')
const credentialCode = ref(mockMode ? 'XHLJ-2026-0001' : '')
const loading = ref(false)
const result = ref<CheckinRecord | null>(null)
/** 按场次和手输凭证编号执行现场核验。 */
async function submit() {
  if (loading.value) return
  if (!eventId.value.trim() || !credentialCode.value.trim()) { ElMessage.warning('请输入场次 ID 和入场凭证'); return }
  loading.value = true; result.value = null
  try { result.value = await api.admin.checkin({ eventId: eventId.value.trim(), credentialCode: credentialCode.value.trim(), inputMode: 'MANUAL_INPUT' }); ElMessage.success('核验成功') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '核验失败') }
  finally { loading.value = false }
}
</script>
<template><section class="admin-page"><h1 class="page-title">现场核验</h1><p class="page-description">输入参会凭证编号完成核验。生产环境需接入扫码设备和后端核验规则。</p>
  <el-card shadow="never" class="check-card"><h3>凭证核验</h3><div class="check-form"><el-input v-model="eventId" size="large" aria-label="场次 ID" placeholder="场次 ID" /><el-input v-model="credentialCode" size="large" aria-label="入场凭证" placeholder="请输入凭证编号" @keyup.enter="submit" /><el-button type="primary" size="large" :loading="loading" @click="submit">确认核验</el-button></div><div v-if="mockMode" class="hint">Mock 示例：ACT-1003 / XHLJ-2026-0001（第二次核验会提示重复核验）</div>
    <div v-if="result" class="result"><el-result icon="success" title="核验成功" sub-title="该凭证已完成现场核验"><template #extra><div class="result-detail"><strong>{{ result.attendeeName }}</strong><span>{{ result.eventTitle ?? result.eventId }}</span><span>{{ result.credentialNo }}</span></div></template></el-result></div>
  </el-card></section></template>
<style scoped>
.check-card { max-width: 760px; } .check-card h3 { margin: 0 0 20px; } .check-form { display: flex; gap: 12px; } .hint { color: #8a96a7; font-size: 12px; margin-top: 14px; }
.result { margin-top: 22px; border-top: 1px solid #edf0f5; } .result-detail { display: grid; gap: 8px; color: #66758c; font-size: 13px; } .result-detail strong { color: #1f2d3d; font-size: 16px; }
@media (max-width:600px) { .check-form { flex-direction: column; } }
</style>
