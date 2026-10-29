# 星云商城（Nebula Stationery）

> 一个基于 Vue 3 + Spring Boot 3 的文具店商城系统

---

## 项目简介

星云商城是一个完整的电商系统，包含前台购物和后台管理功能。顾客可以浏览商品、加入购物车、下单付款、确认收货并评价；管理员可以管理商品和订单。

除了基础商城功能，项目还实现了优惠券、秒杀、拼团、积分、多规格商品、收货地址、退款售后、评价图片/追评/点赞、搜索联想等功能，并在功能 7 中接入了 Redis 缓存、消息队列、短信验证码、物流轨迹、Docker 部署和微服务拆分。

---

## 技术栈

### 前端
- Vue 3 + Vite + Axios
- Vue Router + Pinia
- 手写 CSS（参考 tonymoly 极简风格）

### 后端
- Java 17 + Spring Boot 3
- Spring Security + JWT
- MyBatis-Plus + MySQL 8
- Redis（缓存 + Stream 消息队列）
- Spring Cloud（Eureka + Gateway + OpenFeign，微服务版）

### 部署
- Docker / Docker Compose（MySQL + Redis + 后端 + Nginx 前端）

---

## 项目结构

```
nebula-stationery/
├── fronted/              # Vue 3 前端
├── backend/              # Spring Boot 后端（单体版，主交付）
├── cloud/                # 微服务版（Eureka + Gateway + 4 个业务服务）
├── sql/                  # 建表语句 + 初始化数据 + 各功能增量脚本
├── docker-compose.yml    # 一键部署：MySQL + Redis + 后端 + 前端
├── .env.example          # Docker 部署的环境变量模板
├── 素材/                 # 项目相关文件（logo 等）
├── docs-framework/       # 规格说明文档
│   ├── 01_PRD.md         # 产品需求文档
│   ├── 02_Architecture.md # 架构说明
│   ├── 03_Database.md    # 数据库设计
│   ├── 04_API.md         # 接口契约
│   ├── 07_Deployment.md  # Docker 部署说明
│   ├── 08_Microservices.md # 微服务拆分说明
│   └── flow/             # 业务流程图
│       ├── 全局 flow.md
│       ├── User_Auth.md
│       └── Dashboard.md
└── README.md
```

---

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/init-data.sql

# 功能 3/4、5/6、7 的增量脚本（按顺序执行一次即可）
mysql -u root -p < sql/migration-feature34.sql
mysql -u root -p < sql/migration-feature56.sql
mysql -u root -p < sql/migration-feature7.sql
```

> 脚本内部已经 `USE nebula-store`，不需要在命令行再指定库名。

### 2. 准备后端配置

```bash
cd backend/src/main/resources
cp application.yml.example application.yml
# 编辑 application.yml：填 MySQL 密码、JWT 密钥、Redis 地址、OSS / 短信 / 物流密钥（按需）
```

`application.yml` 含敏感信息，已在 `.gitignore` 中，**不要提交**。

### 3. 启动后端

需要先启动 MySQL 和 Redis（默认 127.0.0.1:6379）。Redis 不可用时后端会自动降级为进程内缓存，不会启动失败。

```bash
cd backend
mvn spring-boot:run
```

后端运行在：http://localhost:8080

### 4. 启动前端

```bash
cd fronted
npm install
npm run dev
```

前端运行在：http://localhost:5173

### 5. Docker 一键部署（可选）

```bash
cp .env.example .env
# 编辑 .env：填 DB_PASSWORD、JWT_SECRET，其余保持默认
docker compose up -d --build
```

前端 <http://localhost:8081>，后端 <http://localhost:8080>。详细说明见 [07_Deployment.md](docs-framework/07_Deployment.md)。

---

## 用户角色

| 角色 | 说明 | 核心权限 |
| --- | --- | --- |
| 游客 | 未登录 | 浏览首页、分类、商品详情 |
| 普通用户 | 自主注册 / 手机验证码登录 | 登录、加购、下单、付款、订单管理、评价 |
| 管理员 | 初始化数据固定 | 登录后台、订单管理、商品管理、中间件监控 |

---

## 核心功能

### 前台
- 首页（分类入口 + 推荐商品 + 换一换）
- 商品分类（两级分类）、商品搜索联想
- 商品详情（多规格选择 + 评价列表 + 评价图片）
- 注册与登录（账号密码 / 手机验证码，JWT 认证）
- 购物车、收货地址管理
- 下单与付款（模拟）、优惠券、积分抵扣
- 我的订单、确认收货、评价（最多 6 张图片）、追评、点赞
- 退款售后、物流轨迹时间线
- 秒杀、拼团活动页

### 后台
- 数据统计
- 订单管理（列表 + 详情 + 发货，填写快递公司和单号）
- 商品管理（新增/编辑 + 上下架 + SKU）
- 中间件监控（Redis 缓存命中率、消息队列积压与死信）

### 功能 7：Redis / 消息队列 / 微服务 / Docker

| 子项 | 实现 |
| --- | --- |
| Redis 缓存 | 商品、分类、统计接口缓存；Redis 挂了自动降级为进程内缓存并定时重连 |
| 消息队列 | Redis Stream + 消费组 + 失败重试 + 死信队列 + 延迟队列（超时未支付自动取消） |
| 短信验证码 | Provider 可切换：`mock`（控制台打印验证码） / `aliyun`（阿里云短信） |
| 真实物流 | Provider 可切换：`mock`（按时间生成轨迹） / `kuaidi100`（快递100 实时查询） |
| Docker 部署 | 多阶段构建镜像 + Compose 编排 MySQL/Redis/后端/Nginx 前端 |
| 微服务拆分 | Eureka 注册中心 + Gateway 统一鉴权 + OpenFeign 跨服务调用（`cloud/` 目录） |

---

## 配置项说明（功能 7 相关）

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| `stationery.sms.provider` | `mock` | `mock` 时验证码打印到日志并在前端提示；`aliyun` 需填 AccessKey、签名、模板 |
| `stationery.logistics.provider` | `mock` | `mock` 时按 `stage-minutes` 生成轨迹；`kuaidi100` 需填 key 和 customer |
| `stationery.logistics.mock.stage-minutes` | `1` | 模拟物流每一段的分钟数，调大观察更久 |
| `stationery.mq.poll-ms` | `1000` | 消息队列消费者轮询间隔 |

---

## 文档

- [产品需求文档](docs-framework/01_PRD.md)
- [架构说明](docs-framework/02_Architecture.md)
- [数据库设计](docs-framework/03_Database.md)
- [接口契约](docs-framework/04_API.md)
- [Docker 部署说明](docs-framework/07_Deployment.md)
- [微服务拆分说明](docs-framework/08_Microservices.md)
- [业务流程图](docs-framework/flow/)

---

## Logo

![星云商城 Logo](素材/logo.png)

---

## 许可证

MIT
