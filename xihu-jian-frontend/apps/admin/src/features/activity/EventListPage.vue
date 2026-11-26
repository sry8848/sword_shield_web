<script setup lang="ts">
import type { EventFilters, EventStatus, EventSummary } from './activity'
import { Calendar, Plus, Search } from '@element-plus/icons-vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { formatDateTime } from '@xhlj/shared'
import { ElButton, ElEmpty, ElIcon, ElInput, ElMessage, ElMessageBox, ElOption, ElPagination, ElSelect, ElSkeleton, ElTag } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { eventStatuses, registrationLabels } from './activity'
import { changeEventStatus, listEvents } from './api'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/empty/style/css'
import 'element-plus/es/components/icon/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/skeleton/style/css'
import 'element-plus/es/components/tag/style/css'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const pageSize = 10

// URL 保存已提交的条件，输入框单独保留待提交的值。
const filters = computed<EventFilters>(() => {
  // 1. 验证 URL 中的筛选和分页参数。
  const { keyword, status, page } = route.query
  const pageNumber = Number(page)

  // 2. 将有效条件交给列表请求。
  return {
    keyword: typeof keyword === 'string' ? keyword.trim() || undefined : undefined,
    status: typeof status === 'string' && Object.hasOwn(eventStatuses, status) ? status as EventStatus : undefined,
    page: typeof page === 'string' && Number.isSafeInteger(pageNumber) && pageNumber > 0 ? pageNumber : 1,
    pageSize,
  }
})
const keywordInput = ref('')
const statusInput = ref<EventStatus | ''>('')
watch([() => filters.value.keyword, () => filters.value.status], ([keyword, status]) => {
  keywordInput.value = keyword ?? ''
  statusInput.value = status ?? ''
}, { immediate: true })

const { data: events, isPending, isError, error, refetch } = useQuery({
  queryKey: computed(() => ['events', 'list', filters.value] as const),
  queryFn: ({ queryKey, signal }) => listEvents(queryKey[2], signal),
})

const { mutateAsync: updateStatus, isPending: isUpdatingStatus } = useMutation({
  mutationFn: ({ eventId, action }: { eventId: string, action: 'publish' | 'unpublish' }) => changeEventStatus(eventId, action),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events'] }),
})

/** 应用当前筛选，从第一页重新查询。 */
function handleSearch() {
  const keyword = keywordInput.value.trim()
  return router.push({ name: 'events', query: { keyword: keyword || undefined, status: statusInput.value || undefined } })
}

/** 清除关键词和状态，恢复默认列表。 */
function handleReset() {
  keywordInput.value = ''
  statusInput.value = ''
  return handleSearch()
}

/** 保留已提交的筛选条件，更新 URL 中的页码。 */
function handlePageChange(page: number) {
  return router.push({ name: 'events', query: { ...route.query, page: page === 1 ? undefined : String(page) } })
}

/** 打开新建或编辑抽屉，保留列表的筛选和翻页位置。 */
function handleOpenEditor(eventId?: string) {
  return router.push(eventId
    ? { name: 'event-edit', params: { eventId }, query: route.query }
    : { name: 'event-create', query: route.query })
}

/** 确认对外可见性变更，提交所选场次并刷新列表。 */
async function handleChangeStatus(event: EventSummary) {
  const isPublished = event.status === 'PUBLISHED'
  const action = isPublished ? 'unpublish' : 'publish'
  const actionLabel = isPublished ? '下线' : '发布'
  const registeredCount = event.registeredCount ?? 0

  // 1. 告知公开议程和已有报名受到的影响。
  const message = isPublished
    ? `「${event.title}」将从公开议程隐藏，并停止接受新报名。${registeredCount ? `已有 ${registeredCount} 条报名记录会保留。` : ''}`
    : `「${event.title}」发布后将在公开议程显示，报名开放时间以已保存的设置为准。`
  const confirmed = await ElMessageBox.confirm(message, `${actionLabel}场次`, {
    confirmButtonText: `确认${actionLabel}`,
    cancelButtonText: '取消',
    type: isPublished ? 'warning' : 'info',
    distinguishCancelAndClose: true,
  }).then(() => true, () => false)
  if (!confirmed)
    return

  // 2. 等待写操作和列表刷新，再显示结果。
  try {
    await updateStatus({ eventId: event.id, action })
    ElMessage.success(`场次已${actionLabel}`)
  }
  catch (error) {
    ElMessage.error(error instanceof Error ? error.message : `场次${actionLabel}失败`)
  }
}
</script>

