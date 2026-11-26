<script setup lang="ts">
import type { EventCreate, EventDetail, EventUpdate } from './activity'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElButton, ElDrawer, ElForm, ElFormItem, ElInput, ElMessage, ElSkeleton, ElTag } from 'element-plus'
import { computed, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { eventStatuses } from './activity'
import { getEvent, saveEvent } from './api'
import 'element-plus/es/components/drawer/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/form/style/css'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const eventId = computed(() => route.name === 'event-edit' ? String(route.params.eventId) : '')
const editorTitle = computed(() => eventId.value ? '编辑场次' : '新建场次')

const { data: event, isPending: isLoadingEvent, isError, error, refetch } = useQuery({
  queryKey: computed(() => ['events', 'detail', eventId.value] as const),
  queryFn: ({ queryKey, signal }) => getEvent(queryKey[2], signal),
  enabled: computed(() => !!eventId.value),
})

const { mutateAsync: submitEvent, isPending: isSaving } = useMutation({
  mutationFn: ({ fields, id }: { fields: EventCreate | EventUpdate, id: string }) => saveEvent(fields, id || undefined),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events'] }),
})

const form = reactive({
  title: '',
  summary: '',
  description: '',
  venueName: '',
  startTime: '',
  endTime: '',
  registrationStartAt: '',
  registrationEndAt: '',
  capacity: '',
})
const formError = ref('')
// 初始表单值用于 PATCH，只提交运营实际修改的字段。
const initialFields = shallowRef<EventCreate>()

// 日期字段固定按大会时区编辑，与看原型的设备时区无关。
const conferenceInputTime = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Shanghai',
  dateStyle: 'short',
  timeStyle: 'short',
})

/** 将接口时间转成 datetime-local 需要的大会当地时间。 */
function asInputTime(value?: string | null) {
  return value ? conferenceInputTime.format(new Date(value)).replace(' ', 'T') : ''
}

/** 用当前详情填充表单，创建场次时初始化空白字段。 */
function fillForm(detail?: EventDetail) {
  // 1. 仅复制当前表单管理的字段，编辑时保留其他契约字段。
  Object.assign(form, {
    title: detail?.title ?? '',
    summary: detail?.summary ?? '',
    description: detail?.description ?? '',
    venueName: detail?.venue?.name ?? '',
    startTime: asInputTime(detail?.startTime),
    endTime: asInputTime(detail?.endTime),
    registrationStartAt: asInputTime(detail?.registrationStartAt),
    registrationEndAt: asInputTime(detail?.registrationEndAt),
    capacity: detail?.capacity == null ? '' : String(detail.capacity),
  })
  initialFields.value = detail ? toRequestFields() : undefined
  formError.value = ''
}

watch([eventId, event], ([id, current], [previousId, previousEvent]) => {
  // 同一场次后台刷新时保留用户正在编辑的表单。
  if (!id || (current?.id === id && (id !== previousId || !previousEvent)))
    fillForm(current)
}, { immediate: true })

/** 将当前表单转换为带大会时区偏移的场次请求。 */
function toRequestFields(): EventCreate {
  return {
    title: form.title.trim(),
    summary: form.summary.trim() || null,
    description: form.description.trim() || null,
    venueName: form.venueName.trim() || null,
    startTime: `${form.startTime}:00+08:00`,
    endTime: `${form.endTime}:00+08:00`,
    registrationStartAt: form.registrationStartAt ? `${form.registrationStartAt}:00+08:00` : null,
    registrationEndAt: form.registrationEndAt ? `${form.registrationEndAt}:00+08:00` : null,
    capacity: form.capacity === '' ? null : Number(form.capacity),
  }
}

/** 关闭抽屉，回到原来的列表条件。 */
function handleClose() {
  return router.replace({ name: 'events', query: route.query })
}

/** 校验编辑值并提交，失败时保留用户输入。 */
async function handleSubmit() {
  // 1. 检查表单的时间范围和已报名人数约束。
  formError.value = ''
  if (!form.title.trim() || !form.startTime || !form.endTime) {
    formError.value = '请填写场次名称、开始时间和结束时间。'
    return
  }
  if (form.endTime <= form.startTime) {
    formError.value = '场次结束时间应晚于开始时间。'
    return
  }
  if (form.registrationStartAt && form.registrationEndAt && form.registrationEndAt <= form.registrationStartAt) {
    formError.value = '报名截止时间应晚于报名开始时间。'
    return
  }
  const fields = toRequestFields()
  if (fields.capacity != null && (!Number.isSafeInteger(fields.capacity) || fields.capacity < Math.max(1, event.value?.registeredCount ?? 0))) {
    formError.value = '名额须为正整数，且不得少于已报名人数。'
    return
  }

  // 2. 编辑只提交变化的字段，创建提交完整表单。
  const before = initialFields.value
  const changes: EventUpdate = before
    ? Object.fromEntries(Object.entries(fields).filter(([key, value]) => before[key as keyof EventCreate] !== value))
    : fields
  if (!Object.keys(changes).length) {
    await handleClose()
    return
  }

  // 3. 保存成功后保留筛选，回到刷新后的列表。
  try {
    const id = eventId.value
    await submitEvent({ fields: changes, id })
    ElMessage.success(id ? '场次已保存' : '草稿已保存')
    await handleClose()
  }
  catch (error) {
    formError.value = error instanceof Error ? error.message : '场次保存失败'
  }
}
</script>

