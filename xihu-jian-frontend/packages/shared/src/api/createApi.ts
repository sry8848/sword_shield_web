import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import { request } from '../http/client'
import type { PageQuery, PageResult } from '../types/api'
import type {
  Activity,
  ActivityCreateRequest,
  CheckinRequest,
  LoginRequest,
  LoginResult,
  Registration,
  StatisticsOverview,
  User,
} from '../types/domain'

export function createApi(client: AxiosInstance) {
  const call = <T>(config: AxiosRequestConfig) => request<T>(client, config)

  return {
    auth: {
      login: (data: LoginRequest) => call<LoginResult>({ url: '/api/auth/login', method: 'POST', data }),
      me: () => call<User>({ url: '/api/auth/me', method: 'GET' }),
    },
    activities: {
      list: (params: PageQuery = {}) => call<PageResult<Activity>>({ url: '/api/activities', method: 'GET', params }),
      detail: (activityId: string) => call<Activity>({ url: `/api/activities/${encodeURIComponent(activityId)}`, method: 'GET' }),
      register: (activityId: string) => call<Registration>({ url: `/api/activities/${encodeURIComponent(activityId)}/registrations`, method: 'POST' }),
    },
    registrations: {
      mine: () => call<Registration[]>({ url: '/api/me/registrations', method: 'GET' }),
    },
    admin: {
      activities: () => call<PageResult<Activity>>({ url: '/api/admin/activities', method: 'GET', params: { page: 1, pageSize: 100 } }),
      createActivity: (data: ActivityCreateRequest) => call<Activity>({ url: '/api/admin/activities', method: 'POST', data }),
      updateActivity: (activityId: string, data: ActivityCreateRequest) => call<Activity>({ url: `/api/admin/activities/${encodeURIComponent(activityId)}`, method: 'PUT', data }),
      publishActivity: (activityId: string) => call<Activity>({ url: `/api/admin/activities/${encodeURIComponent(activityId)}/publish`, method: 'POST' }),
      registrations: () => call<Registration[]>({ url: '/api/admin/registrations', method: 'GET' }),
      checkin: (data: CheckinRequest) => call<Registration>({ url: '/api/admin/check-ins', method: 'POST', data }),
      overview: () => call<StatisticsOverview>({ url: '/api/admin/statistics/overview', method: 'GET' }),
    },
  }
}
