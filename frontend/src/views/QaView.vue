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
        <p class="hint">Ctrl + Enter 发送。多轮对话区域已预留，首版按单轮问答返回。</p>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <div v-if="history.length" class="thread panel">
      <h2>对话区（预留）</h2>
      <div v-for="item in history" :key="item.questionId" class="turn">
        <div class="bubble user">
          <p class="meta">你</p>
          <p>{{ item.question }}</p>
        </div>
        <div class="bubble assistant">
          <p class="meta">
            助手 · {{ item.intentType }}
            <span v-if="item.companyName"> · {{ item.companyName }}</span>
            <span> · {{ item.latencyMs }}ms</span>
          </p>
          <p class="answer">{{ item.answer }}</p>
          <p v-if="item.insufficientEvidence" class="warn">依据不足：请补充相关文档后再试。</p>
        </div>
      </div>
    </div>

    <div v-if="latest" class="results">
      <section class="panel">
        <h2>引用来源</h2>
        <p v-if="!latest.citations?.length" class="empty">暂无引用</p>
        <article v-for="cite in latest.citations" :key="`${cite.chunkId}-${cite.rankNo}`" class="cite">
          <header>
            <strong>[{{ cite.rankNo }}]</strong>
            <span>{{ cite.documentTitle || `文档 #${cite.documentId}` }}</span>
            <span v-if="cite.docType" class="tag">{{ docTypeLabel(cite.docType) }}</span>
          </header>
          <p v-if="cite.titlePath" class="path">{{ cite.titlePath }}<span v-if="cite.pageNo"> · p.{{ cite.pageNo }}</span></p>
          <p class="quote">{{ cite.quoteText }}</p>
        </article>
      </section>

      <section class="panel">
        <h2>检索片段</h2>
        <p v-if="!latest.chunks?.length" class="empty">暂无片段</p>
        <article v-for="chunk in latest.chunks" :key="chunk.id" class="chunk">
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
import { askQuestion, listCompanies } from '@/api/qa'
import type { Company, QaAnswer } from '@/types/api'

const companies = ref<Company[]>([])
const companyId = ref('')
const docTypeFilter = ref('')
const question = ref('')
const loading = ref(false)
const error = ref('')
const history = ref<QaAnswer[]>([])
const latest = ref<QaAnswer | null>(null)

onMounted(async () => {
  try {
    companies.value = await listCompanies()
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '加载公司列表失败'
  }
})

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
    history.value = [...history.value, result]
    latest.value = result
    question.value = ''
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : '提问失败'
  }
  finally {
    loading.value = false
  }
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

.field span {
  color: var(--muted);
  font-size: 0.9rem;
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

.actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.primary {
  border: 0;
  border-radius: 999px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #7dd3fc, #38bdf8);
  color: #081120;
  font-weight: 600;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.hint,
.empty,
.path,
.meta {
  color: var(--muted);
  margin: 0;
}

.error,
.warn {
  color: #fca5a5;
}

.thread h2,
.results h2 {
  margin: 0 0 14px;
  font-size: 1.05rem;
}

.turn {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
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

.answer {
  white-space: pre-wrap;
  margin: 8px 0 0;
}

.results {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
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
  margin-bottom: 6px;
}

.tag {
  font-size: 0.75rem;
  color: #081120;
  background: #7dd3fc;
  border-radius: 999px;
  padding: 2px 8px;
}

.quote,
.chunk p {
  margin: 6px 0 0;
  color: var(--muted);
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .composer-grid,
  .results {
    grid-template-columns: 1fr;
  }
}
</style>
