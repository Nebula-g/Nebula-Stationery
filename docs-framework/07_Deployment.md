# 07 部署文档（Docker Compose）

> 目标：一条命令把 MySQL、Redis、后端、前端全部拉起来，换台机器也能跑。

---

## 1. 部署结构

```
浏览器
  │  http://localhost:8081
  ▼
┌─────────────────────────────┐
│ frontend (nginx 1.27)       │  Vue 打包产物
│  /            → index.html  │
│  /api/*       → backend:8080│
│  /uploads/*   → backend:8080│
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│ backend (Spring Boot 3)     │  8080
│  Redis 缓存 + Stream 消息队列│
└──────┬───────────────┬──────┘
       ▼               ▼
┌────────────┐   ┌────────────┐
│ mysql 8.0  │   │ redis 7    │
└────────────┘   └────────────┘
```

| 服务 | 容器名 | 宿主机端口 | 说明 |
| --- | --- | --- | --- |
| frontend | nebula-frontend | 8081 | nginx 托管前端并反向代理接口 |
| backend | nebula-backend | 8080 | Spring Boot 应用 |
| mysql | nebula-mysql | 3306 | 首次启动自动建表 + 初始化数据 |
| redis | nebula-redis | 6379 | 缓存 + 消息队列，AOF 持久化 |

---

## 2. 前置条件

- Docker Desktop（Windows 建议开启 WSL2 后端）
- 无需本机装 JDK / Node / MySQL，全部在镜像里

---

## 3. 启动步骤

```bash
# 1. 进入项目根目录
cd Store

# 2. 准备环境变量（.env 已被 .gitignore 忽略）
cp .env.example .env
# 编辑 .env：填 DB_PASSWORD、JWT_SECRET

# 3. 构建并启动（首次会下载依赖，约 3~10 分钟）
docker compose up -d --build

# 4. 查看状态
docker compose ps
docker compose logs -f backend
```

启动完成后：

- 商城前台：http://localhost:8081
- 后端接口：http://localhost:8080/api/products
- 管理员账号：`admin / 123456`

> 首次启动 MySQL 会自动执行 `sql/` 下的建表、增量脚本和初始化数据；
> 之后重启不会重复执行（数据在 `mysql-data` 卷里）。

---

## 4. 常用运维命令

```bash
docker compose stop            # 停止，保留数据
docker compose start           # 再启动
docker compose down            # 删除容器（保留数据卷）
docker compose down -v         # 连数据一起删，下次启动重新初始化
docker compose logs backend    # 看后端日志
docker compose restart backend # 改完配置只重启后端
```

只改代码不重建镜像时，可以本地跑后端连容器里的 MySQL/Redis：

```bash
docker compose up -d mysql redis
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

---

## 5. 环境变量说明

| 变量 | 必填 | 作用 |
| --- | --- | --- |
| `DB_PASSWORD` | 是 | MySQL root 密码，同时传给后端 |
| `JWT_SECRET` | 是 | 登录令牌签名密钥，建议 32 位以上随机串 |
| `SMS_PROVIDER` | 否 | `mock`（默认）/ `aliyun` |
| `LOGISTICS_PROVIDER` | 否 | `mock`（默认）/ `kuaidi100` |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BUCKET_NAME` | 否 | 不填则评价图片存容器内 `/app/uploads`（挂载到 uploads 卷） |

短信、物流、OSS 的完整密钥都只从环境变量注入，镜像和仓库里不出现明文。

---

## 6. 镜像构建细节

### 后端 `backend/Dockerfile`

两阶段构建：

1. `maven:3.9-eclipse-temurin-17` 里 `mvn package -DskipTests`
2. `eclipse-temurin:17-jre` 只拷贝 jar，镜像更小

`backend/.dockerignore` 排除了 `application.yml`（含本机数据库密码），
容器里改用 `application-docker.yml` 配置档案，配置项全部来自环境变量。

### 前端 `fronted/Dockerfile`

1. `node:20-alpine` 里 `npm ci && npm run build`
2. `nginx:1.27-alpine` 托管 `dist/`，`nginx.conf` 负责 SPA 路由回退和 `/api` 反代

---

## 7. 常见问题

**Q：首次启动 backend 一直重启？**
MySQL 初始化需要 20~40 秒，compose 已配置 healthcheck 等它就绪。若仍失败，
`docker compose logs mysql` 看是否有 SQL 报错，注意 `sql/` 脚本需按文件名顺序执行。

**Q：端口被占用？**
本机已经在跑 `npm run dev`（5173）或本地后端（8080）时，先停掉再 `docker compose up`；
前端映射用的是 8081，就是为了不和开发服务器抢端口。

**Q：想换成微服务版本？**
见 [08_Microservices.md](08_Microservices.md)，微服务版使用 Eureka + Gateway，
网关端口 8090，服务端口 8081~8084。
