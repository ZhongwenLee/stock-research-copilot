<template>
  <section class="documents-page">
    <div class="panel toolbar">
      <div class="hero">
        <div>
          <p class="eyebrow">Document Center</p>
          <h2>文档管理</h2>
          <p class="lead">管理财报、公告、研报，快速查看处理状态、原始文件和解析片段。</p>
        </div>
        <div class="stats">
          <div>
            <strong>{{ pagination.total }}</strong>
            <span>文档总数</span>
          </div>
          <div>
            <strong>{{ documents.filter((item) => item.processStatus === 'DONE').length }}</strong>
            <span>已处理</span>
          </div>
        </div>
      </div>

      <div class="filters-grid">
        <label class="field">
          <span>公司</span>
          <select v-model="filters.companyId">
            <option value="">全部公司</option>
            <option v-for="item in companies" :key="item.id" :value="String(item.id)">
              {{ item.name }}（{{ item.stockCode }}）
            </option>
          </select>
        </label>

        <label class="field">
          <span>类型</span>
          <select v-model="filters.docType">
            <option value="">全部类型</option>
            <option value="FINANCIAL_REPORT">财报</option>
            <option value="ANNOUNCEMENT">公告</option>
            <option value="RESEARCH_REPORT">研报</option>
          </select>
        </label>

        <label class="field">
          <span>状态</span>
          <select v-model="filters.processStatus">
            <option value="">全部状态</option>
            <option value="UPLOADED">已上传</option>
            <option value="PROCESSING">处理中</option>
            <option value="DONE">已完成</option>
            <option value="FAILED">失败</option>
          </select>
        </label>

        <label class="field">
          <span>关键词</span>
          <input v-model="filters.keyword" placeholder="按标题搜索" @keydown.enter.prevent="loadDocuments" />
        </label>

        <label class="field">
          <span>起始日期</span>
          <input v-model="filters.startDate" type="date" />
        </label>

        <label class="field">
          <span>结束日期</span>
          <input v-model="filters.endDate" type="date" />
        </label>
      </div>

      <div class="actions">
        <button class="primary" type="button" :disabled="loading" @click="loadDocuments">
          {{ loading ? '加载中…' : '查询' }}
        </button>
        <button class="secondary" type="button" @click="resetFilters">重置</button>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <div class="grid">
      <section class="panel list-panel">
        <div class="section-head">
          <h3>文档列表</h3>
          <span class="muted">第 {{ pagination.pageNum }} 页 / 共 {{ pageCount }} 页</span>
        </div>

        <div v-if="!documents.length && !loading" class="empty-state">
          <strong>暂无文档</strong>
          <span>试试调整筛选条件或先上传文档。</span>
        </div>

        <article
          v-for="item in documents"
          :key="item.id"
          class="doc-card"
          :class="{ active: selectedDocument?.id === item.id }"
          @click="selectDocument(item)"
        >
          <div class="doc-main">
            <div>
              <h4>{{ item.title }}</h4>
              <p>{{ docMeta(item) }}</p>
            </div>
            <div class="pill-group">
              <span class="pill">{{ docTypeLabel(item.docType) }}</span>
              <span class="pill status" :class="statusClass(item.processStatus)">
                {{ statusLabel(item.processStatus) }}
              </span>
            </div>
          </div>
          <p v-if="item.errorMessage" class="error-text">{{ item.errorMessage }}</p>
        </article>

        <div class="pagination" v-if="pagination.total > pagination.pageSize">
          <button type="button" :disabled="pagination.pageNum <= 1" @click="changePage(pagination.pageNum - 1)">上一页</button>
          <span>{{ pagination.pageNum }} / {{ pageCount }}</span>
          <button type="button" :disabled="pagination.pageNum >= pageCount" @click="changePage(pagination.pageNum + 1)">下一页</button>
        </div>
      </section>

      <section class="panel detail-panel">
        <template v-if="selectedDocument">
          <div class="detail-header">
            <div>
              <p class="eyebrow">Document Detail</p>
              <h3>{{ selectedDocument.title }}</h3>
              <p class="muted">{{ selectedDocument.fileName }} · {{ selectedDocument.docType }}</p>
            </div>
            <div class="detail-actions">
              <a class="secondary link" :href="documentFileUrl(selectedDocument.id)" target="_blank" rel="noreferrer">查看原始文件</a>
              <button class="secondary" type="button" @click="loadChunks(selectedDocument.id)">查看片段</button>
              <button class="secondary" type="button" @click="handleReprocess(selectedDocument.id)">重新处理</button>
            </div>
          </div>

          <div class="detail-grid">
            <div><span>公司</span><strong>{{ selectedCompanyName }}</strong></div>
            <div><span>发布日</span><strong>{{ selectedDocument.publishDate || '—' }}</strong></div>
            <div><span>状态</span><strong>{{ statusLabel(selectedDocument.processStatus) }}</strong></div>
            <div><span>大小</span><strong>{{ formatFileSize(selectedDocument.fileSize) }}</strong></div>
          </div>

          <div class="chunk-toolbar">
            <h4>解析片段</h4>
            <span class="muted">{{ chunks.length }} 条</span>
          </div>

          <div v-if="selectedChunk" class="chunk-view">
            <div class="chunk-head">
              <strong>#{{ selectedChunk.chunkIndex }}</strong>
              <span>{{ selectedChunk.titlePath || selectedChunk.section || '未命名片段' }}</span>
            </div>
            <p class="chunk-content">{{ selectedChunk.content }}</p>
          </div>

          <div class="chunk-list">
            <button
              v-for="chunk in chunks"
              :key="chunk.id"
              class="chunk-item"
              :class="{ active: selectedChunk?.id === chunk.id }"
              type="button"
              @click="selectedChunk = chunk"
            >
              <span>#{{ chunk.chunkIndex }}</span>
              <small>{{ chunk.titlePath || chunk.section || '未命名片段' }}</small>
            </button>
          </div>
        </template>

        <div v-else class="empty-state large">
          <strong>选择一条文档查看详情</strong>
          <span>这里会展示原始文件入口与解析片段。</span>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { listCompanies } from '@/api/qa'
