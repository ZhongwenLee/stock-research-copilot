export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface Company {
  id: number
  stockCode: string
  name: string
  exchange?: string
  industry?: string
  status?: string
}

export interface DocumentChunk {
  id: number
  documentId: number
  companyId: number
  chunkIndex: number
  titlePath?: string
  content: string
  pageNo?: number
  section?: string
  tokenCount?: number
  vectorId?: string
  createdAt?: string
}

export interface Citation {
  chunkId: number
  documentId: number
  documentTitle?: string
  docType?: string
  quoteText?: string
  rankNo: number
  score?: number
  titlePath?: string
  pageNo?: number
  section?: string
}

export interface QaAskRequest {
  question: string
  companyId?: number | null
  stockCode?: string
  docTypes?: string[]
  topK?: number
  conversationId?: string | null
}

export interface QaAnswer {
  questionId: number
  question: string
  answer: string
  intentType: string
  companyId?: number
  companyName?: string
  stockCode?: string
  preferredDocTypes?: string[]
  insufficientEvidence: boolean
  citations: Citation[]
  chunks: DocumentChunk[]
  latencyMs: number
}
