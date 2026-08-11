<template>
  <section class="panel">
    <p class="eyebrow">Stock Research Copilot</p>
    <h2>把个股研究链路跑通</h2>
    <p class="lead">
      当前工程骨架已就绪：文档管理、研究问答、研究摘要页面已可导航，后端统一接口规范与健康检查可用。
    </p>

    <div class="grid">
      <article v-for="item in cards" :key="item.title">
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
        <RouterLink :to="item.to">进入</RouterLink>
      </article>
    </div>

    <div class="health">
      <span>后端状态</span>
      <strong :class="{ ok: healthOk, bad: healthOk === false }">{{ healthText }}</strong>
      <button type="button" @click="checkHealth">重新检测</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchHealth } from '@/api/health'

const cards = [
  { title: '文档管理', desc: '上传财报、公告、研报，查看解析与入库状态。', to: '/documents' },
  { title: '研究问答', desc: '按公司提问，返回带引用来源的回答。', to: '/qa' },
  { title: '研究摘要', desc: '生成公司概况、业绩变化、风险与机构观点。', to: '/summary' },
]

const healthOk = ref<boolean | null>(null)
const healthText = ref('检测中...')

async function checkHealth() {
  healthText.value = '检测中...'
  healthOk.value = null
  try {
    const data = await fetchHealth()
    healthOk.value = data.status === 'UP'
    healthText.value = healthOk.value ? `UP · ${data.service}` : '异常'
  } catch {
    healthOk.value = false
    healthText.value = '无法连接后端 (http://localhost:8080)'
  }
}

onMounted(checkHealth)
</script>

<style scoped>
.panel {
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 28px;
  background: rgba(8, 15, 31, 0.55);
}

.eyebrow {
  margin: 0;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: #7dd3fc;
  font-size: 0.75rem;
}

h2 {
  margin: 12px 0 0;
  font-size: clamp(1.8rem, 4vw, 2.6rem);
  letter-spacing: -0.04em;
}

.lead {
  margin: 14px 0 0;
  max-width: 720px;
  color: var(--muted);
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

article {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.55);
}

article h3 {
  margin: 0 0 8px;
  font-size: 1.05rem;
}

article p {
  margin: 0 0 14px;
  color: var(--muted);
}

article a {
  color: #7dd3fc;
  text-decoration: none;
}

.health {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  color: var(--muted);
}

.health strong.ok {
  color: #4ade80;
}

.health strong.bad {
  color: #f87171;
}

.health button {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