import { getDocumentFileUrl, listDocuments, reprocessDocument } from '@/api/documents'
import { listChunksByDocument } from '@/api/chunks'
import type { Company, DocumentChunk, DocumentVO, PageResult } from '@/types/api'

const companies = ref<Company[]>([])
const documents = ref<DocumentVO[]>([])
const selectedDocument = ref<DocumentVO | null>(null)
const selectedChunk = ref<DocumentChunk | null>(null)
const chunks = ref<DocumentChunk[]>([])
const loading = ref(false)
const error = ref('')
const selectedCompanyName = computed(() => {
  const company = companies.value.find((item) => item.id === selectedDocument.value?.companyId)
  return company ? `${company.name}（${company.stockCode}）` : '—'
})

const filters = reactive({
  companyId: '',
  docType: '',
  processStatus: '',
  keyword: '',
  startDate: '',
  endDate: '',
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const pageCount = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.pageSize)))

onMounted(async () => {
  await Promise.all([loadCompanies(), loadDocuments()])
})

async function loadCompanies() {
  try {
    companies.value = await listCompanies()
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载公司列表失败'
  }
}

async function loadDocuments() {
  loading.value = true
  error.value = ''
  try {
    const result = await listDocuments({
      companyId: filters.companyId ? Number(filters.companyId) : undefined,
      docType: filters.docType || undefined,
      processStatus: filters.processStatus || undefined,
      keyword: filters.keyword || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }) as PageResult<DocumentVO>
    documents.value = result.records
    pagination.total = result.total
    if (!selectedDocument.value && result.records.length) {
      await selectDocument(result.records[0])
    }
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载文档失败'
  }
  finally {
    loading.value = false
  }
}

async function selectDocument(item: DocumentVO) {
  selectedDocument.value = item
  selectedChunk.value = null
  await loadChunks(item.id)
}

async function loadChunks(documentId: number) {
  try {
    chunks.value = await listChunksByDocument(documentId)
    selectedChunk.value = chunks.value[0] ?? null
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载片段失败'
  }
}

async function handleReprocess(id: number) {
  try {
    await reprocessDocument(id)
    await loadDocuments()
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '重新处理失败'
  }
}

function changePage(pageNum: number) {
  pagination.pageNum = pageNum
  void loadDocuments()
}

function resetFilters() {
  filters.companyId = ''
  filters.docType = ''
  filters.processStatus = ''
  filters.keyword = ''
  filters.startDate = ''
  filters.endDate = ''
  pagination.pageNum = 1
  void loadDocuments()
}

function docTypeLabel(value?: string) {
  if (value === 'FINANCIAL_REPORT') return '财报'
  if (value === 'ANNOUNCEMENT') return '公告'
  if (value === 'RESEARCH_REPORT') return '研报'
  return value || '未知'
}