<template>
  <ElDrawer
    :model-value="true"
    size="min(640px, 100vw)"
    :aria-label="editorTitle"
    :close-on-click-modal="false"
    :close-on-press-escape="!isSaving"
    :show-close="!isSaving"
    append-to-body
    @close="handleClose"
  >
    <template #header="{ titleId }">
      <div class="editor-heading">
        <h2 :id="titleId">
          {{ editorTitle }}
        </h2>
        <ElTag v-if="event" :type="eventStatuses[event.status].type" effect="light" round>
          {{ eventStatuses[event.status].label }}
        </ElTag>
      </div>
    </template>

    <ElSkeleton v-if="eventId && isLoadingEvent" :rows="6" animated />
    <div v-else-if="eventId && isError" role="alert">
      <p>{{ error?.message || '场次详情加载失败' }}</p>
      <ElButton @click="refetch()">
        重新加载
      </ElButton>
    </div>
    <ElForm v-else id="event-editor" label-position="top" :disabled="isSaving" @submit.prevent="handleSubmit">
      <section class="form-section" aria-labelledby="basic-heading">
        <h3 id="basic-heading">
          基本信息
        </h3>
        <ElFormItem label="场次名称" for="event-title" required>
          <ElInput id="event-title" v-model="form.title" maxlength="120" placeholder="例如：数字安全主论坛" required />
        </ElFormItem>
        <div class="form-grid">
          <ElFormItem label="场次开始时间" for="event-start" required>
            <input id="event-start" v-model="form.startTime" class="date-input" type="datetime-local" :disabled="isSaving" required>
          </ElFormItem>
          <ElFormItem label="场次结束时间" for="event-end" required>
            <input id="event-end" v-model="form.endTime" class="date-input" type="datetime-local" :disabled="isSaving" required>
          </ElFormItem>
        </div>
        <ElFormItem label="会场" for="event-venue">
          <ElInput id="event-venue" v-model="form.venueName" placeholder="填写会场名称" />
        </ElFormItem>
        <ElFormItem label="场次摘要" for="event-summary">
          <ElInput id="event-summary" v-model="form.summary" type="textarea" :rows="2" maxlength="300" show-word-limit placeholder="用于议程列表的简短介绍" />
        </ElFormItem>
        <ElFormItem label="详细介绍" for="event-description">
          <ElInput id="event-description" v-model="form.description" type="textarea" :rows="3" placeholder="填写场次详情" />
        </ElFormItem>
      </section>
      <section class="form-section" aria-labelledby="registration-heading">
        <div class="section-heading">
          <h3 id="registration-heading">
            报名设置
          </h3>
          <span>北京时间 UTC+08:00</span>
        </div>
        <div class="form-grid">
          <ElFormItem label="报名开始时间" for="registration-start">
            <input id="registration-start" v-model="form.registrationStartAt" class="date-input" type="datetime-local" :disabled="isSaving">
          </ElFormItem>
          <ElFormItem label="报名截止时间" for="registration-end">
            <input id="registration-end" v-model="form.registrationEndAt" class="date-input" type="datetime-local" :disabled="isSaving">
          </ElFormItem>
        </div>
        <ElFormItem label="报名名额" for="event-capacity">
          <ElInput id="event-capacity" v-model="form.capacity" type="number" min="1" step="1" placeholder="留空表示不限额" />
        </ElFormItem>
      </section>
    </ElForm>

    <template #footer>
      <p v-if="formError" class="form-error" role="alert">
        {{ formError }}
      </p>
      <ElButton :disabled="isSaving" @click="handleClose">
        取消
      </ElButton>
      <ElButton v-if="!eventId || (event && !isError)" type="primary" native-type="submit" form="event-editor" :loading="isSaving">
        {{ eventId ? '保存更改' : '保存草稿' }}
      </ElButton>
    </template>
  </ElDrawer>
</template>

<style scoped>
.editor-heading { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.editor-heading h2 { margin: 0; color: var(--color-text); font-size: 22px; line-height: 1.35; }
.form-section { padding-bottom: 4px; }
.form-section + .form-section { padding-top: 20px; border-top: 1px solid var(--color-border); }
.form-section h3 { margin: 0 0 16px; color: var(--color-text); font-size: 15px; }
.section-heading { display: flex; align-items: baseline; flex-wrap: wrap; justify-content: space-between; gap: 8px; }
.section-heading span { color: var(--color-muted); font-size: 12px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 16px; }
.date-input { width: 100%; min-height: 32px; padding: 4px 10px; border: 1px solid var(--el-border-color); border-radius: var(--el-border-radius-base); background: var(--color-surface); color: var(--color-text); font: inherit; }
.date-input:disabled { color: var(--color-muted); }
.form-error { margin: 0 0 12px; text-align: left; color: var(--color-error); font-size: 14px; }
@media (max-width: 520px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
