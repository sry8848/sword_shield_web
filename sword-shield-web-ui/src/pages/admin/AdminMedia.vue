<template>
  <div class="admin-media-page">
    <h2 class="page-title">
      <el-icon class="title-icon"><Camera /></el-icon>
      媒体资产管理
    </h2>
    <p class="page-subtitle">管理大会精彩花絮、图片和视频内容</p>

    <!-- Upload Form -->
    <el-card class="upload-card">
      <h3>
        <el-icon class="heading-icon"><Upload /></el-icon>
        上传新媒体
      </h3>
      <el-form :model="form" label-width="120px" class="upload-form">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="例如：开幕式精彩瞬间" />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input 
            v-model="form.summary" 
            type="textarea" 
            :rows="3" 
            placeholder="简要描述这个媒体内容" 
          />
        </el-form-item>
        
        <el-form-item label="媒体类型" required>
          <el-radio-group v-model="form.type">
            <el-radio label="image">
              <el-icon class="inline-icon"><Picture /></el-icon>
              图片
            </el-radio>
            <el-radio label="video">
              <el-icon class="inline-icon"><VideoCamera /></el-icon>
              视频
            </el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="上传文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :accept="form.type === 'image' ? 'image/*' : 'video/*'"
          >
            <el-button type="primary" :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                {{ form.type === 'image' ? '支持 JPG、PNG、GIF 格式' : '支持 MP4、AVI、MOV 格式' }}
              </div>
            </template>
          </el-upload>
          <div v-if="selectedFile" class="file-info">
            <el-icon class="status-icon"><CircleCheck /></el-icon>
            已选择: {{ selectedFile.name }}
          </div>
        </el-form-item>
        
        <el-form-item label="封面图URL">
          <el-input 
            v-model="form.coverUrl" 
            placeholder="视频封面图URL（可选）" 
          />
        </el-form-item>
        
        <el-form-item label="关联资讯">
          <el-select 
            v-model="form.articleId" 
            placeholder="选择要关联的资讯（可选）"
            clearable
            filterable
          >
            <el-option
              v-for="article in articles"
              :key="article.id"
              :label="article.title"
              :value="article.id"
            />
          </el-select>
          <div class="form-tip">关联后，媒体将显示在该资讯详情页</div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="submitMedia" 
            :loading="uploading"
            size="large"
          >
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

    <!-- Media List -->
    <el-card class="list-card">
      <div class="list-header">
        <h3>
          <el-icon class="heading-icon"><List /></el-icon>
          已发布媒体列表
        </h3>
        <el-radio-group v-model="filterType" @change="loadMedia(1)" size="small">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="image">图片</el-radio-button>
          <el-radio-button label="video">视频</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="mediaList" style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'image' ? 'success' : 'primary'">
              <template v-if="row.type === 'image'">
                <el-icon class="tag-icon"><Picture /></el-icon>
                图片
              </template>
              <template v-else>
                <el-icon class="tag-icon"><VideoCamera /></el-icon>
                视频
              </template>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="previewMedia(row)" :icon="View">
              预览
            </el-button>
            <el-button size="small" type="danger" @click="deleteMedia(row)" :icon="Delete">
              删除
            </el-button>
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
          @current-change="loadMedia"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- Preview Dialog -->
    <el-dialog 
      v-model="previewVisible" 
      :title="currentMedia?.title" 
      width="70%"
      center
    >
      <div class="preview-content">
        <img 
          v-if="currentMedia?.type === 'image'" 
          :src="previewUrl" 
          alt="预览图片"
          class="preview-image"
        />
        <video 
          v-else 
          :src="previewUrl" 
          controls 
          autoplay
          class="preview-video"
        ></video>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, CircleCheck, Delete, List, Picture, Promotion, Upload, VideoCamera, View } from '@element-plus/icons-vue'
import adminApi from '../../adminApi'

const form = reactive({
  title: '',
  summary: '',
  type: 'image',
  coverUrl: '',
  articleId: null as number | null
})

const selectedFile = ref<File | null>(null)
const uploadRef = ref()
const uploading = ref(false)
const loading = ref(false)

const mediaList = ref<any[]>([])
const articles = ref<any[]>([])
const total = ref(0)
const pageSize = ref(10)
const filterType = ref('')

const previewVisible = ref(false)
const previewUrl = ref('')
const currentMedia = ref<any>(null)

let uploadedObjectName = ''

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

async function submitMedia() {
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
    // Step 1: Upload file
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    
    const uploadRes = await adminApi.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (uploadRes.data.code !== 0) {
      ElMessage.error(uploadRes.data.message || '文件上传失败')
      return
    }
    
    uploadedObjectName = uploadRes.data.data.objectName
    
    // Step 2: Create media record
    const mediaData = {
      title: form.title,
      summary: form.summary,
      type: form.type,
      objectName: uploadedObjectName,
      coverUrl: form.coverUrl,
      articleId: form.articleId
    }
    
    const { data } = await adminApi.post('/content/media', mediaData)
    
    if (data.code === 0) {
      ElMessage.success('媒体发布成功！')
      resetForm()
      loadMedia(1)
    } else {
      ElMessage.error(data.message || '发布失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.summary = ''
  form.type = 'image'
  form.coverUrl = ''
  form.articleId = null
  selectedFile.value = null
  uploadedObjectName = ''
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

async function loadMedia(page = 1) {
  loading.value = true
  try {
    const params: any = {
      page: page - 1,
      size: pageSize.value
    }
    
    if (filterType.value) {
      params.type = filterType.value
    }
    
    const { data } = await adminApi.get('/content/media', { params })
    
    if (data.code === 0) {
      mediaList.value = data.data.items
      total.value = data.data.total
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadArticles() {
  try {
    const { data } = await adminApi.get('/content/articles', {
      params: { page: 0, size: 100 }
    })
    if (data.code === 0) {
      articles.value = data.data.items
    }
  } catch (error) {
    console.error('Failed to load articles', error)
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  loadMedia(1)
}

async function previewMedia(row: any) {
  try {
    const { data } = await adminApi.get('/file/presigned', {
      params: {
        objectName: row.objectName,
        expireSeconds: 600
      }
    })
    
    if (data.code === 0) {
      previewUrl.value = data.data
      currentMedia.value = row
      previewVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取预览链接失败')
  }
}

async function deleteMedia(row: any) {
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

    const { data } = await adminApi.delete(`/content/media/${row.id}`)

    if (data.code === 0) {
      ElMessage.success('媒体删除成功')
      loadMedia(1)
    } else {
      ElMessage.error(data.message || '媒体删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '媒体删除失败')
    }
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadMedia(1)
  loadArticles()
})
</script>

<style scoped>
.admin-media-page {
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
  color: #67C23A;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
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

.inline-icon {
  margin-right: 6px;
}

.status-icon {
  margin-right: 6px;
}

.btn-icon {
  margin-right: 6px;
}

.tag-icon {
  margin-right: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.preview-content {
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-video {
  width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}
</style>
