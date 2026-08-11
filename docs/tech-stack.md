# 技术方案与环境约定（Step 0.3）

## 1. 技术选型

| 层级 | 选型 | 说明 |
|------|------|------|
| 后端 | Java 17 + Spring Boot | Web / Validation / Security / Redis / WebSocket |
| ORM | MyBatis Plus | 业务表 CRUD |
| 接口文档 | Knife4j（OpenAPI 3） | 本地调试与联调 |
| 前端 | Vue 3 + TypeScript + Vite | Pinia + Vue Router + Element Plus + Axios |
| 关系库 | MySQL 8 | 元数据、问答与摘要记录 |
| 缓存 | Redis 6+ | 会话、热点缓存、任务状态 |
| 向量库 | Milvus（首版） | chunk embedding 存储与召回；后续可替换 pgvector |
| 表迁移 | Flyway | SQL 版本化迁移 |
| Embedding | OpenAI 兼容接口 / 国内云 Embedding | 通过配置切换 |
| Rerank | 可选云端 Rerank API | 首版可先用启发式排序兜底 |
| LLM | OpenAI 兼容 Chat Completions | 问答、摘要、Agent 编排 |

## 2. 本地端口约定

| 服务 | 端口 |
|------|------|
| 前端 Vite | `5173` |
| 后端 Spring Boot | `8080` |
| MySQL | `3306` |
| Redis | `6379` |
| Milvus | `19530` |
| Knife4j 文档 | `http://localhost:8080/doc.html` |

## 3. 配置文件约定

```text
backend/src/main/resources/
├── application.yaml          # 公共默认配置
├── application-dev.yaml      # 本地开发（默认激活）
└── db/migration/             # Flyway SQL
```

环境变量优先于配置文件，命名统一使用大写下划线：

| 变量 | 用途 |
|------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | MySQL |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis |
| `AI_API_KEY` / `AI_BASE_URL` / `AI_CHAT_MODEL` / `AI_EMBEDDING_MODEL` | 模型 |
| `MILVUS_HOST` / `MILVUS_PORT` | 向量库 |
| `FILE_STORAGE_PATH` | 上传文件本地目录 |

## 4. 启动方式

### 4.1 基础设施（Docker）

项目根目录：

```bash
docker compose up -d
```

会启动 MySQL、Redis（Milvus 作为可选 profile 后续接入）。

### 4.2 后端

```bash
cd backend
mvn spring-boot:run
```

默认 `spring.profiles.active=dev`。

### 4.3 前端

```bash
cd frontend
npm install
npm run dev
```

前端通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`。

## 5. 开发约定

- API 统一前缀：`/api/v1`
- 统一响应体：`{ code, message, data, traceId }`
- 成功码：`0`
- 业务错误使用明确错误码，见接口规范
- 日志使用 JSON 友好格式，关键链路打印耗时

## 6. 测试环境约定

- 测试库名：`stock_research_copilot_test`
- 不连接真实收费模型时，可用 mock / 录制响应做联调
- CI 可只起 MySQL + Redis，向量与模型使用 stub
