<template>
  <div class="submission-management">
    <div class="page-header">
      <div class="title-group">
        <h3>在线投稿</h3>
        <div class="filters">
          <el-select
            v-model="filters.status"
            placeholder="状态"
            style="width: 120px"
            @change="reload"
          >
            <el-option label="全部" :value="-1" />
            <el-option label="待审核" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
          <el-button @click="reload">刷新</el-button>
        </div>
      </div>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" empty-text="暂无投稿">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '已发布' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">查看</el-button>
          <el-button
            v-if="row.status !== 1"
            link
            type="success"
            @click="publish(row)"
          >
            通过并发布
          </el-button>
          <el-button link type="danger" @click="deleteItem(row)">删除</el-button>
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
        :current-page="currentPage"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="detailVisible" title="投稿详情" width="820px" destroy-on-close>
      <div v-if="current">
        <div class="detail-title">{{ current.title }}</div>
        <div class="detail-meta">
          <el-tag type="info" effect="plain">UGC</el-tag>
          <el-tag :type="current.status === 1 ? 'success' : 'warning'">
            {{ current.status === 1 ? '已发布' : '待审核' }}
          </el-tag>
          <span class="meta-time">提交：{{ formatDateTime(current.createdAt) }}</span>
        </div>
        <el-divider />
        <div class="detail-summary">
          <div class="label">摘要</div>
          <div class="value">{{ current.summary }}</div>
        </div>
        <el-divider />
        <div class="detail-body">
          <div class="label">正文</div>
          <div class="value">
            <el-input v-model="current.body" type="textarea" :rows="12" readonly />
          </div>
        </div>
        <div v-if="current.coverUrl" class="detail-cover">
          <div class="label">封面</div>
          <img :src="current.coverUrl" alt="cover" class="cover-img" />
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="current && current.status !== 1"
          type="success"
          :loading="publishing"
          @click="publish(current)"
        >
          通过并发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台投稿审核页（UGC）。
 *
 * - 展示用户投稿文章（category=UGC），支持查看详情、通过发布、删除等操作。
 *
 * 依赖接口：
 * - GET `/content/admin/articles?category=UGC`
 * - PUT `/content/admin/articles/:id`（通过并发布）
 * - DELETE `/content/admin/articles/:id`
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import adminApi from '../../adminApi'

type Submission = {
  /** 投稿文章 ID。 */
  id?: number

  /** 标题。 */
  title: string

  /** 摘要/简介。 */
  summary: string

  /** 正文内容。 */
  body: string

  /** 封面图片 URL（可为空）。 */
  coverUrl?: string

  /** 分类（UGC）。 */
  category?: string

  /** 状态：0-草稿，1-发布（以服务端约定为准）。 */
  status: number

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null

  /** 发布时间（发布后才有）。 */
  publishedAt?: string | null
}

const loading = ref(false)
const publishing = ref(false)

const items = ref<Submission[]>([])
const total = ref(0)
const pageSize = ref(10)
const currentPage = ref(1)

const filters = reactive<{ status: number }>({
  status: -1
})

const detailVisible = ref(false)
const current = ref<Submission | null>(null)

function formatDateTime(dateStr: string | null | undefined) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: currentPage.value - 1,
      size: pageSize.value,
      category: 'UGC'
    }
    if (filters.status !== -1) params.status = filters.status

    const { data } = await adminApi.get('/content/admin/articles', { params })
    if (data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
    } else {
      ElMessage.error(data.message || '加载投稿列表失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载投稿列表失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  currentPage.value = 1
  fetchList()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchList()
}

function showDetail(row: Submission) {
  current.value = row
  detailVisible.value = true
}

async function publish(row: Submission) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`通过并发布“${row.title}”？`, '审核确认', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'warning'
    })
    publishing.value = true
    const payload = {
      title: row.title,
      summary: row.summary,
      body: row.body,
      coverUrl: row.coverUrl || null,
      category: 'UGC',
      status: 1
    }
    const { data } = await adminApi.put(`/content/admin/articles/${row.id}`, payload)
    if (data.code === 0) {
      ElMessage.success('已发布')
      detailVisible.value = false
      fetchList()
    } else {
      ElMessage.error(data.message || '发布失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '发布失败')
    }
  } finally {
    publishing.value = false
  }
}

async function deleteItem(row: Submission) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确定要删除投稿“${row.title}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const { data } = await adminApi.delete(`/content/admin/articles/${row.id}`)
    if (data.code === 0) {
      ElMessage.success('删除成功')
      fetchList()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.submission-management {
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
  gap: 16px;
}

.title-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  color: #6b7280;
  font-size: 13px;
}

.meta-time {
  margin-left: auto;
}

.label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}

.value {
  color: #111827;
}

.detail-cover {
  margin-top: 14px;
}

.cover-img {
  max-width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(209, 213, 219, 0.9);
}

:deep(.el-table) {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(148, 163, 184, 0.45);
}
</style>
