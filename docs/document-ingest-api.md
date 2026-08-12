# 文档入库接口说明（Step 2.1 ~ 2.6）

## 公司

- `POST /api/v1/companies` — 创建公司
- `GET /api/v1/companies` — 公司列表
- `GET /api/v1/companies/{id}` — 公司详情

## 文档

- `POST /api/v1/documents/upload` — 上传文档（multipart）
  - 参数：`companyId`、`docType`（`FINANCIAL_REPORT` / `ANNOUNCEMENT` / `RESEARCH_REPORT`）、`title?`、`publishDate?`、`file`
  - 支持：`pdf` / `docx` / `txt` / `html` / `htm`
- `GET /api/v1/documents/{id}` — 文档详情（含 `processStatus`）
- `GET /api/v1/documents` — 分页查询（`companyId` / `docType` / `processStatus`）
- `POST /api/v1/documents/{id}/reprocess` — 重新解析入库

处理状态：`UPLOADED → PARSING → CHUNKING → EMBEDDING → READY`，失败为 `FAILED`。

## 知识片段

- `GET /api/v1/chunks/{id}` — 片段详情
- `GET /api/v1/chunks` — 分页查询
  - `documentId` / `companyId` / `startDate` / `endDate`（按文档 `publishDate` 过滤）/ `pageNum` / `pageSize`

## 向量与 Embedding

- 默认：`VECTOR_PROVIDER=memory` + 无 `AI_API_KEY` 时使用本地 Stub Embedding
- 生产：配置 `AI_API_KEY`，并将 `VECTOR_PROVIDER=milvus`、`MILVUS_ENABLED=true`
