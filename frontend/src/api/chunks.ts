import { get } from './http'
import type { DocumentChunk, PageResult } from '@/types/api'

export interface ChunkQuery {
  documentId?: number | null
  companyId?: number | null
  startDate?: string | null
  endDate?: string | null
  pageNum?: number
  pageSize?: number
}

export function listChunks(params: ChunkQuery) {
  return get<PageResult<DocumentChunk>>('/v1/chunks', params as Record<string, unknown>)
}

export function getChunk(id: number) {
  return get<DocumentChunk>(`/v1/chunks/${id}`)
}

export function listChunksByDocument(documentId: number) {
  return get<PageResult<DocumentChunk>>('/v1/chunks', { documentId, pageNum: 1, pageSize: 200 })
    .then((result) => result.records)
}
