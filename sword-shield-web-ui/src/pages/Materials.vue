<template>
  <div class="page">
    <h2 class="title">📄 资料下载（嘉宾PPT）</h2>
    <el-card class="card">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="主讲人">
          <el-input v-model="form.speaker" placeholder="请输入主讲人" />
        </el-form-item>
        <el-form-item label="PPT文件" required>
          <input type="file" accept="application/*,.ppt,.pptx,.pdf" @change="onFile" ref="fileRef" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="uploading">上传并保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <h3>已发布资料</h3>
      <el-table :data="materials" style="width:100%">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="speaker" label="主讲人" />
        <el-table-column prop="size" label="大小(B)" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="copyLink(row)">复制下载链接</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="size" @current-change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const form = ref({ title: '', speaker: '' })
const fileRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
let lastUpload: { objectName: string, contentType: string, size: number } | null = null

const materials = ref<any[]>([])
const total = ref(0)
const size = ref(10)

async function onFile(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) return
  const file = files[0]
  const fd = new FormData()
  fd.append('file', file)
  try {
    uploading.value = true
    const { data } = await api.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (data.code === 0) {
      lastUpload = { objectName: data.data.objectName, contentType: data.data.contentType, size: data.data.size }
      ElMessage.success('上传成功，点击“上传并保存”完成保存')
    } else ElMessage.error(data.message || '上传失败')
  } catch { ElMessage.error('上传失败，请稍后重试') } finally { uploading.value = false }
}

async function submit() {
  if (!form.value.title.trim()) return ElMessage.warning('请填写标题')
  if (!lastUpload) return ElMessage.warning('请先上传文件')
  try {
    const payload = {
      title: form.value.title,
      speaker: form.value.speaker,
      objectName: lastUpload.objectName,
      contentType: lastUpload.contentType,
      size: lastUpload.size,
    }
    console.log('发送资料保存请求:', payload)
    const { data } = await api.post('/content/material', payload)
    console.log('资料保存响应:', data)
    if (data.code === 0) {
      ElMessage.success('保存成功')
      form.value = { title: '', speaker: '' }
      lastUpload = null
      if (fileRef.value) fileRef.value.value = ''
      load(1)
    } else ElMessage.error(data.message || '保存失败')
  } catch (err: any) {
    console.error('保存资料失败:', err)
    const msg = err.response?.data?.message || err.message || '保存失败，请稍后重试'
    ElMessage.error(msg)
  }
}

async function load(page = 1) {
  try {
    const { data } = await api.get('/content/materials', { params: { page: page - 1, size: size.value } })
    if (data.code === 0) { materials.value = data.data.items; total.value = data.data.total }
  } catch { ElMessage.error('加载失败，请稍后重试') }
}

async function copyLink(row: any) {
  try {
    const pres = await api.get('/file/presigned', { params: { objectName: row.objectName, expireSeconds: 300 } })
    await navigator.clipboard.writeText(pres.data.data)
    ElMessage.success('下载链接已复制')
  } catch { ElMessage.error('获取链接失败') }
}

onMounted(() => { load(1) })
</script>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
.card { border-radius: 16px; margin-bottom: 16px; }
.list-card { border-radius: 16px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>
