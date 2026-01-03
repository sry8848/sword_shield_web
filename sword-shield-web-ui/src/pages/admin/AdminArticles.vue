<template>
  <div class="article-management">
    <div class="page-header">
      <div class="title-group">
        <h3>资讯列表</h3>
        <div class="filters">
          <el-select
            v-model="filters.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            @change="reload"
          >
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
          <el-select
            v-model="filters.category"
            placeholder="分类"
            clearable
            filterable
            allow-create
            default-first-option
            style="width: 160px"
            @change="reload"
          >
            <el-option v-for="c in categoryPresets" :key="c" :label="c" :value="c" />
          </el-select>
          <el-button @click="reload">刷新</el-button>
        </div>
      </div>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新建资讯
      </el-button>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" empty-text="暂无资讯">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="author" label="作者" width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.publishedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑资讯' : '新建资讯'"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="110px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="form.category"
            placeholder="选择或输入分类"
            filterable
            allow-create
            default-first-option
            clearable
            style="width: 100%"
          >
            <el-option v-for="c in categoryPresets" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="可选，默认使用管理员账号" />
        </el-form-item>

        <el-form-item label="摘要" required>
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入摘要" />
        </el-form-item>

        <el-form-item label="正文" required>
          <el-input v-model="form.body" type="textarea" :rows="10" placeholder="支持 HTML（前台会按 HTML 渲染）" />
        </el-form-item>

        <el-form-item label="封面图URL">
          <el-input v-model="form.coverUrl" placeholder="可选" />
        </el-form-item>

        <el-form-item label="直播链接">
          <el-input v-model="form.liveUrl" placeholder="可选" />
        </el-form-item>

        <el-form-item label="回放链接">
          <el-input v-model="form.replayUrl" placeholder="可选" />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">草稿</el-radio>
            <el-radio :label="1">发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台资讯管理页。
 *
 * - 管理员对文章（非 UGC）进行增删改查（CRUD），支持草稿/发布状态与分类筛选。
 *
 * 依赖接口：
 * - GET `/content/admin/articles`
 * - POST `/content/admin/articles`
 * - PUT `/content/admin/articles/:id`
 * - DELETE `/content/admin/articles/:id`
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import adminApi from '../../adminApi'

type Article = {
  /** 文章 ID（编辑/删除时存在）。 */
  id?: number

  /** 标题。 */
  title: string

  /** 摘要/简介。 */
  summary: string

  /** 正文内容。 */
  body: string

  /** 封面图片 URL（可为空）。 */
  coverUrl?: string

  /** 分类/频道（可为空）。 */
  category?: string

  /** 作者署名（可为空）。 */
  author?: string

  /** 直播链接（可为空）。 */
  liveUrl?: string

  /** 回放链接（可为空）。 */
  replayUrl?: string

  /** 状态：0-草稿，1-发布。 */
  status: number

  /** 发布时间（发布后才有）。 */
  publishedAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const items = ref<Article[]>([])
const total = ref(0)
const pageSize = ref(10)
const currentPage = ref(1)

const categoryPresets = ['通用', '技术', '安全', '行业', '通知']

const filters = reactive<{ status: number | null; category: string }>({
  status: null,
  category: ''
})

const form = reactive<Article>({
  title: '',
  summary: '',
  body: '',
  coverUrl: '',
  category: '通用',
  author: '',
  liveUrl: '',
  replayUrl: '',
  status: 1
})

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
      excludeCategory: 'UGC'
    }
    if (filters.category) params.category = filters.category
    if (filters.status !== null) params.status = filters.status

    const { data } = await adminApi.get('/content/admin/articles', { params })
    if (data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
    } else {
      ElMessage.error(data.message || '加载资讯列表失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载资讯列表失败')
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

function showCreateDialog() {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    title: '',
    summary: '',
    body: '',
    coverUrl: '',
    category: '通用',
    author: '',
    liveUrl: '',
    replayUrl: '',
    status: 1
  })
  dialogVisible.value = true
}

function showEditDialog(row: Article) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title || '',
    summary: row.summary || '',
    body: row.body || '',
    coverUrl: row.coverUrl || '',
    category: row.category || '通用',
    author: row.author || '',
    liveUrl: row.liveUrl || '',
    replayUrl: row.replayUrl || '',
    status: typeof row.status === 'number' ? row.status : 0
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.title.trim()) return ElMessage.warning('请输入标题')
  if (!form.summary.trim()) return ElMessage.warning('请输入摘要')
  if (!form.body.trim()) return ElMessage.warning('请输入正文')

  submitting.value = true
  try {
    const payload = {
      title: form.title,
      summary: form.summary,
      body: form.body,
      coverUrl: form.coverUrl || null,
      category: form.category || '通用',
      author: form.author || null,
      liveUrl: form.liveUrl || null,
      replayUrl: form.replayUrl || null,
      status: form.status
    }

    if (isEdit.value && form.id) {
      const { data } = await adminApi.put(`/content/admin/articles/${form.id}`, payload)
      if (data.code === 0) {
        ElMessage.success('资讯更新成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(data.message || '资讯更新失败')
      }
    } else {
      const { data } = await adminApi.post('/content/admin/articles', payload)
      if (data.code === 0) {
        ElMessage.success('资讯创建成功')
        dialogVisible.value = false
        fetchList()
      } else {
        ElMessage.error(data.message || '资讯创建失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function deleteItem(row: Article) {
  try {
    await ElMessageBox.confirm(`确定要删除资讯“${row.title}”吗？`, '提示', {
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
.article-management {
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

:deep(.el-table) {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(148, 163, 184, 0.45);
}
</style>
