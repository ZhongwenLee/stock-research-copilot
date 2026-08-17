<template>
  <section class="summary-page">
    <div class="panel composer">
      <div class="hero">
        <div>
          <p class="eyebrow">Research Summary</p>
          <h2>一键生成结构化研究摘要</h2>
          <p class="lead">
            支持极速 / 深度两种模式，自动聚合公司多篇文档，输出公司概况、经营变化、财务指标、风险与机构观点。
          </p>
        </div>
        <div class="status-card">
          <span>当前模式</span>
          <strong>{{ modeLabel }}</strong>
          <span v-if="latest?.latencyMs">{{ latest.latencyMs }} ms</span>
        </div>
      </div>

      <div class="controls-grid">
        <label class="field">
          <span>公司</span>
          <select v-model="companyId">
            <option value="">请选择公司</option>
            <option v-for="item in companies" :key="item.id" :value="String(item.id)">
              {{ item.name }}（{{ item.stockCode }}）
            </option>
          </select>
        </label>

        <label class="field">
          <span>模式</span>
          <select v-model="mode">
            <option value="FAST">极速摘要</option>
            <option value="DEEP">深度摘要</option>
          </select>
        </label>

        <label class="field">
          <span>文档范围</span>
          <select v-model="docTypeFilter">
            <option value="">全部类型</option>
            <option value="FINANCIAL_REPORT">财报</option>
            <option value="ANNOUNCEMENT">公告</option>
            <option value="RESEARCH_REPORT">研报</option>
          </select>
        </label>

        <label class="field">
          <span>TopK</span>
          <input v-model="topK" type="number" min="1" max="20" />
        </label>

        <label class="field">
          <span>起始日期</span>
          <input v-model="startDate" type="date" />
        </label>

        <label class="field">
          <span>结束日期</span>
          <input v-model="endDate" type="date" />
        </label>
      </div>

      <div class="actions">
        <button class="primary" type="button" :disabled="loading || !companyId" @click="submit">
          {{ loading ? '生成中…' : '生成摘要' }}
        </button>
        <p class="hint">支持检索最近文档并聚合为结构化章节，引用可点击跳转到片段。</p>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <div class="panel history-toolbar">
      <div>
        <h3>历史摘要</h3>
        <p class="hint">可刷新最近 5 条摘要记录。</p>
      </div>
      <button class="secondary" type="button" @click="loadHistory">刷新历史</button>
    </div>

    <div v-if="history.length" class="panel history-panel">
      <div v-for="item in history" :key="item.summaryId" class="history-item" @click="openHistory(item.summaryId)">
        <div>
          <strong>{{ item.companyName }}</strong>
          <span>{{ item.mode }} · {{ item.latencyMs }}ms · {{ item.citationCount || 0 }} 引用</span>
        </div>
        <p>{{ item.overview }}</p>
      </div>
    </div>

    <div v-if="latest" class="results">
      <section class="panel overview-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">{{ latest.title }}</p>
            <h3>{{ latest.companyName }} · {{ latest.mode === 'DEEP' ? '深度摘要' : '极速摘要' }}</h3>
          </div>
          <div class="meta-pills">
            <span v-if="latest.stockCode" class="pill">{{ latest.stockCode }}</span>
            <span v-if="latest.docTypes?.length" class="pill">{{ latest.docTypes.join(' / ') }}</span>
            <span v-if="latest.insufficientEvidence" class="pill danger">依据不足</span>
          </div>
        </div>
        <p class="overview">{{ latest.overview }}</p>
      </section>

      <section class="panel sections-panel">
        <h3>结构化章节</h3>
        <article v-for="section in latest.sections" :key="section.title" class="section-card">
          <header>
            <strong>{{ section.title }}</strong>
            <span>{{ section.charCount || 0 }} 字 · {{ section.citationCount || 0 }} 条引用</span>
          </header>
          <p>{{ section.content }}</p>
        </article>
      </section>

      <section class="panel citations-panel">
        <h3>引用来源</h3>
        <p v-if="!latest.citations?.length" class="empty">暂无引用</p>
        <article v-for="cite in latest.citations" :key="`${cite.chunkId}-${cite.rankNo}`" class="cite" :id="`summary-cite-${cite.chunkId}`">
          <header>
            <strong>[{{ cite.rankNo }}]</strong>
            <button class="link-button" type="button" @click="focusChunk(cite.chunkId)">
              {{ cite.documentTitle || `文档 #${cite.documentId}` }}
            </button>
            <span v-if="cite.docType" class="tag">{{ docTypeLabel(cite.docType) }}</span>
          </header>
          <p v-if="cite.titlePath" class="path">
            {{ cite.titlePath }}<span v-if="cite.pageNo"> · p.{{ cite.pageNo }}</span>
          </p>
          <p class="quote">{{ cite.quoteText }}</p>
        </article>
      </section>

      <section class="panel chunks-panel">
        <h3>检索片段</h3>
        <p v-if="!latest.chunks?.length" class="empty">暂无片段</p>
        <article v-for="chunk in latest.chunks" :key="chunk.id" class="chunk" :id="`summary-chunk-${chunk.id}`">
          <header>
            <strong>#{{ chunk.id }}</strong>
            <span>{{ chunk.titlePath || chunk.section || '未命名片段' }}</span>
          </header>
          <p>{{ chunk.content }}</p>
        </article>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listCompanies } from '@/api/qa'
