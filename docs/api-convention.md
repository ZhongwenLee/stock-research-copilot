# 接口规范模板（Step 1.4）

## 1. 统一响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "a1b2c3d4e5f6"
}
```

| 字段 | 说明 |
|------|------|
| code | `0` 成功，非 0 失败 |
| message | 可读说明 |
| data | 业务数据，可空 |
| traceId | 请求追踪 ID，与响应头 `X-Trace-Id` 一致 |

## 2. 错误码

| 码 | 含义 |
|----|------|
| 0 | 成功 |
| 40000 | 错误请求 |
| 40001 | 参数校验失败 |
| 40100 | 未认证 |
| 40300 | 无权限 |
| 40400 | 资源不存在 |
| 50000 | 系统内部错误 |
| 50300 | 依赖服务不可用 |

## 3. 分页约定

请求：

```json
{
  "pageNum": 1,
  "pageSize": 20
}
```

响应 `data`：

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 20
}
```

## 4. 路径约定

- 前缀：`/api/v1`
- 文档：`http://localhost:8080/doc.html`
- 健康检查：`GET /api/v1/health`
- 文档入库接口：见 [document-ingest-api.md](document-ingest-api.md)

## 5. 后端实现位置

- `ApiResponse` / `PageQuery` / `PageResult`
- `ErrorCode` / `BizException` / `GlobalExceptionHandler`
- Knife4j / OpenAPI 配置：`OpenApiConfig`
