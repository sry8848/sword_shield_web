<template>
  <div class="admin-materials-page">
    <h2 class="page-title">
      <el-icon class="title-icon"><Document /></el-icon>
      资料管理
    </h2>
    <p class="page-subtitle">上传并发布大会资料（PPT、PDF 等）</p>

    <el-card class="upload-card">
      <h3>
        <el-icon class="heading-icon"><Upload /></el-icon>
        上传资料
      </h3>
      <el-form :model="form" label-width="120px" class="upload-form">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="例如：主论坛演讲PPT" />
        </el-form-item>

        <el-form-item label="主讲人">
          <el-input v-model="form.speaker" placeholder="例如：张三" />
        </el-form-item>

        <el-form-item label="资料文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".ppt,.pptx,.pdf,application/*"
          >
            <el-button type="primary" :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PPT / PPTX / PDF 等格式</div>
            </template>
          </el-upload>
          <div v-if="selectedFile" class="file-info">
            <el-icon class="status-icon"><CircleCheck /></el-icon>
            已选择: {{ selectedFile.name }}
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitMaterial" :loading="uploading" size="large">
            <span v-if="!uploading">
              <el-icon class="btn-icon"><Promotion /></el-icon>
              上传并发布
            </span>
            <span v-else>上传中...</span>
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <div class="list-header">
        <h3>
          <el-icon class="heading-icon"><List /></el-icon>
          已发布资料
        </h3>
      </div>
      <el-table :data="materials" style="width: 100%" v-loading="loading" empty-text="暂无已发布资料">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="speaker" label="主讲人" width="160" />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="copyLink(row)">复制下载链接</el-button>
            <el-button size="small" type="danger" @click="deleteMaterial(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="loadMaterials"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import adminApi from '../../adminApi'

const form = reactive({
  title: '',
  speaker: ''
})

const selectedFile = ref<File | null>(null)
const uploadRef = ref()
const uploading = ref(false)
const loading = ref(false)

const materials = ref<any[]>([])
const total = ref(0)
const pageSize = ref(10)

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

async function submitMaterial() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const uploadRes = await adminApi.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (uploadRes.data.code !== 0) {
      ElMessage.error(uploadRes.data.message || '文件上传失败')
      return
    }

    const uploadData = uploadRes.data.data
    const payload = {
      title: form.title,
      speaker: form.speaker,
      objectName: uploadData.objectName,
      contentType: uploadData.contentType,
      size: uploadData.size
    }

    const { data } = await adminApi.post('/content/material', payload)

    if (data.code === 0) {
      ElMessage.success('资料发布成功')
      resetForm()
      loadMaterials(1)
    } else {
      ElMessage.error(data.message || '资料发布失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.speaker = ''
  selectedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

async function loadMaterials(page = 1) {
  loading.value = true
  try {
    const { data } = await adminApi.get('/content/materials', {
      params: { page: page - 1, size: pageSize.value }
    })
    if (data.code === 0) {
      materials.value = data.data.items
      total.value = data.data.total
    }
  } catch {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  loadMaterials(1)
}

async function copyLink(row: any) {
  try {
    const pres = await adminApi.get('/file/presigned', {
      params: { objectName: row.objectName, expireSeconds: 600 }
    })
    await navigator.clipboard.writeText(pres.data.data)
    ElMessage.success('下载链接已复制')
  } catch {
    ElMessage.error('获取链接失败')
  }
}

async function deleteMaterial(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除"${row.title}"吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const { data } = await adminApi.delete(`/content/material/${row.id}`)
    if (data.code === 0) {
      ElMessage.success('资料删除成功')
      loadMaterials(1)
    } else {
      ElMessage.error(data.message || '资料删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '资料删除失败')
    }
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatSize(size: number) {
  if (size === null || size === undefined) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

onMounted(() => {
  loadMaterials(1)
})
</script>

<style scoped>
.admin-materials-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #409eff;
}

.page-subtitle {
  color: #666;
  margin-bottom: 24px;
  font-size: 14px;
}

.upload-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.upload-card h3 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-form {
  max-width: 800px;
}

.file-info {
  margin-top: 8px;
  color: #67c23a;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
}

.list-card {
  border-radius: 12px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list-header h3 {
  font-size: 20px;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.heading-icon {
  color: #409eff;
}

.status-icon {
  margin-right: 6px;
}

.btn-icon {
  margin-right: 6px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
