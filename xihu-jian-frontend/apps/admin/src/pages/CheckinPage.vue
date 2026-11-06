<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import type { Registration } from '@xhlj/shared'
const ticketCode = ref('XHLJ-2026-0001'); const loading = ref(false); const result = ref<Registration | null>(null)
async function submit() {
  if (!ticketCode.value.trim()) { ElMessage.warning('请输入入场凭证'); return }
  loading.value = true; result.value = null
  try { result.value = await api.admin.checkin({ ticketCode: ticketCode.value.trim() }); ElMessage.success('核验成功') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '核验失败') }
  finally { loading.value = false }
}
</script>
<template><section class="admin-page"><h1 class="page-title">现场核验</h1><p class="page-description">输入参会凭证编号完成核验。生产环境需接入扫码设备和后端核验规则。</p>
  <el-card shadow="never" class="check-card"><h3>凭证核验</h3><div class="check-form"><el-input v-model="ticketCode" size="large" placeholder="请输入或扫描凭证编号" @keyup.enter="submit" /><el-button type="primary" size="large" :loading="loading" @click="submit">确认核验</el-button></div><div class="hint">Mock 示例凭证：XHLJ-2026-0001（第二次核验会提示重复核验）</div>
    <div v-if="result" class="result"><el-result icon="success" title="核验成功" sub-title="该凭证已完成现场核验"><template #extra><div class="result-detail"><strong>{{ result.attendeeName }}</strong><span>{{ result.activityTitle }}</span><span>{{ result.ticketCode }}</span></div></template></el-result></div>
  </el-card></section></template>
<style scoped>
.check-card { max-width: 760px; } .check-card h3 { margin: 0 0 20px; } .check-form { display: flex; gap: 12px; } .hint { color: #8a96a7; font-size: 12px; margin-top: 14px; }
.result { margin-top: 22px; border-top: 1px solid #edf0f5; } .result-detail { display: grid; gap: 8px; color: #66758c; font-size: 13px; } .result-detail strong { color: #1f2d3d; font-size: 16px; }
@media (max-width:600px) { .check-form { flex-direction: column; } }
</style>
