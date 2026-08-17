<template>
  <section class="qa-page">
    <div class="composer panel">
      <div class="composer-grid">
        <label class="field">
          <span>公司</span>
          <select v-model="companyId">
            <option value="">自动识别 / 不限</option>
            <option v-for="item in companies" :key="item.id" :value="String(item.id)">
              {{ item.name }}（{{ item.stockCode }}）
            </option>
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
      </div>

      <label class="field">
        <span>研究问题</span>
        <textarea
          v-model="question"
          rows="5"
          placeholder="例如：贵州茅台近一年业绩变化的主要驱动因素是什么？"
          @keydown.ctrl.enter.prevent="submit"
        />
      </label>

      <div class="actions">
        <button class="primary" type="button" :disabled="loading || !question.trim()" @click="submit">
          {{ loading ? '检索生成中…' : '提问' }}
        </button>
        <p class="hint">Ctrl + Enter 发送。支持查看历史提问。</p>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <div class="panel history-toolbar">
      <div>
        <h3>历史问答</h3>
        <p class="hint">可切换查看最近 5 条记录。</p>
      </div>
      <button class="secondary" type="button" @click="loadHistory">刷新历史</button>
    </div>

    <div v-if="history.length" class="panel thread">
      <div v-for="item in history" :key="item.questionId" class="turn" @click="openHistory(item.questionId)">
        <div class="bubble user">
          <p class="meta">你 · {{ item.createdAt || '历史记录' }}</p>
          <p>{{ item.question }}</p>
        </div>
        <div class="bubble assistant">
          <p class="meta">
            助手 · {{ item.intentType }}
            <span v-if="item.companyName"> · {{ item.companyName }}</span>
            <span> · {{ item.latencyMs }}ms</span>
            <span v-if="item.citationCount !== undefined"> · {{ item.citationCount }} 引用</span>
          </p>
          <p class="answer">{{ item.answer }}</p>
          <p v-if="item.insufficientEvidence" class="warn">依据不足：请补充相关文档后再试。</p>
        </div>
      </div>
    </div>

    <div v-if="latest" class="results">
      <section class="panel answer-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">{{ latest.intentType }}</p>
            <h3>{{ latest.companyName || '未识别公司' }} · {{ latest.stockCode || '未提供代码' }}</h3>
          </div>
          <span v-if="latest.insufficientEvidence" class="pill danger">依据不足</span>
        </div>
        <p class="overview">{{ latest.answer }}</p>
      </section>

      <section class="panel citations-panel">
        <h3>引用来源</h3>
        <p v-if="!latest.citations?.length" class="empty">暂无引用</p>
        <article v-for="cite in latest.citations" :key="`${cite.chunkId}-${cite.rankNo}`" class="cite">
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
        <article v-for="chunk in latest.chunks" :key="chunk.id" class="chunk" :id="`qa-chunk-${chunk.id}`">
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
import { onMounted, ref } from 'vue'
import { listCompanies, askQuestion } from '@/api/qa'
import { getQuestionHistory, listQuestionHistory } from '@/api/history'
import type { Company, QaAnswer, QuestionHistoryItem } from '@/types/api'

const companies = ref<Company[]>([])
const companyId = ref('')
const docTypeFilter = ref('')
const question = ref('')
const loading = ref(false)
const error = ref('')
const history = ref<QuestionHistoryItem[]>([])
const latest = ref<QaAnswer | null>(null)

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
    const result = await listQuestionHistory({ pageNum: 1, pageSize: 5 })
    history.value = result.records
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载历史记录失败'
  }
}

async function submit() {
  if (!question.value.trim() || loading.value) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    const result = await askQuestion({
      question: question.value.trim(),
      companyId: companyId.value ? Number(companyId.value) : undefined,
      docTypes: docTypeFilter.value ? [docTypeFilter.value] : undefined,
    })
    latest.value = result
    question.value = ''
    await loadHistory()
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '提问失败'
  }
  finally {
    loading.value = false
  }
}

async function openHistory(questionId: number) {
  try {
    latest.value = await getQuestionHistory(questionId)
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载历史详情失败'
  }
}

function focusChunk(chunkId: number) {
  document.getElementById(`qa-chunk-${chunkId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function docTypeLabel(type: string) {
  if (type === 'FINANCIAL_REPORT') return '财报'
  if (type === 'ANNOUNCEMENT') return '公告'
  if (type === 'RESEARCH_REPORT') return '研报'
  return type
}
</script>

<style scoped>
.qa-page {
  display: grid;
  gap: 18px;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 24px;
  background: rgba(8, 15, 31, 0.55);
}

.composer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span,
.hint,
.empty,
.overview,
.quote,
.answer,
.meta,
.path {
  color: var(--muted);
}

select,
textarea {
  width: 100%;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.72);
  color: var(--text);
  padding: 12px 14px;
}

textarea {
  resize: vertical;
  min-height: 120px;
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
  background: linear-gradient(135deg, #7dd3fc, #38bdf8);
  color: #081120;
  font-weight: 600;
  cursor: pointer;
}

.secondary {
  background: rgba(15, 23, 42, 0.72);
  color: var(--text);
  border: 1px solid var(--border);
}

.error,
.warn {
  color: #fca5a5;
}

.thread {
  display: grid;
  gap: 14px;
}

.turn {
  display: grid;
  gap: 10px;
  cursor: pointer;
}

.bubble {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 14px 16px;
}

.bubble.user {
  background: rgba(56, 189, 248, 0.08);
}

.bubble.assistant {
  background: rgba(15, 23, 42, 0.55);
}

.results {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
}

.answer-panel,
.citations-panel,
.chunks-panel {
  min-width: 0;
}

.answer-panel {
  grid-column: 1 / -1;
}

.eyebrow {
  margin: 0;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #7dd3fc;
  font-size: 0.75rem;
}

.pill,
.tag {
  font-size: 0.75rem;
  border-radius: 999px;
  padding: 2px 8px;
}

.pill.danger {
  background: rgba(248, 113, 113, 0.14);
  color: #fca5a5;
}

.tag {
  background: #7dd3fc;
  color: #081120;
}

.cite,
.chunk {
  border-top: 1px solid var(--border);
  padding: 14px 0;
}

.cite header,
.chunk header {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
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
.overview {
  white-space: pre-wrap;
  margin: 8px 0 0;
}

@media (max-width: 1080px) {
  .composer-grid,
  .results {
    grid-template-columns: 1fr;
  }
}
</style>
