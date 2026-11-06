interface ApiResponse {
  code: string
  message: string
  traceId: string
  timestamp: string
}

export interface ApiEnvelope<T> extends ApiResponse {
  data: T
}

export interface ApiErrorResponse extends ApiResponse {
  details?: Record<string, unknown>
}

export interface PageQuery {
  page?: number
  pageSize?: number
}

export interface PageInfo {
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface PageResult<T> {
  items: T[]
  pageInfo: PageInfo
}
