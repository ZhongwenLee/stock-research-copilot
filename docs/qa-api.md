# 问答接口契约（Step 3.1 ~ 3.6）

## 约定

- 路径前缀：`/api/v1`
- 首版：**非流式**同步返回（流式输出后续再加）
- 统一响应体：`{ code, message, data, traceId }`

## 提问

`POST /api/v1/qa/ask`

### 请求

```json
{
  "question": "贵州茅台近一年业绩变化的主要驱动因素是什么？",
  "companyId": 1,
  "stockCode": "600519",
  "docTypes": ["FINANCIAL_REPORT"],
  "topK": 8,
  "conversationId": null
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| question | 是 | 用户问题，最长 2000 |
| companyId | 否 | 目标公司 ID；为空时可从问题文本 / stockCode 解析 |
| stockCode | 否 | 股票代码覆盖 |
| docTypes | 否 | 文档范围：`FINANCIAL_REPORT` / `ANNOUNCEMENT` / `RESEARCH_REPORT` |
| topK | 否 | 重排后保留片段数，默认 8 |
| conversationId | 否 | 预留多轮；首版忽略 |

### 响应 `data`

```json
{
  "questionId": 12,
  "question": "……",
  "answer": "……[1]……[2]",
  "intentType": "QA",
  "companyId": 1,
  "companyName": "贵州茅台",
  "stockCode": "600519",
  "preferredDocTypes": ["FINANCIAL_REPORT"],
  "insufficientEvidence": false,
  "citations": [
    {
      "chunkId": 101,
      "documentId": 9,
      "documentTitle": "2024年年报",
      "docType": "FINANCIAL_REPORT",
      "quoteText": "……",
      "rankNo": 1,
      "score": 0.82,
      "titlePath": "经营情况讨论与分析",
      "pageNo": 12,
      "section": "经营情况讨论与分析"
    }
  ],
  "chunks": [],
  "latencyMs": 1860
}
```

| 字段 | 说明 |
|------|------|
| intentType | `QA` / `SUMMARY` / `COMPARE` / `AGENT` |
| insufficientEvidence | 无可用片段或模型明确表示依据不足 |
| citations | 引用来源（优先取答案中的 `[n]`） |
| chunks | 实际进入上下文的片段 |
| latencyMs | 端到端耗时 |

## 处理链路

1. 意图识别（公司 / 文档类型 / 问题类型）
2. 向量召回 + 关键词召回，RRF 融合去重
3. 启发式重排并截取 topK
4. 拼装提示词上下文（控制字符上限）
5. LLM 生成带引用的回答（无 `AI_API_KEY` 时走 Stub）
6. 落库 `question` + `citation`

## 配置

| 变量 / 配置 | 默认 | 说明 |
|-------------|------|------|
| `app.qa.recall-top-k` | 20 | 召回候选数 |
| `app.qa.rerank-top-k` | 8 | 重排保留数 |
| `app.qa.max-context-chars` | 6000 | 上下文最大字符 |
| `AI_API_KEY` | 空 | 空则 Embedding / Chat 均使用 Stub |
