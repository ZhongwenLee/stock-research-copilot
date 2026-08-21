# Step 7.4 性能优化方案

## 目标

在核心 RAG 链路稳定后，降低入库与问答延迟，减轻热点读路径压力。

## 已落地优化

### 1. Embedding 批量处理

- 配置项：`app.ingest.embedding-batch-size`（默认 16）
- 入库时按批次调用 Embedding API，并配合 `embedding-max-retries` 重试
- 指标：`rag.embed` timer

### 2. 召回与重排序效率

- 向量召回与关键词召回并行执行（`CompletableFuture`），再做 RRF 融合
- 召回规模由 `app.qa.recall-top-k` 控制，重排后保留 `app.qa.rerank-top-k`
- 重排前按缺失 chunkId 批量补全内容，避免 N+1
- 指标：`rag.retrieve`、`rag.rerank`

### 3. 热点数据缓存

- 启用 Spring Cache；Redis 可用时走 Redis，否则回退内存缓存（测试/本地）
- 缓存键：
  - `companies`：公司详情与全量列表（TTL 约 30 分钟）
  - `readyDocs`：公司 READY 文档 ID 集合（TTL 约 5 分钟）
- 公司创建后清空 `companies` 缓存；文档入库成功后清空对应 `readyDocs`

### 4. 文档入库异步化

- `app.ingest.async-enabled=true` 时，上传事务提交后通过 `documentIngestExecutor` 异步解析/切分/向量化
- 指标：`rag.ingest`；失败打 `ALERT ingest_failed` 日志便于告警对接

## 建议运维调参

| 场景 | 建议 |
|------|------|
| Embedding 限流 | 下调 `embedding-batch-size` 到 8 |
| 召回慢 | 下调 `recall-top-k`，或收紧公司/文档类型过滤 |
| 上下文过长 | 下调 `max-context-chars` |
| Redis 不可用 | 自动回退内存缓存；生产建议保证 Redis 可用 |

## 后续可选项

- 关键词检索改为全文索引（MySQL FULLTEXT / Elasticsearch）
- Embedding 批次间有限并发
- 查询改写与语义缓存（相同问题短 TTL）