<template>
  <section class="event-page" aria-labelledby="events-title">
    <header class="page-heading">
      <div>
        <h1 id="events-title">
          活动与议程
        </h1>
        <p>场次和报名时间均使用北京时间。</p>
      </div>
      <ElButton type="primary" :icon="Plus" @click="handleOpenEditor()">
        新建场次
      </ElButton>
    </header>

    <div class="events-card">
      <form class="filters" aria-label="场次筛选" @submit.prevent="handleSearch">
        <ElInput v-model="keywordInput" class="keyword" aria-label="场次名称" placeholder="搜索场次名称" :prefix-icon="Search" clearable />
        <ElSelect v-model="statusInput" class="status-filter" aria-label="场次状态" placeholder="全部状态" value-on-clear="" clearable>
          <ElOption v-for="(status, value) in eventStatuses" :key="value" :label="status.label" :value="value" />
        </ElSelect>
        <ElButton type="primary" plain native-type="submit">
          查询
        </ElButton>
        <ElButton text @click="handleReset">
          重置
        </ElButton>
      </form>

      <div v-if="isPending" class="query-state" role="status" aria-label="正在加载场次">
        <ElSkeleton :rows="5" animated />
      </div>
      <div v-else-if="isError" class="query-state error-state" role="alert">
        <p>{{ error?.message || '场次加载失败' }}</p>
        <ElButton @click="refetch()">
          重新加载
        </ElButton>
      </div>
      <ElEmpty v-else-if="!events?.items.length" :description="filters.keyword || filters.status ? '没有符合条件的场次' : '暂无场次'" :image-size="88">
        <ElButton v-if="filters.keyword || filters.status" @click="handleReset">
          清除筛选
        </ElButton>
        <ElButton v-else type="primary" @click="handleOpenEditor()">
          新建场次
        </ElButton>
      </ElEmpty>
      <div v-else class="table-scroll" tabindex="0" role="region" aria-label="场次列表">
        <table class="events-table" aria-label="场次列表">
          <thead>
            <tr>
              <th scope="col">
                场次名称
              </th>
              <th scope="col">
                场次时间
              </th>
              <th scope="col" class="venue-column">
                会场
              </th>
              <th scope="col" class="speaker-column">
                讲者
              </th>
              <th scope="col" class="registration-column">
                已报名 / 名额
              </th>
              <th scope="col">
                发布状态
              </th>
              <th scope="col">
                操作
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="event in events.items" :key="event.id">
              <th scope="row" class="event-name">
                <span>{{ event.title }}</span>
                <span v-if="event.summary" class="cell-meta">{{ event.summary }}</span>
              </th>
              <td class="time-column">
                <span class="event-time"><ElIcon><Calendar /></ElIcon>{{ formatDateTime(event.startTime) }}</span>
                <span class="cell-meta time-end">至 {{ formatDateTime(event.endTime) }}</span>
              </td>
              <td class="venue-column">
                {{ event.venue?.name || '—' }}
              </td>
              <td class="speaker-column">
                {{ event.speakers?.map(speaker => speaker.name).join('、') || '—' }}
              </td>
              <td class="registration-column">
                <span class="registration-count">{{ event.registeredCount ?? 0 }}</span>
                <span class="cell-meta registration-capacity"> / {{ event.capacity ?? '不限额' }}</span>
                <span class="cell-meta">{{ registrationLabels[event.registrationStatus] }}</span>
              </td>
              <td class="event-status">
                <ElTag :type="eventStatuses[event.status].type" effect="light" round>
                  <span class="status-dot" aria-hidden="true" />{{ eventStatuses[event.status].label }}
                </ElTag>
              </td>
              <td class="row-actions">
                <ElButton link type="primary" :aria-label="`编辑 ${event.title}`" @click="handleOpenEditor(event.id)">
                  编辑
                </ElButton>
                <ElButton
                  v-if="event.status !== 'FINISHED'"
                  link
                  :type="event.status === 'PUBLISHED' ? 'danger' : 'primary'"
                  :disabled="isUpdatingStatus"
                  :aria-label="`${event.status === 'PUBLISHED' ? '下线' : '发布'} ${event.title}`"
                  @click="handleChangeStatus(event)"
                >
                  {{ event.status === 'PUBLISHED' ? '下线' : '发布' }}
                </ElButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer v-if="events && !isError" class="table-footer">
        <span>共 {{ events.pageInfo.total }} 条场次</span>
        <ElPagination
          :current-page="filters.page ?? 1"
          :page-size="pageSize"
          :total="events.pageInfo.total"
          layout="prev, pager, next"
          :pager-count="5"
          background
          @update:current-page="handlePageChange"
        />
      </footer>
    </div>
  </section>
  <RouterView />
