<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="title">
          <el-icon class="title-icon"><Document /></el-icon>
          资料下载（嘉宾PPT）
        </h2>
        <p class="subtitle">仅展示已发布资料</p>
      </div>
    </div>
    <el-card class="list-card">
      <h3>已发布资料</h3>
      <el-table :data="materials" style="width:100%" empty-text="暂无已发布资料">
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

const materials = ref<any[]>([])
const total = ref(0)
const size = ref(10)

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
.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.title { margin: 0 0 6px; display: flex; align-items: center; gap: 8px; }
.title-icon { color: #409eff; }
.subtitle { margin: 0; color: #6b7280; font-size: 14px; }
.list-card { border-radius: 16px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>
