# 本地基础设施说明

## 启动

在项目根目录执行：

```bash
docker compose up -d
```

默认服务：

- MySQL: `127.0.0.1:3306`，账号 `root/root`，库名 `stock_research_copilot`
- Redis: `127.0.0.1:6379`，无密码

## 停止

```bash
docker compose down
```

数据卷位于 `infra/mysql/data` 与 `infra/redis/data`。
