import { post } from './http'
import type { AgentAnswer } from '@/types/api'

export interface AgentRunRequest {
  question: string
  companyId?: number | null
  stockCode?: string
  docTypes?: string[]
  topK?: number
  summaryMode?: string | null
  startDate?: string | null
  endDate?: string | null
}

export function runAgent(body: AgentRunRequest) {
  return post<AgentAnswer>('/v1/agent/run', body)
}
