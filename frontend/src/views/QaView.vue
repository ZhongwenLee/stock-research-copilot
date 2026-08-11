<template>
  <section class="panel">
    <h2>研究问答</h2>
    <p>页面壳已就绪。后续将接入问题输入、公司选择、回答面板与引用区域。</p>
    <div class="placeholder">
      <label for="question">问题预览</label>
      <textarea id="question" rows="4" placeholder="例如：贵州茅台近一年业绩变化的主要驱动因素是什么？" disabled />
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

.placeholder {
  margin-top: 20px;
  display: grid;
  gap: 8px;
}

textarea {
  width: 100%;
  resize: vertical;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: rgba(15, 23, 42, 0.7);
  color: var(--text);
  padding: 14px;
  opacity: 0.7;
}
</style>