import { generateSummary } from '@/api/summary'
import { getSummaryHistory, listSummaryHistory } from '@/api/history'
import type { Company, SummaryAnswer, SummaryHistoryItem } from '@/types/api'

const companies = ref<Company[]>([])
const companyId = ref('')
const docTypeFilter = ref('')
const mode = ref<'FAST' | 'DEEP'>('FAST')
const topK = ref(6)
const startDate = ref('')
const endDate = ref('')
const loading = ref(false)
const error = ref('')
const history = ref<SummaryHistoryItem[]>([])
const latest = ref<SummaryAnswer | null>(null)

const modeLabel = computed(() => (mode.value === 'DEEP' ? '深度摘要' : '极速摘要'))

onMounted(async () => {
  await Promise.all([loadCompanies(), loadHistory()])
})

async function loadCompanies() {
  try {
    companies.value = await listCompanies()
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载公司列表失败'
  }
}

async function loadHistory() {
  try {
    const result = await listSummaryHistory({ pageNum: 1, pageSize: 5 })
    history.value = result.records
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载摘要历史失败'
  }
}

async function submit() {
  if (!companyId.value || loading.value) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    const result = await generateSummary({
      companyId: Number(companyId.value),
      docTypes: docTypeFilter.value ? [docTypeFilter.value] : undefined,
      mode: mode.value,
      topK: topK.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
    })
    latest.value = result
    await loadHistory()
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '生成摘要失败'
  }
  finally {
    loading.value = false
  }
}

async function openHistory(summaryId: number) {
  try {
    latest.value = await getSummaryHistory(summaryId)
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载摘要详情失败'
  }
}

function focusChunk(chunkId: number) {
  document.getElementById(`summary-chunk-${chunkId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function docTypeLabel(type: string) {
  if (type === 'FINANCIAL_REPORT') return '财报'
  if (type === 'ANNOUNCEMENT') return '公告'
  if (type === 'RESEARCH_REPORT') return '研报'
  return type
}
</script>

<style scoped>
.summary-page {
  display: grid;
  gap: 18px;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 24px;
  background: rgba(8, 15, 31, 0.55);
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
h3 {
  margin: 10px 0 0;
}

.lead,
.hint,
.empty,
.overview,
.quote,
.section-card p,
.chunk p,
.history-item p,
.meta-pills,
.path {
  color: var(--muted);
}

.status-card {
  min-width: 170px;
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid var(--border);
}

.status-card strong {
  color: var(--text);
  font-size: 1.05rem;
}

.controls-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
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
.section-head,
.history-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: space-between;
}

.primary,
.secondary {
  border: 0;
  border-radius: 999px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #c084fc, #8b5cf6);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.secondary {
  background: rgba(15, 23, 42, 0.72);
  color: var(--text);
  border: 1px solid var(--border);
}

.history-panel {
  display: grid;
  gap: 12px;
}

.history-item {
  border-top: 1px solid var(--border);
  padding: 14px 0;
  cursor: pointer;
}

.history-item > div,
.section-card header,
.cite header,
.chunk header {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.results {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
}

.overview-panel,
.sections-panel,
.citations-panel,
.chunks-panel {
  min-width: 0;
}

.overview-panel {
  grid-column: 1 / -1;
}

.meta-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pill,
.tag {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.78rem;
}

.pill {
  background: rgba(125, 211, 252, 0.12);
  color: #7dd3fc;
}

.pill.danger {
  background: rgba(248, 113, 113, 0.15);
  color: #fca5a5;
}

.tag {
  background: #7dd3fc;
  color: #081120;
}

.section-card,
.cite,
.chunk {
  border-top: 1px solid var(--border);
  padding: 14px 0;
}

.link-button {
  border: 0;
  background: transparent;
  color: #7dd3fc;
  padding: 0;
  cursor: pointer;
  text-align: left;
}

.quote,
.chunk p,
.section-card p,
.history-item p,
.overview {
  white-space: pre-wrap;
  margin: 8px 0 0;
}

@media (max-width: 1080px) {
  .results,
  .controls-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }
}
</style>
