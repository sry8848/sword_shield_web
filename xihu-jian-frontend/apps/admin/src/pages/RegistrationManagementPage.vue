<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import { formatDateTime, type Registration } from '@xhlj/shared'
const rows = ref<Registration[]>([]); const loading = ref(false)
async function load() { loading.value = true; try { rows.value = await api.admin.registrations() } catch (error) { ElMessage.error(error instanceof Error ? error.message : '报名列表加载失败') } finally { loading.value = false } }
onMounted(load)
</script>
<template><section class="admin-page"><h1 class="page-title">报名管理</h1><p class="page-description">查看参会人员报名记录和当前状态。</p><el-card shadow="never"><el-table v-loading="loading" :data="rows" empty-text="暂无报名记录"><el-table-column prop="id" label="记录编号" min-width="130" /><el-table-column prop="attendeeName" label="参会人" width="120" /><el-table-column prop="activityTitle" label="活动名称" min-width="220" /><el-table-column prop="ticketCode" label="入场凭证" min-width="180" /><el-table-column prop="createdAt" label="报名时间" min-width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column><el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'CHECKED_IN' ? 'success' : row.status === 'REGISTERED' ? 'primary' : 'info'">{{ row.status === 'CHECKED_IN' ? '已核验' : row.status === 'REGISTERED' ? '待入场' : '已取消' }}</el-tag></template></el-table-column></el-table></el-card></section></template>
