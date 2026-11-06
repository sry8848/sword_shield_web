import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import type { PageQuery, PageResult } from '../types/api'
import type {
  Activity,
  ActivityCreateRequest,
  ActivityDetail,
  ActivityUpdateRequest,
  AdminProfile,
  CheckinRecord,
  CheckinRequest,
  LoginRequest,
  LoginResult,
  Registration,
  RegistrationCredential,
  RegistrationDetail,
  RegistrationRequest,
  StatisticsOverview,
  User,
} from '../types/domain'
import { ApiError, request } from '../http/client'

const anonymous = { headers: { Authorization: false } }
const cookieAuth = { ...anonymous, withCredentials: true }

/** 为 Portal 和 Admin 提供共享的 MVP 接口方法，返回响应中的业务数据。 */
export function createApi(client: AxiosInstance) {
  const call = <T>(config: AxiosRequestConfig) => request<T>(client, {
    ...config,
    url: `/api/v1${config.url}`,
  })

  // 退出需要等正在进行的刷新结束，才能撤销服务端可能轮换后的 Cookie。
  let pendingRefresh: Promise<LoginResult> | undefined
  let pendingLogout: Promise<{ loggedOut: boolean }> | undefined

  return {
    auth: {
      login: (data: LoginRequest) => call<LoginResult>({ ...cookieAuth, url: '/auth/login', method: 'POST', data }),
      refresh: () => {
        if (pendingLogout)
          return Promise.reject(new ApiError('UNAUTHENTICATED', '正在退出登录', 401))
        pendingRefresh ??= call<LoginResult>({ ...cookieAuth, url: '/auth/refresh', method: 'POST' })
          .finally(() => { pendingRefresh = undefined })
        return pendingRefresh
      },
      logout: () => {
        pendingLogout ??= (async () => {
          if (pendingRefresh)
            await Promise.allSettled([pendingRefresh])
          return call<{ loggedOut: boolean }>({ ...cookieAuth, url: '/auth/logout', method: 'POST' })
        })().finally(() => { pendingLogout = undefined })
        return pendingLogout
      },
      me: () => call<User>({ url: '/auth/me', method: 'GET' }),
    },
    activities: {
      list: (params: PageQuery & { keyword?: string } = {}) => call<PageResult<Activity>>({ ...anonymous, url: '/public/events', method: 'GET', params }),
      detail: (eventId: string) => call<ActivityDetail>({ ...anonymous, url: `/public/events/${encodeURIComponent(eventId)}`, method: 'GET' }),
      register: (eventId: string, data: RegistrationRequest) => call<RegistrationDetail>({ url: `/events/${encodeURIComponent(eventId)}/registrations`, method: 'POST', data }),
    },
    registrations: {
      mine: (params: PageQuery = {}) => call<PageResult<Registration>>({ url: '/me/registrations', method: 'GET', params }),
      credential: (registrationId: string) => call<RegistrationCredential>({ url: `/me/registrations/${encodeURIComponent(registrationId)}/credential`, method: 'GET' }),
    },
    admin: {
      me: () => call<AdminProfile>({ url: '/admin/me', method: 'GET' }),
      activities: (params: PageQuery = {}) => call<PageResult<Activity>>({ url: '/admin/events', method: 'GET', params }),
      createActivity: (data: ActivityCreateRequest) => call<ActivityDetail>({ url: '/admin/events', method: 'POST', data }),
      updateActivity: (eventId: string, data: ActivityUpdateRequest) => call<ActivityDetail>({ url: `/admin/events/${encodeURIComponent(eventId)}`, method: 'PATCH', data }),
      publishActivity: (eventId: string) => call<ActivityDetail>({ url: `/admin/events/${encodeURIComponent(eventId)}/publish`, method: 'POST' }),
      registrations: (params: PageQuery = {}) => call<PageResult<Registration>>({ url: '/admin/registrations', method: 'GET', params }),
      registration: (registrationId: string) => call<RegistrationDetail>({ url: `/admin/registrations/${encodeURIComponent(registrationId)}`, method: 'GET' }),
      checkin: (data: CheckinRequest) => call<CheckinRecord>({ url: '/admin/check-ins', method: 'POST', data }),
      overview: () => call<StatisticsOverview>({ url: '/admin/statistics/overview', method: 'GET' }),
    },
  }
}
