import type { EventDetail, EventStatus } from '../features/activity/activity'

/** 生成稳定的演示资源 ID，供预览与深链接使用。 */
export function fixtureId(index: number) {
  return `01935f3a-7b2c-7abc-8def-${String(index).padStart(12, '0')}`
}

const mainHall = { id: fixtureId(101), name: '主会场 · 大会堂' }
const eastHall = { id: fixtureId(102), name: '分会场 · 东厅' }
const westHall = { id: fixtureId(103), name: '分会场 · 西厅' }

const schedule = [
  ['数字安全主论坛', '大会开幕式与主旨演讲', '20', '09:00', '12:00', mainHall, '大会组委会', 'PUBLISHED', 486, 600],
  ['人工智能与可信治理论坛', '讨论大模型应用中的信任与安全治理', '20', '14:00', '16:30', eastHall, '周启明', 'PUBLISHED', 128, 200],
  ['数据要素安全论坛', '数据流通、合规和基础设施安全', '20', '14:30', '17:00', westHall, '陈雨桐', 'PUBLISHED', 76, 150],
  ['云原生安全技术峰会', '云原生环境下的安全工程实践', '21', '09:30', '11:30', eastHall, '林嘉禾', 'DRAFT', 0, 180],
  ['产业数字化安全圆桌', '面向产业运营场景的安全经验交流', '21', '10:00', '11:30', westHall, '顾知行', 'UNPUBLISHED', 32, 80],
  ['数字安全人才发展论坛', '产教协同与安全人才培养', '21', '14:00', '16:30', eastHall, '许文萱', 'DRAFT', 0, null],
  ['大会闭幕交流', '成果分享与闭幕交流', '21', '16:30', '17:30', mainHall, '大会组委会', 'DRAFT', 0, null],
] as const satisfies readonly (readonly [
  string,
  string,
  string,
  string,
  string,
  NonNullable<EventDetail['venue']>,
  string,
  EventStatus,
  number,
number | null,
])[]

/** 从固定议程生成独立的 Mock 场次记录。 */
export function activityFixtures(): EventDetail[] {
  return schedule.map(([title, summary, day, startsAt, endsAt, venue, speaker, status, registeredCount, capacity], index) => ({
    id: fixtureId(index + 1),
    title,
    summary,
    description: summary,
    startTime: `2027-05-${day}T${startsAt}:00+08:00`,
    endTime: `2027-05-${day}T${endsAt}:00+08:00`,
    venue,
    speakers: [{ id: fixtureId(201 + index), name: speaker }],
    status,
    registrationStatus: status === 'PUBLISHED' ? 'OPEN' : status === 'UNPUBLISHED' ? 'CLOSED' : 'NOT_OPEN',
    registeredCount,
    capacity,
    registrationStartAt: '2026-11-01T09:00:00+08:00',
    registrationEndAt: '2027-05-19T18:00:00+08:00',
  }))
}