</template>

<style scoped>
.event-page { width: min(100%, var(--content-width)); margin-inline: auto; }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.page-heading h1 { margin: 0 0 8px; font-size: 28px; font-weight: 700; line-height: 1.25; }
.page-heading p { margin: 0; color: var(--color-muted); font-size: 14px; line-height: 1.6; }
.page-heading .el-button { height: 40px; padding-inline: 16px; font-weight: 600; }
.events-card { min-width: 0; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); overflow: hidden; }
.filters { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 20px; }
.filters .el-button { margin: 0; }
.keyword { width: 268px; }
.status-filter { width: 144px; }
.query-state { min-height: 160px; padding: 24px; border-top: 1px solid var(--color-border); }
.error-state { text-align: center; }
.error-state p { color: var(--color-error); }
.table-scroll { overflow-x: auto; }
.table-scroll:focus-visible { outline-offset: -2px; }
.events-table { width: 100%; border-collapse: collapse; color: var(--color-text); text-align: left; font-size: 13px; }
.events-table :is(th, td) { padding: 16px 12px; border-block-start: 1px solid var(--color-border); vertical-align: middle; }
.events-table :is(th, td):first-child { padding-inline-start: 20px; }
.events-table :is(th, td):last-child { padding-inline-end: 20px; }
.events-table thead th { padding-block: 12px; background: var(--color-table-heading); color: var(--color-muted); font-weight: 600; white-space: nowrap; }
.events-table tbody tr:hover { background: var(--color-table-heading); }
.event-name { width: 30%; min-width: 200px; font-size: 14px; font-weight: 600; line-height: 1.45; overflow-wrap: anywhere; }
.cell-meta { display: block; margin-top: 4px; color: var(--color-muted); font-size: 12px; font-weight: 400; line-height: 1.45; }
.event-time { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
.event-time .el-icon { color: var(--color-muted); }
.time-end { padding-inline-start: 19px; white-space: nowrap; }
.registration-column, .row-actions { white-space: nowrap; }
.registration-count { font-weight: 600; font-variant-numeric: tabular-nums; }
.registration-capacity { display: inline; }
.status-dot { display: inline-block; width: 6px; height: 6px; margin-inline-end: 6px; border-radius: 50%; background: currentColor; vertical-align: 1px; }
.row-actions .el-button { min-height: 32px; font-weight: 500; }
.table-footer { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 20px; border-top: 1px solid var(--color-border); color: var(--color-muted); font-size: 13px; }
@media (max-width: 1100px) {
  .speaker-column { display: none; }
}
@media (max-width: 900px) {
  .registration-column { display: none; }
}
@media (max-width: 767px) {
  .page-heading h1 { font-size: 23px; }
  .page-heading p { font-size: 13px; }
  .filters { padding: 12px; gap: 8px; }
  .keyword { width: 100%; }
  .status-filter { flex: 1; min-width: 108px; }
  .events-table, .events-table tbody { display: block; }
  .events-table thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
  .events-table tbody tr {
    display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center;
    gap: 8px 12px; padding: 12px; border-top: 1px solid var(--color-border);
  }
  .events-table tbody :is(th, td) { padding: 0; border: 0; }
  .events-table tbody :is(th, td):is(:first-child, :last-child) { padding-inline: 0; }
  .event-name { grid-area: 1 / 1; width: auto; min-width: 0; }
  .event-status { grid-area: 1 / 2; justify-self: end; }
  .time-column { grid-area: 2 / 1; }
  .row-actions { grid-area: 2 / 2; justify-self: end; }
  .row-actions .el-button { min-height: 40px; }
  .venue-column { display: none; }
  .table-footer { padding-inline: 12px; }
}
</style>
