# 智能个股研究助手

## Stock Research Copilot Based on RAG and Agent

基于 **Java + Spring Boot + RAG + Agent** 构建的智能个股研究助手，面向股票研究场景，支持财报、公告、研报等多源文档的导入、解析、检索问答与研究摘要生成，帮助用户快速理解一家公司的基本面、业绩变化、风险因素与机构观点。

---

## 项目简介

本项目聚焦于“个股研究”这一高价值金融场景，围绕 **文档知识库构建、语义检索增强生成、Agent 工具调用、研究摘要生成** 等核心能力，打造一个面向投资研究的 AI 助手。

与普通聊天机器人不同，本项目强调：

- 对股票研究资料的结构化处理
- 基于来源文档的可信问答
- 支持财报 / 公告 / 研报等多类型资料
- 通过 Agent 自动拆解研究任务并调用工具
- 输出带引用来源的研究结论，降低幻觉风险

---

## 核心功能

### 1. 个股研究问答

- 支持按股票代码 / 公司名称发起问题
- 基于财报、公告、研报等资料进行 RAG 问答
- 返回带引用来源的回答
- 支持多轮追问与上下文连续对话

### 2. 财报与公告解读

- 自动提取关键业绩指标与变化原因
- 总结公司经营情况、风险提示与管理层表述
- 对公告内容进行摘要与事件提炼

### 3. 研报对比与观点聚合

- 聚合多篇券商研报
- 提炼一致观点与分歧点
- 总结近期市场关注焦点与催化因素

### 4. 研究摘要生成

- 一键生成个股研究备忘录
- 输出公司概况、业绩变化、核心风险、机构观点等内容
- 支持极速摘要与深度摘要两种模式

### 5. 知识库管理

- 支持文档上传、解析与入库
- 支持按公司、文档类型、时间范围管理知识
- 支持知识片段查看与来源追溯

---

## 技术栈

### 后端

- Java 17
- Spring Boot
- Spring Web
- Spring Validation
- Spring Security
- Spring Data Redis
- MyBatis Plus
- WebSocket
- Spring AI / LangChain4j
- Maven

### 数据与存储

- MySQL
- Redis
- 向量数据库（Milvus / pgvector / Elasticsearch Vector）

### AI 能力

- 大模型 API
- Embedding 模型
- Rerank 模型
- Tool Calling / Function Calling
- OCR（可选）

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- Axios

### 工程化与部署

- Git
- Docker
- Nginx
- Swagger / Knife4j
- JUnit

---

## 项目结构

```text
stock-research-copilot/
├── backend/                  # Spring Boot 后端
├── frontend/                 # Vue3 前端
├── docs/                     # 项目文档、设计稿、接口说明
├── infra/                    # 本地 MySQL / Redis 初始化
├── data/                     # 测试文档、样例资料
├── docker-compose.yml        # 本地基础设施
└── README.md
```

### 关键文档

- [开发计划](docs/development-plan.md)
- [首版范围](docs/scope.md)
- [领域模型](docs/domain-model.md)
- [技术方案与环境约定](docs/tech-stack.md)
- [接口规范](docs/api-convention.md)
- [文档入库接口](docs/document-ingest-api.md)
- [问答接口](docs/qa-api.md)
- [本地基础设施](infra/README.md)

### 后端目录示例

```text
backend/
├── src/main/java/...
│   ├── controller/
│   ├── service/
│   ├── service/impl/
│   ├── mapper/
│   ├── entity/
│   ├── dto/
│   ├── vo/
│   ├── config/
│   ├── common/
│   ├── rag/
│   ├── agent/
│   ├── parser/
│   └── job/
└── src/main/resources/
    ├── application.yml
    └── ...
```

---

## 运行环境

### 基础环境

- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8+
- Redis 6+

### 可选环境

- 向量数据库服务
- OCR 服务
- 大模型 API Key

---

## 快速开始

### 1. 克隆仓库

```bash
git clone <your-repo-url>
cd stock-research-copilot
```

### 2. 启动基础设施

```bash
docker compose up -d
```

默认 MySQL：`root/root`，库名 `stock_research_copilot`；Redis：`6379`。

### 3. 启动后端

进入后端目录：

```bash
cd backend
```

可参考 `.env.example` 配置环境变量，然后启动：

```bash
mvn spring-boot:run
```

后端默认访问地址：

```text
http://localhost:8080
```

接口文档：

```text
http://localhost:8080/doc.html
```

### 4. 启动前端

进入前端目录：

```bash
cd ../frontend
```

安装依赖并启动：

```bash
npm install
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

---

## 配置说明

### 后端配置示例

在 `backend/src/main/resources/application.yaml` 中配置：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stock_research_copilot
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
ai:
  api-key: your_api_key
  model: your_model_name
  embedding-model: your_embedding_model
```

实际配置请根据你的模型提供商和向量库类型进行调整。

---

## 核心流程

### 文档入库流程

1. 上传财报 / 公告 / 研报
2. 文档解析与清洗
3. 文本切分为 chunk
4. 生成 embedding
5. 写入向量数据库
6. 保存文档元数据与索引信息

### 问答流程

1. 用户输入问题
2. 识别问题类型与目标公司
3. 检索相关知识片段
4. 重排序召回结果
5. 大模型生成答案
6. 返回回答与引用来源

### Agent 流程

1. 识别用户意图
2. 自动选择工具
3. 调用检索 / 摘要 / 结构化分析能力
4. 汇总结果并输出研究结论

---

## 设计亮点

- RAG + Agent 双核心架构
- 面向股票研究场景的垂直化设计
- 支持文档引用，降低幻觉风险
- 支持多源资料聚合与观点对比
- 适合简历展示，具备较强业务表达能力

---

## 未来规划

- 支持更多文档格式，如 HTML、Excel、图片 OCR
- 加入混合检索与重排序优化
- 支持多 Agent 协作
- 增加研究报告导出能力
- 增加个性化关注与订阅提醒
- 增加行情数据联动分析

---

## 免责声明

本项目仅用于学习、研究与技术演示，不构成任何投资建议。

系统输出内容仅供参考，请结合真实市场信息与专业判断使用。
