<template>
  <div class="activity-management">
    <div class="page-header">
      <h3>活动列表</h3>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新建活动
      </el-button>
    </div>
    
    <el-table :data="activities" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="活动标题" min-width="200" />
      <el-table-column prop="speaker" label="演讲者" width="120" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="报名/上限" width="100">
        <template #default="{ row }">
          {{ row.currentRegistrations || 0 }} / {{ row.maxRegistrations || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="heat" label="热度" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '已下线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">
            编辑
          </el-button>
          <el-button link type="danger" @click="deleteActivity(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑活动' : '新建活动'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="120px">
        <el-form-item label="活动标题" required>
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>
        
        <el-form-item label="演讲者">
          <el-input v-model="form.speaker" placeholder="请输入演讲者" />
        </el-form-item>
        
        <el-form-item label="活动描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述"
          />
        </el-form-item>
        
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="报名截止时间">
          <el-date-picker
            v-model="form.registrationDeadline"
            type="datetime"
            placeholder="选择报名截止时间"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="最大报名人数">
          <el-input-number v-model="form.maxRegistrations" :min="0" />
        </el-form-item>
        
        <el-form-item label="活动地点">
          <el-input v-model="form.location" placeholder="请输入活动地点" />
        </el-form-item>
        
        <el-form-item label="直播链接">
          <el-input v-model="form.liveUrl" placeholder="请输入直播链接" />
        </el-form-item>
        
        <el-form-item label="回放链接">
          <el-input v-model="form.replayUrl" placeholder="请输入回放链接" />
        </el-form-item>
        
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="请输入封面图URL" />
        </el-form-item>
        
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">发布</el-radio>
            <el-radio :label="0">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import adminApi from '../../adminApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

interface Activity {
  id?: number
  title: string
  speaker?: string
  description?: string
  startTime: string
  endTime: string
  registrationDeadline?: string
  maxRegistrations?: number
  location?: string
  liveUrl?: string
  replayUrl?: string
  coverImage?: string
  status: number
  currentRegistrations?: number
  heat?: number
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const activities = ref<Activity[]>([])

const form = ref<Activity>({
  title: '',
  speaker: '',
  description: '',
  startTime: '',
  endTime: '',
  registrationDeadline: '',
  maxRegistrations: 100,
  location: '',
  liveUrl: '',
  replayUrl: '',
  coverImage: '',
  status: 1
})

function getAuthHeaders() {
  const token = localStorage.getItem('adminToken')
  return { Authorization: `Bearer ${token}` }
}

async function loadActivities() {
  loading.value = true
  try {
    const { data } = await adminApi.get('/admin/activities')
    if (data.code === 0) {
      activities.value = data.data
    } else {
      ElMessage.error(data.message || '加载活动列表失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载活动列表失败')
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  isEdit.value = false
  form.value = {
    title: '',
    speaker: '',
    description: '',
    startTime: '',
    endTime: '',
    registrationDeadline: '',
    maxRegistrations: 100,
    location: '',
    liveUrl: '',
    replayUrl: '',
    coverImage: '',
    status: 1
  }
  dialogVisible.value = true
}

function showEditDialog(activity: Activity) {
  isEdit.value = true
  form.value = {
    ...activity,
    startTime: activity.startTime,
    endTime: activity.endTime,
    registrationDeadline: activity.registrationDeadline || ''
  }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.title) {
    ElMessage.warning('请输入活动标题')
    return
  }
  if (!form.value.startTime || !form.value.endTime) {
    ElMessage.warning('请选择开始时间和结束时间')
    return
  }
  
  submitting.value = true
  try {
    const payload = {
      ...form.value,
      startTime: new Date(form.value.startTime).toISOString(),
      endTime: new Date(form.value.endTime).toISOString(),
      registrationDeadline: form.value.registrationDeadline 
        ? new Date(form.value.registrationDeadline).toISOString() 
        : null
    }
    
    if (isEdit.value) {
      const { data } = await adminApi.put(`/admin/activities/${form.value.id}`, payload)
      if (data.code === 0) {
        ElMessage.success('活动更新成功')
        dialogVisible.value = false
        loadActivities()
      } else {
        ElMessage.error(data.message || '活动更新失败')
      }
    } else {
      const { data } = await adminApi.post('/admin/activities', payload)
      if (data.code === 0) {
        ElMessage.success('活动创建成功')
        dialogVisible.value = false
        loadActivities()
      } else {
        ElMessage.error(data.message || '活动创建失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function deleteActivity(activity: Activity) {
  try {
    await ElMessageBox.confirm(`确定要删除活动"${activity.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const { data } = await adminApi.delete(`/admin/activities/${activity.id}`)
    
    if (data.code === 0) {
      ElMessage.success('活动删除成功')
      loadActivities()
    } else {
      ElMessage.error(data.message || '活动删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '活动删除失败')
    }
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.activity-management {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

:deep(.el-table) {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(148, 163, 184, 0.45);
}
</style>
