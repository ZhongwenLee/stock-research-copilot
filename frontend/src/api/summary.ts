import { post } from './http'
import type { SummaryAnswer } from '@/types/api'

export interface SummaryGenerateRequest {
  companyId: number
  stockCode?: string
  docTypes?: string[]
  mode?: 'FAST' | 'DEEP'
  startDate?: string | null
  endDate?: string | null
  topK?: number
}

export function generateSummary(body: SummaryGenerateRequest) {
  return post<SummaryAnswer>('/v1/summary/generate', body)
}
