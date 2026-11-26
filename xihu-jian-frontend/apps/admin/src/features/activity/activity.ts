import type { components, paths } from '@xhlj/shared'

export type EventSummary = components['schemas']['EventSummary']
export type EventDetail = components['schemas']['EventDetail']
export type EventCreate = components['schemas']['EventCreateRequest']
export type EventUpdate = components['schemas']['EventUpdateRequest']
export type EventPage = components['schemas']['EventPageData']
export type EventStatus = EventSummary['status']
export type EventFilters = NonNullable<paths['/admin/events']['get']['parameters']['query']>

export const eventStatuses = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  UNPUBLISHED: { label: '已下线', type: 'warning' },
  FINISHED: { label: '已结束', type: 'info' },
} as const satisfies Record<EventStatus, { label: string, type: string }>

export const registrationLabels: Record<EventSummary['registrationStatus'], string> = {
  NOT_OPEN: '未开放',
  OPEN: '报名中',
  CLOSED: '已截止',
  FULL: '已满额',
}
