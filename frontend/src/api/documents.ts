import { get, post } from './http'
import type { DocumentVO, PageResult } from '@/types/api'

export interface DocumentQuery {
  companyId?: number | null
  docType?: string | null
  processStatus?: string | null
  keyword?: string | null
  startDate?: string | null
  endDate?: string | null
  pageNum?: number
  pageSize?: number
}

export function listDocuments(params: DocumentQuery) {
  return get<PageResult<DocumentVO>>('/v1/documents', params as Record<string, unknown>)
}

export function getDocument(id: number) {
  return get<DocumentVO>(`/v1/documents/${id}`)
}

export function reprocessDocument(id: number) {
  return post<DocumentVO>(`/v1/documents/${id}/reprocess`)
}

export function getDocumentFileUrl(id: number) {
  return `/api/v1/documents/${id}/file`
}
