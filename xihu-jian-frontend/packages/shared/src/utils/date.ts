/** 将接口日期时间格式化为大会当地时间，缺失或无法解析时返回占位符。 */
export function formatDateTime(value?: string): string {
  // 1. 解析接口传入的日期时间。
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'

  // 2. 按本届杭州大会的时区格式化。
  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(date)
}
