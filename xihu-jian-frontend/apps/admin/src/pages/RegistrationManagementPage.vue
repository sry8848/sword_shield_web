<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import { formatDateTime, type Registration, type RegistrationDetail } from '@xhlj/shared'
const rows = ref<Registration[]>([]); const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const detail = ref<RegistrationDetail | null>(null)
const detailVisible = ref(false)

/** 加载当前页的报名摘要。 */
async function load() {
  loading.value = true
  try {
    const result = await api.admin.registrations({ page: page.value, pageSize })
    rows.value = result.items
    total.value = result.pageInfo.total
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '报名列表加载失败') }
  finally { loading.value = false }
}

/** 单独查询参会人资料和凭证信息。 */
async function showDetail(registrationId: string) {
  try {
    detail.value = await api.admin.registration(registrationId)
    detailVisible.value = true
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '报名详情加载失败') }
}
onMounted(load)
</script>
<template><section class="admin-page"><h1 class="page-title">报名管理</h1><p class="page-description">查看参会人员报名记录和当前状态。</p><el-card shadow="never"><el-table v-loading="loading" :data="rows" empty-text="暂无报名记录"><el-table-column prop="id" label="记录编号" min-width="130" /><el-table-column prop="eventTitle" label="活动名称" min-width="220" /><el-table-column prop="registeredAt" label="报名时间" min-width="180"><template #default="{ row }">{{ formatDateTime(row.registeredAt) }}</template></el-table-column><el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'CHECKED_IN' ? 'success' : row.status === 'REGISTERED' ? 'primary' : 'info'">{{ row.status === 'CHECKED_IN' ? '已核验' : row.status === 'REGISTERED' ? '待入场' : '已取消' }}</el-tag></template></el-table-column><el-table-column label="操作" width="110"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row.id)">查看详情</el-button></template></el-table-column>
  </el-table><el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" /></el-card>
  <el-dialog v-model="detailVisible" title="报名详情"><dl v-if="detail"><dt>参会人</dt><dd>{{ detail.attendeeName ?? '—' }}</dd><dt>入场凭证</dt><dd>{{ detail.credential?.credentialNo ?? '—' }}</dd></dl></el-dialog>
</section></template>
