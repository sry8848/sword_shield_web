<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/client'
import { formatDateTime, type Registration } from '@xhlj/shared'
const rows = ref<Registration[]>([]); const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

/** 查询当前用户的报名列表。 */
async function load() {
  loading.value = true
  try {
    const result = await api.registrations.mine({ page: page.value, pageSize })
    rows.value = result.items
    total.value = result.pageInfo.total
  }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '报名记录加载失败') }
  finally { loading.value = false }
}
/** 按需查询凭证编号，列表仅返回凭证状态。 */
async function showCredential(registrationId: string) {
  try {
    const { credential } = await api.registrations.credential(registrationId)
    await ElMessageBox.alert(credential.credentialNo, '入场凭证', { confirmButtonText: '关闭', showClose: false })
  }
  catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '凭证加载失败')
    }
  }
}

onMounted(load)
</script>
<template>
  <section class="page-container registrations-page"><div class="heading"><span class="section-kicker">MY CONFERENCE</span><h1>我的报名</h1><p>查看报名记录和现场入场凭证。</p></div>
    <el-card shadow="never"><el-table v-loading="loading" :data="rows" empty-text="暂无报名记录"><el-table-column prop="eventTitle" label="活动名称" min-width="220" /><el-table-column prop="registeredAt" label="报名时间" min-width="180"><template #default="{ row }">{{ formatDateTime(row.registeredAt) }}</template></el-table-column><el-table-column label="入场凭证" min-width="140"><template #default="{ row }"><el-button v-if="row.status !== 'CANCELLED'" link type="primary" @click="showCredential(row.id)">查看凭证</el-button></template></el-table-column><el-table-column prop="status" label="状态" width="130"><template #default="{ row }"><el-tag :type="row.status === 'CHECKED_IN' ? 'success' : row.status === 'REGISTERED' ? 'primary' : 'info'">{{ row.status === 'CHECKED_IN' ? '已核验' : row.status === 'REGISTERED' ? '待入场' : '已取消' }}</el-tag></template></el-table-column></el-table><el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" /></el-card>
  </section>
</template>
<style scoped>
.registrations-page { min-height: 65vh; padding-top: 44px; } .heading { margin-bottom: 24px; } .section-kicker { color: #4388df; font-size: 11px; letter-spacing: 2px; font-weight: 700; }
h1 { margin: 12px 0 8px; font-size: 30px; } .heading p { color: #66758c; margin: 0; }
</style>
