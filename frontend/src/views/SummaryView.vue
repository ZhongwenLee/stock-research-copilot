<template>
  <section class="panel">
    <h2>研究摘要</h2>
    <p>页面壳已就绪。后续将支持极速 / 深度摘要，并展示结构化结果与引用跳转。</p>
    <div class="modes">
      <button type="button" disabled>极速摘要</button>
      <button type="button" disabled>深度摘要</button>
    </div>
  </section>
</template>

<style scoped>
.panel {
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 28px;
  background: rgba(8, 15, 31, 0.55);
}

h2 {
  margin: 0;
}

p {
  color: var(--muted);
}

.modes {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

button {
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.7);
  color: var(--muted);
  border-radius: 999px;
  padding: 10px 16px;
  cursor: not-allowed;
}
</style>
