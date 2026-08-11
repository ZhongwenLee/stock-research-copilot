<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <p class="brand-mark">SRC</p>
        <div>
          <p class="brand-name">Stock Research Copilot</p>
          <p class="brand-sub">智能个股研究助手</p>
        </div>
      </div>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ 'is-active': isActive(item.to) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <div class="content">
      <header class="topbar">
        <h1>{{ pageTitle }}</h1>
        <p>面向财报 / 公告 / 研报的研究工作台</p>
      </header>
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { to: '/', label: '首页' },
  { to: '/documents', label: '文档管理' },
  { to: '/qa', label: '研究问答' },
  { to: '/summary', label: '研究摘要' },
]

const pageTitle = computed(() => {
  return typeof route.meta.title === 'string' ? route.meta.title : '工作台'
})

function isActive(path: string) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  min-height: 100vh;
}

.sidebar {
  padding: 28px 20px;
  border-right: 1px solid var(--border);
  background: rgba(8, 15, 31, 0.88);
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 32px;
}

.brand-mark {
  margin: 0;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #081120;
  background: linear-gradient(135deg, #7dd3fc, #38bdf8);
}

.brand-name {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text);
}

.brand-sub {
  margin: 4px 0 0;
  font-size: 0.78rem;
  color: var(--muted);
}

.nav {
  display: grid;
  gap: 8px;
}

.nav-link {
  padding: 12px 14px;
  border-radius: 12px;
  color: var(--muted);
  text-decoration: none;
  border: 1px solid transparent;
  transition: 0.2s ease;
}

.nav-link:hover,
.nav-link.is-active {
  color: var(--text);
  background: rgba(56, 189, 248, 0.12);
  border-color: rgba(125, 211, 252, 0.22);
}

.content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  padding: 28px 32px 12px;
}

.topbar h1 {
  margin: 0;
  font-size: 1.7rem;
  letter-spacing: -0.03em;
}

.topbar p {
  margin: 8px 0 0;
  color: var(--muted);
}

.main {
  padding: 12px 32px 32px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: none;
    border-bottom: 1px solid var(--border);
  }

  .nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
