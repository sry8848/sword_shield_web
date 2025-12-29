<template>
  <div class="submit-container">
    <el-card class="submit-card">
      <div class="submit-back">
        <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
      </div>
      <h2 class="submit-title">
        <el-icon class="title-icon"><EditPen /></el-icon>
        投稿 · 大会相关内容
      </h2>
      <p class="submit-desc">分享你的大会见闻、心得与精彩瞬间，优秀内容将有机会在平台展示。</p>
      <el-form :model="form" label-width="100px" class="submit-form">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="摘要" required>
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="简要描述你的内容" />
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="form.body" type="textarea" :rows="8" placeholder="请输入正文内容" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-input v-model="form.coverUrl" placeholder="图片URL（可选）" />
            <input type="file" accept="image/*" @change="onFile" hidden ref="fileRef" />
            <el-button @click="triggerFile">上传图片</el-button>
          </div>
          <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-preview" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="submitting" class="submit-btn">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const form = ref({ title: '', summary: '', body: '', coverUrl: '' })
const submitting = ref(false)
const fileRef = ref<HTMLInputElement | null>(null)
const router = useRouter()

function triggerFile() { fileRef.value?.click() }

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/submissions')
}

async function onFile(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) return
  const file = files[0]
  if (file.size > 5 * 1024 * 1024) return ElMessage.warning('图片大小不能超过5MB')
  const fd = new FormData()
  fd.append('file', file)
  try {
    const { data } = await api.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (data.code === 0) {
      const pres = await api.get('/file/presigned', { params: { objectName: data.data.objectName } })
      form.value.coverUrl = pres.data.data
      ElMessage.success('上传成功')
    }
  } catch { ElMessage.error('上传失败，请稍后重试') }
}

async function submit() {
  if (!form.value.title.trim() || !form.value.summary.trim() || !form.value.body.trim()) {
    return ElMessage.warning('请完整填写标题、摘要和正文')
  }
  submitting.value = true
  try {
    const { data } = await api.post('/content/article/submit', { ...form.value, category: 'UGC' })
    if (data.code === 0) {
      ElMessage.success('提交成功，待审核')
      form.value = { title: '', summary: '', body: '', coverUrl: '' }
    } else {
      ElMessage.error(data.message || '提交失败')
    }
  } catch { ElMessage.error('提交失败，请稍后重试') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.submit-container { max-width: 900px; margin: 0 auto; }
.submit-card { border-radius: 16px; box-shadow: 0 8px 32px rgba(0,0,0,0.08); }
.submit-back { margin-bottom: 6px; }
.submit-title { margin: 0 0 8px; font-size: 24px; display: flex; align-items: center; gap: 8px; }
.title-icon { color: #2563eb; }
.submit-desc { margin: 0 0 16px; color: #666; }
.cover-upload { display: flex; gap: 12px; align-items: center; }
.cover-preview { margin-top: 12px; max-width: 100%; border-radius: 8px; }
.submit-btn { width: 100%; background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%); border: none; }
</style>
