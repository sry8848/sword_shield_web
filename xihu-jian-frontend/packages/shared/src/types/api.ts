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
