# 领域模型草案（Step 0.2）

## 1. 对象总览

```text
Company 1 ─── n Document 1 ─── n DocumentChunk
                │
                └── DocumentSource（可选，记录原始来源）

Question 1 ─── n Citation ───> DocumentChunk
ResearchSummary 1 ─── n Citation ───> DocumentChunk
```

## 2. 核心对象字段

### 2.1 Company（公司）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| stockCode | String | 股票代码，如 `600519` |
| name | String | 公司名称 |
| exchange | String | 交易所，如 `SH` / `SZ` |
| industry | String | 行业（可选） |
| status | String | `ACTIVE` / `INACTIVE` |
| createdAt / updatedAt | DateTime | 时间戳 |

### 2.2 Document（文档）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| companyId | Long | 所属公司 |
| title | String | 文档标题 |
| docType | String | `FINANCIAL_REPORT` / `ANNOUNCEMENT` / `RESEARCH_REPORT` |
| fileName | String | 原始文件名 |
| fileExt | String | 扩展名 |
| fileSize | Long | 字节数 |
| storagePath | String | 本地或对象存储路径 |
| publishDate | LocalDate | 文档发布日期（可空） |
| processStatus | String | `UPLOADED` / `PARSING` / `CHUNKING` / `EMBEDDING` / `READY` / `FAILED` |
| errorMessage | String | 失败原因 |
| createdAt / updatedAt | DateTime | 时间戳 |

### 2.3 DocumentSource（文档来源）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| documentId | Long | 关联文档 |
| sourceType | String | `UPLOAD` / `URL` / `MANUAL` |
| sourceUrl | String | 来源 URL（可空） |
| sourceName | String | 来源名称（可空） |
| rawMeta | String | JSON 扩展元数据 |

### 2.4 DocumentChunk（知识片段）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| documentId | Long | 所属文档 |
| companyId | Long | 公司维度冗余，便于查询 |
| chunkIndex | Integer | 文档内序号 |
| titlePath | String | 标题路径，如 `三、风险因素/1.经营风险` |
| content | String | 片段正文 |
| pageNo | Integer | 页码（可空） |
| section | String | 章节（可空） |
| tokenCount | Integer | 预估 token |
| vectorId | String | 向量库中的向量 ID |
| createdAt / updatedAt | DateTime | 时间戳 |

### 2.5 Question（问答记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| companyId | Long | 目标公司（可空） |
| questionText | String | 用户问题 |
| answerText | String | 模型回答 |
| intentType | String | `QA` / `SUMMARY` / `COMPARE` / `AGENT` |
| latencyMs | Long | 总耗时 |
| createdAt | DateTime | 创建时间 |

### 2.6 Citation（引用）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| refType | String | `QUESTION` / `SUMMARY` |
| refId | Long | 关联问答或摘要 ID |
| chunkId | Long | 引用片段 |
| quoteText | String | 引用摘录 |
| rankNo | Integer | 展示顺序 |
| score | Double | 相关分（可空） |

### 2.7 ResearchSummary（研究摘要）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| companyId | Long | 目标公司 |
| mode | String | `QUICK` / `DEEP` |
| overview | String | 公司概况 |
| businessChange | String | 经营变化 |
| financials | String | 财务指标 |
| risks | String | 风险 |
| institutionViews | String | 机构观点 |
| focusPoints | String | 关注点 |
| latencyMs | Long | 耗时 |
| createdAt | DateTime | 创建时间 |

## 3. 关联关系

- 一个公司拥有多篇文档。
- 一篇文档对应一条来源记录（可扩展为多条）。
- 一篇文档切分为多个 chunk；chunk 通过 `vectorId` 对齐向量库。
- 一次问答 / 一份摘要可引用多个 chunk，通过 `Citation` 关联。

## 4. 状态约定

文档处理状态单向推进：

`UPLOADED → PARSING → CHUNKING → EMBEDDING → READY`

任意阶段失败进入 `FAILED`，并写入 `errorMessage`。