function statusLabel(value?: string) {
  if (value === 'UPLOADED') return '已上传'
  if (value === 'PROCESSING') return '处理中'
  if (value === 'DONE') return '已完成'
  if (value === 'FAILED') return '失败'
  return value || '未知'
}

function statusClass(value?: string) {
  return {
    success: value === 'DONE',
    warning: value === 'PROCESSING' || value === 'UPLOADED',
    danger: value === 'FAILED',
  }
}

function docMeta(item: DocumentVO) {
  const size = formatFileSize(item.fileSize)
  const publishDate = item.publishDate || '未填写发布日期'
  return `${item.fileExt || 'unknown'} · ${size} · ${publishDate}`
}

function formatFileSize(value?: number) {
  if (!value && value !== 0) return '—'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function documentFileUrl(id: number) {
  return getDocumentFileUrl(id)
}
</script>

<style scoped>
.documents-page {
  display: grid;
  gap: 18px;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 24px;
  background: rgba(8, 15, 31, 0.55);
}

.toolbar {
  display: grid;
  gap: 18px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #7dd3fc;
  font-size: 0.75rem;
}

h2,
h3,
h4 {
  margin: 8px 0 0;
}

.lead,
.muted,
.error,
.error-text,
.empty-state span,
.doc-main p,
.chunk-content {
  color: var(--muted);
}

.stats {
  display: flex;
  gap: 12px;
}

.stats div {
  min-width: 120px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.72);
}

.stats strong {
  display: block;
  font-size: 1.5rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  color: var(--muted);
  font-size: 0.9rem;
}

select,
input {
  width: 100%;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.72);
  color: var(--text);
  padding: 12px 14px;
}

.actions,
.detail-actions,
.chunk-toolbar,
.section-head,
.detail-header,
.doc-main,
.chunk-head,
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.primary,
.secondary,
.link {
  border-radius: 999px;
  padding: 10px 18px;
  border: 1px solid var(--border);
  text-decoration: none;
  cursor: pointer;
}

.primary {
  border: 0;
  background: linear-gradient(135deg, #7dd3fc, #38bdf8);
  color: #081120;
  font-weight: 600;
}

.secondary {
  background: rgba(15, 23, 42, 0.72);
  color: var(--text);
}

.grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 18px;
}

.list-panel,
.detail-panel {
  min-width: 0;
}

.doc-card {
  margin-top: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.5);
  cursor: pointer;
}

.doc-card.active {
  border-color: rgba(125, 211, 252, 0.4);
  box-shadow: 0 0 0 1px rgba(125, 211, 252, 0.12) inset;
}

.doc-main h4,
.chunk-head strong {
  margin: 0;
}

.pill-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(125, 211, 252, 0.12);
  color: #7dd3fc;
  font-size: 0.75rem;
}

.pill.status.success {
  background: rgba(74, 222, 128, 0.14);
  color: #4ade80;
}

.pill.status.warning {
  background: rgba(251, 191, 36, 0.14);
  color: #fbbf24;
}

.pill.status.danger {
  background: rgba(248, 113, 113, 0.14);
  color: #f87171;
}

.empty-state {
  margin-top: 16px;
  padding: 28px;
  border: 1px dashed var(--border);
  border-radius: 18px;
  display: grid;
  gap: 8px;
  justify-items: center;
  text-align: center;
}

.empty-state.large {
  min-height: 360px;
  align-content: center;
}

.detail-header {
  align-items: flex-start;
}

.detail-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid div {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.5);
}

.detail-grid span {
  display: block;
  color: var(--muted);
  font-size: 0.85rem;
  margin-bottom: 6px;
}

.chunk-toolbar {
  margin-top: 18px;
}

.chunk-view {
  margin-top: 12px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.5);
}

.chunk-content {
  white-space: pre-wrap;
}

.chunk-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.chunk-item {
  text-align: left;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.45);
  color: var(--text);
  cursor: pointer;
  display: grid;
  gap: 4px;
}

.chunk-item.active {
  border-color: rgba(125, 211, 252, 0.4);
  background: rgba(56, 189, 248, 0.12);
}

.pagination {
  margin-top: 18px;
  justify-content: center;
}

.pagination button {
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(15, 23, 42, 0.7);
  color: var(--text);
}

.error,
.error-text {
  margin: 0;
}

@media (max-width: 1080px) {
  .grid,
  .filters-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }
}
</style>
