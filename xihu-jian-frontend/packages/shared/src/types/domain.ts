export type UserRole = 'USER' | 'ADMIN' | 'OPS' | 'CHECKIN'

export interface User {
  id: string
  username: string
  displayName: string
  role: UserRole
}

export interface LoginResult {
  accessToken: string
  user: User
}

export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED'
export type RegistrationAvailability = 'OPEN' | 'NOT_OPEN' | 'CLOSED' | 'FULL'

export interface Activity {
  id: string
  title: string
  summary: string
  startAt: string
  endAt?: string
  venue: string
  speaker?: string
  status: ActivityStatus
  registrationStatus: RegistrationAvailability
  capacity: number
  registeredCount: number
}

export interface ActivityCreateRequest {
  title: string
  summary: string
  startAt: string
  venue: string
  speaker?: string
  capacity: number
}

export type RegistrationStatus = 'REGISTERED' | 'CANCELLED' | 'CHECKED_IN'

export interface Registration {
  id: string
  activityId: string
  activityTitle: string
  attendeeName: string
  ticketCode: string
  status: RegistrationStatus
  createdAt: string
}

export interface CheckinRequest {
  ticketCode: string
}

export interface StatisticsOverview {
  activityCount: number
  registrationCount: number
  checkedInCount: number
}
