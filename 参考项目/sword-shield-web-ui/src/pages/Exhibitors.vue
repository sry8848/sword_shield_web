<template>
  <div class="page">
    <h2 class="title">🏢 展商展览</h2>
    <el-row :gutter="16">
      <el-col :span="6" v-for="e in exhibitors" :key="e.id">
        <el-card class="card">
          <div class="logo-wrapper">
            <img v-if="e.logoUrl" :src="e.logoUrl" class="logo" @error="handleImageError" />
            <div v-else class="logo-placeholder">🏢</div>
          </div>
          <h3 class="name">{{ e.name }}</h3>
          <p class="brand" v-if="e.brand">品牌：{{ e.brand }}</p>
          <p class="desc">{{ e.introduction }}</p>
          <div class="action-links">
            <el-button v-if="e.website" size="small" @click="openLink(e.website)">
              <span class="icon">🌐</span> 官网
            </el-button>
            <el-button v-if="e.videoUrl" type="primary" size="small" @click="openLink(e.videoUrl)">
              <span class="icon">🎥</span> 视频介绍
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size" @current-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const exhibitors = ref<any[]>([])
const total = ref(0)
const size = ref(12)

async function load(page = 1) {
  try {
    const { data } = await api.get('/content/exhibitors', { params: { page: page - 1, size: size.value } })
    if (data.code === 0) {
      exhibitors.value = data.data.items
      total.value = data.data.total
    } else ElMessage.error(data.message || '加载失败')
  } catch { ElMessage.error('加载失败，请稍后重试') }
}

function openLink(url: string) {
  window.open(url, '_blank')
}

function handleImageError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

onMounted(() => load(1))
</script>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.title { margin-bottom: 24px; font-size: 28px; }
.card { border-radius: 16px; margin-bottom: 16px; transition: transform 0.3s, box-shadow 0.3s; }
.card:hover { transform: translateY(-4px); box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1); }
.logo-wrapper { width: 100%; height: 140px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 8px; margin-bottom: 12px; }
.logo { max-width: 100%; max-height: 140px; object-fit: contain; }
.logo-placeholder { font-size: 48px; opacity: 0.3; }
.name { margin: 12px 0 8px; font-size: 18px; font-weight: 600; color: #333; }
.brand { color: #666; margin: 4px 0; font-size: 14px; }
.desc { color: #666; margin: 8px 0; min-height: 60px; font-size: 14px; line-height: 1.6; }
.action-links { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.action-links .el-button { flex: 1; min-width: 100px; }
.icon { margin-right: 4px; }
.pager { display: flex; justify-content: center; margin-top: 24px; }
</style>
