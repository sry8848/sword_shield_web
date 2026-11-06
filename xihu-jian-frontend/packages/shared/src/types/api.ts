export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

export interface PageQuery {
  page?: number
  pageSize?: number
  keyword?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface LoginRequest {
  username: string
  password: string
}
