export type AdminRole = 'SUPER_ADMIN' | 'EVENT_OPERATOR' | 'CHECKIN_STAFF' | 'DATA_OPERATOR'

export interface User {
  id: string
  account: string
  displayName: string
  phone?: string | null
  email?: string | null
  avatarUrl?: string | null
}

export interface AdminProfile {
  user: User
  roles: AdminRole[]
  permissions: string[]
}

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED' | 'FINISHED'
export type RegistrationAvailability = 'OPEN' | 'NOT_OPEN' | 'CLOSED' | 'FULL'

export interface VenueBrief {
  id: string
  name: string
  address?: string | null
}

export interface SpeakerBrief {
  id: string
  name: string
  title?: string | null
  organization?: string | null
}

export interface Activity {
  id: string
  title: string
  summary?: string | null
  coverUrl?: string | null
  startTime: string
  endTime: string
  venue?: VenueBrief | null
  speakers?: SpeakerBrief[]
  status: ActivityStatus
  registrationStatus: RegistrationAvailability
  capacity?: number | null
  registeredCount?: number
}

export interface ActivityDetail extends Activity {
  description?: string | null
  registrationStartAt?: string | null
  registrationEndAt?: string | null
  address?: string | null
  organizer?: string | null
  tags?: string[]
}

export interface ActivityCreateRequest {
  title: string
  startTime: string
  endTime: string
  summary?: string | null
  description?: string | null
  coverUrl?: string | null
  registrationStartAt?: string | null
  registrationEndAt?: string | null
  capacity?: number | null
  venueId?: string | null
  venueName?: string | null
  address?: string | null
  speakerIds?: string[]
  organizer?: string | null
  tags?: string[]
}

export type ActivityUpdateRequest = Partial<ActivityCreateRequest>
export type RegistrationStatus = 'REGISTERED' | 'CANCELLED' | 'CHECKED_IN'
export type CredentialStatus = 'VALID' | 'CHECKED_IN' | 'INVALID'

export interface RegistrationRequest {
  attendeeName: string
  phone: string
  organization?: string | null
  position?: string | null
  remark?: string | null
}

export interface Registration {
  id: string
  eventId: string
  eventTitle: string
  status: RegistrationStatus
  registeredAt: string
  credentialStatus?: CredentialStatus
}

export interface Credential {
  credentialNo: string
  qrCodeContent: string
  status: CredentialStatus
  validFrom?: string | null
  validUntil?: string | null
}

export interface RegistrationDetail extends Registration {
  attendeeName?: string
  phone?: string
  organization?: string | null
  position?: string | null
  remark?: string | null
  credential?: Credential
}

export interface RegistrationCredential {
  registrationId: string
  eventId: string
  credential: Credential
}

export interface CheckinRequest {
  eventId: string
  credentialCode: string
  inputMode: 'QR_SCAN' | 'MANUAL_INPUT'
}

export interface CheckinRecord {
  id: string
  eventId: string
  eventTitle?: string
  registrationId: string
  credentialNo: string
  attendeeName?: string
  organization?: string | null
  checkedInAt: string
  operatorId: string
  operatorName?: string | null
  inputMode?: CheckinRequest['inputMode']
}

export interface StatisticsOverview {
  eventCount: number
  registrationCount: number
  checkedInCount: number
  checkInRate: number
  startDate?: string | null
  endDate?: string | null
  generatedAt?: string
}
