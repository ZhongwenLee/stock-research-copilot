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

export interface SummarySection {
  title: string
  content: string
  charCount?: number
  citationCount?: number
}

export interface QaAskRequest {
  question: string
  companyId?: number | null
  stockCode?: string
  docTypes?: string[]
  topK?: number
  conversationId?: string | null
  summaryMode?: string | null
  startDate?: string | null
  endDate?: string | null
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

export interface SummaryAnswer {
  summaryId: number
  companyId: number
  companyName?: string
  stockCode?: string
  mode: string
  title: string
  overview: string
  sections: SummarySection[]
  citations: Citation[]
  chunks: DocumentChunk[]
  docTypes?: string[]
  startDate?: string | null
  endDate?: string | null
  latencyMs: number
  insufficientEvidence: boolean
}
