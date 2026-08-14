import { get, post } from './http'
import type { Company, QaAnswer, QaAskRequest } from '@/types/api'

export function listCompanies() {
  return get<Company[]>('/v1/companies')
}

export function askQuestion(body: QaAskRequest) {
  return post<QaAnswer>('/v1/qa/ask', body)
}
