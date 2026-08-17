import { get } from './http'
import type { PageResult, QuestionHistoryItem, SummaryHistoryItem } from '@/types/api'

export interface QuestionHistoryQuery {
  companyId?: number | null
  intentType?: string | null
  keyword?: string | null
  startDate?: string | null
  endDate?: string | null
  pageNum?: number
  pageSize?: number
}

export interface SummaryHistoryQuery {
  companyId?: number | null
  mode?: string | null
  keyword?: string | null
  startDate?: string | null
  endDate?: string | null
  pageNum?: number
  pageSize?: number
}

export function listQuestionHistory(params: QuestionHistoryQuery) {
  return get<PageResult<QuestionHistoryItem>>('/v1/qa/history', params as Record<string, unknown>)
}

export function getQuestionHistory(questionId: number) {
  return get<QaAnswer>(`/v1/qa/history/${questionId}`)
}

export function listSummaryHistory(params: SummaryHistoryQuery) {
  return get<PageResult<SummaryHistoryItem>>('/v1/summary/history', params as Record<string, unknown>)
}

export function getSummaryHistory(summaryId: number) {
  return get<SummaryAnswer>(`/v1/summary/history/${summaryId}`)
}
