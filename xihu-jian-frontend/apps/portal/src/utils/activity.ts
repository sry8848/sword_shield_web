import type { ActivityStatus, RegistrationAvailability } from '@xhlj/shared'
export function activityStatusLabel(status: ActivityStatus): string {
  const labels: Record<ActivityStatus, string> = { DRAFT: '草稿', PUBLISHED: '已发布', UNPUBLISHED: '已下线', FINISHED: '已结束' }
  return labels[status]
}
export function registrationStatusLabel(status: RegistrationAvailability): string {
  const labels: Record<RegistrationAvailability, string> = { OPEN: '报名开放中', NOT_OPEN: '尚未开放', CLOSED: '报名已截止', FULL: '名额已满' }
  return labels[status]
}
