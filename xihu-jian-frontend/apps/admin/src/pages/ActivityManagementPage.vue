<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { formatDateTime, type Activity, type ActivityCreateRequest } from '@xhlj/shared'
const auth = useAuthStore()
const page = ref(1)
const pageSize = 10
const total = ref(0)
const rows = ref<Activity[]>([]); const loading = ref(false); const dialogVisible = ref(false); const saving = ref(false)
const form = reactive<ActivityCreateRequest>({ title: '', summary: '', startTime: '', endTime: '', venueName: '', capacity: 100 })
/** 加载后台场次列表的当前页。 */
async function load() {
  loading.value = true
  try {
    const result = await api.admin.activities({ page: page.value, pageSize })
    rows.value = result.items
    total.value = result.pageInfo.total
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '活动列表加载失败') }
  finally { loading.value = false }
}
/** 清空创建表单并打开草稿对话框。 */
function openCreate() { Object.assign(form, { title: '', summary: '', startTime: '', endTime: '', venueName: '', capacity: 100 }); dialogVisible.value = true }
/** 保存场次草稿并重新加载列表。 */
async function create() {
  saving.value = true
  try { await api.admin.createActivity({ ...form }); ElMessage.success('活动已创建为草稿'); dialogVisible.value = false; await load() }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '创建失败') }
  finally { saving.value = false }
}
/** 发布选中的场次并更新列表。 */
async function publish(row: Activity) {
  try { await api.admin.publishActivity(row.id); ElMessage.success('活动已发布'); await load() }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '发布失败') }
}
onMounted(load)
</script>
<template><section class="admin-page"><div class="toolbar"><div><h1 class="page-title">活动管理</h1><p class="page-description">创建、维护和发布大会活动。</p></div><el-button v-if="auth.can('event:create')" type="primary" @click="openCreate">新建活动</el-button></div>
  <el-card shadow="never"><el-table v-loading="loading" :data="rows" row-key="id"><el-table-column prop="title" label="活动名称" min-width="220" /><el-table-column prop="startTime" label="开始时间" min-width="180"><template #default="{ row }">{{ formatDateTime(row.startTime) }}</template></el-table-column><el-table-column label="会场" min-width="180"><template #default="{ row }">{{ row.venue?.name ?? '—' }}</template></el-table-column><el-table-column prop="registeredCount" label="报名人数" width="100" /><el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已发布' : row.status === 'DRAFT' ? '草稿' : row.status === 'FINISHED' ? '已结束' : '已下线' }}</el-tag></template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 'DRAFT' && auth.can('event:publish')" link type="primary" @click="publish(row)">发布</el-button><span v-else class="muted">—</span></template></el-table-column></el-table><el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" /></el-card>
  <el-dialog v-model="dialogVisible" title="新建活动" width="560px"><el-form label-position="top"><el-form-item label="活动名称" required><el-input v-model="form.title" maxlength="80" /></el-form-item><el-form-item label="活动简介"><el-input v-model="form.summary" type="textarea" :rows="3" /></el-form-item><el-form-item label="开始时间" required><el-input v-model="form.startTime" placeholder="ISO 8601 日期时间" /></el-form-item><el-form-item label="结束时间" required><el-input v-model="form.endTime" placeholder="ISO 8601 日期时间" /></el-form-item><el-form-item label="活动地点"><el-input v-model="form.venueName" /></el-form-item><el-form-item label="报名人数上限"><el-input-number v-model="form.capacity" :min="1" :max="100000" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="!form.title.trim() || !form.startTime || !form.endTime" @click="create">保存草稿</el-button></template></el-dialog>
</section></template>
<style scoped>.toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 8px; } .muted { color: #a0a8b5; }</style>
