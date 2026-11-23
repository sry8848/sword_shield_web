import type { components } from '../generated/openapi'

interface ApiResponse {
  code: string
  message: string
  traceId: string
  timestamp: string
}

export interface ApiEnvelope<T> extends ApiResponse {
  data: T
}

type ProblemDetail = components['schemas']['ProblemDetail']

export interface ApiErrorResponse extends Partial<ProblemDetail> {
  message?: string
  details?: Record<string, unknown>
}
