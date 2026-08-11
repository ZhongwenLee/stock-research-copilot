<template>
  <section class="panel">
    <h2>文档管理</h2>
    <p>页面壳已就绪。后续将接入上传、筛选、处理状态与片段查看。</p>
    <ul>
      <li>支持文档类型：财报 / 公告 / 研报</li>
      <li>支持筛选维度：公司、类型、时间</li>
      <li>展示解析与向量入库状态</li>
    </ul>
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

p,
li {
  color: var(--muted);
}

ul {
  margin: 18px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
}
</style>
