<template>
  <section class="agent-page">
    <div class="panel composer">
      <div class="hero">
        <div>
          <p class="eyebrow">Agent Orchestration</p>
          <h2>让系统自动选择问答或摘要工具</h2>
          <p class="lead">
            输入研究问题后，系统会先识别意图，再路由到问答或摘要工具，并展示执行步骤、引用来源与结果。
          </p>
        </div>
        <div class="status-card">
          <span>当前状态</span>
          <strong>{{ loading ? '执行中' : '待命' }}</strong>
          <span v-if="latest?.latencyMs">{{ latest.latencyMs }} ms</span>
        </div>
      </div>

      <div class="controls-grid">
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

        <label class="field">
          <span>模式</span>
          <select v-model="summaryMode">
            <option value="">默认</option>
            <option value="FAST">极速摘要</option>
            <option value="DEEP">深度摘要</option>
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

      <label class="field textarea-field">
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
          {{ loading ? '执行中…' : '启动 Agent' }}
        </button>
        <p class="hint">Ctrl + Enter 发送。页面会展示路由结果、步骤和最终回答。</p>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <div v-if="latest" class="results">
      <section class="panel overview-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">{{ latest.intentType }}</p>
            <h3>{{ latest.companyName || '未识别公司' }} · {{ latest.stockCode || '未提供代码' }}</h3>
          </div>
          <div class="meta-pills">
            <span v-if="latest.preferredDocTypes?.length" class="pill">
              {{ latest.preferredDocTypes.join(' / ') }}
            </span>
            <span v-if="latest.insufficientEvidence" class="pill danger">依据不足</span>
          </div>
        </div>
        <p class="overview">{{ latest.answer }}</p>
      </section>

      <section class="panel steps-panel">
        <h3>执行步骤</h3>
        <article v-for="step in latest.steps" :key="`${step.name}-${step.tool}`" class="step-card">
          <header>
            <strong>{{ step.name }}</strong>
            <span class="tag">{{ step.tool }}</span>
            <span class="status" :class="step.status.toLowerCase()">{{ step.status }}</span>
          </header>
          <p v-if="step.note">{{ step.note }}</p>
          <p v-if="step.latencyMs !== undefined">耗时 {{ step.latencyMs }} ms</p>
        </article>
      </section>

      <section class="panel citations-panel">
        <h3>引用来源</h3>
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

      <section class="panel chunks-panel">
        <h3>检索片段</h3>
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

    <div v-if="history.length" class="panel history-panel">
      <h3>最近执行记录</h3>
      <div v-for="item in history" :key="`${item.question}-${item.latencyMs}`" class="history-item">
        <div>
          <strong>{{ item.companyName || '未识别公司' }}</strong>
          <span>{{ item.intentType }} · {{ item.latencyMs }}ms</span>
        </div>
        <p>{{ item.answer }}</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listCompanies } from '@/api/qa'
import { runAgent } from '@/api/agent'
import type { AgentAnswer, Company } from '@/types/api'

const companies = ref<Company[]>([])
const companyId = ref('')
const docTypeFilter = ref('')
const summaryMode = ref('')
const topK = ref(6)
const startDate = ref('')
const endDate = ref('')
const question = ref('')
const loading = ref(false)
const error = ref('')
const history = ref<AgentAnswer[]>([])
const latest = ref<AgentAnswer | null>(null)

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
    const result = await runAgent({
      question: question.value.trim(),
      companyId: companyId.value ? Number(companyId.value) : undefined,
      docTypes: docTypeFilter.value ? [docTypeFilter.value] : undefined,
      summaryMode: summaryMode.value || undefined,
      topK: topK.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
    })
    history.value = [result, ...history.value].slice(0, 5)
    latest.value = result
  }
  catch (e) {
    error.value = e instanceof Error ? e.message : 'Agent 执行失败'
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
.agent-page {
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
.step-card p,
.chunk p,
.history-item p,
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

.textarea-field {
  margin-top: 14px;
}

.field span {
  color: var(--muted);
  font-size: 0.9rem;
}

select,
input,
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
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.primary {
  border: 0;
  border-radius: 999px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #081120;
  font-weight: 600;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.results {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
}

.overview-panel,
.steps-panel,
.citations-panel,
.chunks-panel,
.history-panel {
  min-width: 0;
}

.overview-panel,
.history-panel {
  grid-column: 1 / -1;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.meta-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pill {
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(125, 211, 252, 0.12);
  color: #7dd3fc;
  font-size: 0.78rem;
}

.pill.danger {
  background: rgba(248, 113, 113, 0.15);
  color: #fca5a5;
}

.step-card,
.cite,
.chunk,
.history-item {
  border-top: 1px solid var(--border);
  padding: 14px 0;
}

.step-card header,
.cite header,
.chunk header,
.history-item > div {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.tag {
  font-size: 0.75rem;
  color: #081120;
  background: #7dd3fc;
  border-radius: 999px;
  padding: 2px 8px;
}

.status {
  font-size: 0.75rem;
  border-radius: 999px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--muted);
}

.status.done {
  color: #4ade80;
}

.status.failed {
  color: #f87171;
}

.quote,
.chunk p,
.step-card p,
.history-item p,
.overview {
  white-space: pre-wrap;
  margin: 8px 0 0;
}

.history-item {
  border-top: 1px solid var(--border);
  padding: 14px 0;
}

.error {
  color: #fca5a5;
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
