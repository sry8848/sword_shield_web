<template>
  <div class="page">
    <h2 class="title">🏆 成果展示</h2>
    <el-row :gutter="16">
      <el-col :span="8" v-for="a in list" :key="a.id">
        <el-card class="card">
          <h3 class="name">{{ a.title }}</h3>
          <p class="desc">{{ a.description }}</p>
          <div class="actions">
            <a v-if="a.fileUrl" :href="a.fileUrl" target="_blank">查看文件</a>
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

const list = ref<any[]>([])
const total = ref(0)
const size = ref(12)

async function load(page = 1) {
  try {
    const { data } = await api.get('/content/achievements', { params: { page: page - 1, size: size.value } })
    if (data.code === 0) { list.value = data.data.items; total.value = data.data.total }
    else ElMessage.error(data.message || '加载失败')
  } catch { ElMessage.error('加载失败，请稍后重试') }
}

onMounted(() => load(1))
</script>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.title { margin-bottom: 16px; }
.card { border-radius: 16px; margin-bottom: 16px; }
.name { margin: 8px 0; }
.desc { color: #444; margin: 6px 0; min-height: 48px; }
.actions { margin-top: 8px; }
.pager { display: flex; justify-content: center; margin-top: 12px; }
</style>
