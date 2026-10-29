# 微服务拆分说明（功能 7g）

> 单体版在 `backend/`，微服务版在 `cloud/`，两套代码共用同一个数据库，
> 可以分别启动、互不干扰。前端默认对接单体版，想验证微服务版只需把
> Vite 代理指向网关端口 8090。

---

## 一、整体结构

```
                        ┌──────────────────────────┐
   浏览器 / Vue 前端 ───▶│  gateway  8090            │
                        │  ├─ 跨域 CORS            │
                        │  ├─ JWT 校验（GlobalFilter）│
                        │  ├─ 伪造的 X-User-* 头剔除 │
                        │  └─ 按路径 lb:// 转发      │
                        └────────────┬─────────────┘
                                     │ Eureka 服务发现
        ┌────────────┬───────────────┼──────────────┬─────────────┐
        ▼            ▼               ▼              ▼             │
  user-service  product-service  order-service  review-service    │
     8081           8082             8083           8084          │
        │            │                │               │           │
        └────────────┴────────┬───────┴───────────────┘           │
                              ▼                                   │
                     MySQL  nebula-store                          │
                                                                  │
                    eureka-server 8761 ◀──────────────────────────┘
```

- **eureka-server（8761）**：注册中心，所有服务启动后自动注册，网关和 Feign 靠它找实例。
- **gateway（8090）**：唯一对外的入口，负责跨域、鉴权和路由，业务服务不直接暴露给前端。
- **四个业务服务**：各自独立端口、独立的 Controller，共享一个 MySQL 库（作业演示用，真实项目通常是一服务一库）。

---

## 二、服务与接口划分

| 服务 | 端口 | 对外路径（经网关） | 说明 |
| --- | --- | --- | --- |
| user-service | 8081 | `/api/auth/**`、`/api/users/**` | 注册、登录、个人资料 |
| product-service | 8082 | `/api/products/**`、`/api/categories/**` | 商品列表/详情、分类、库存扣减 |
| order-service | 8083 | `/api/orders/**` | 下单、支付、取消、确认收货 |
| review-service | 8084 | `/api/reviews/**` | 发布评价、按商品查评价 |

服务间接口（网关**不**路由，只能内网调用）：

| 接口 | 提供方 | 调用方 | 用途 |
| --- | --- | --- | --- |
| `GET /api/internal/products/{id}` | product-service | order-service | 下单前拿价格和库存 |
| `POST /api/internal/products/deduct` | product-service | order-service | 扣库存（`stock >= ?` 条件更新，防超卖） |
| `POST /api/internal/products/restore` | product-service | order-service | 取消订单时回滚库存 |
| `GET /api/internal/orders/{id}` | order-service | review-service | 校验订单归属和「已完成」状态 |
| `GET /api/internal/users/{id}/username` | user-service | review-service | 评价列表显示用户名 |

---

## 三、鉴权是怎么跨服务的

1. 前端登录 `POST /api/auth/login`，user-service 签发 JWT（`userId`、`role` 放在 claims 里）。
2. 之后每个请求带 `Authorization: Bearer <token>`，网关的 `AuthGlobalFilter` 统一验签：
   - 白名单：`/api/auth/**`、`GET /api/products**`、`GET /api/categories**`、`GET /api/reviews/product/**`；
   - 其余请求没有有效令牌直接 401；
   - `/api/admin/**` 还要求 `role = 1`。
3. 验签通过后，网关注入 `X-User-Id`、`X-Username`、`X-User-Role` 三个请求头。
4. 业务服务不再解析 JWT，直接用 `CurrentUser.id(request)` 取当前用户。
5. 客户端自带的同名请求头会在网关被清掉，防止伪造身份。

> 这套做法叫「网关统一鉴权 + 内部信任头」，好处是业务服务不需要再依赖 Spring Security。

---

## 四、跨服务调用（Feign）

`order-service` 下单时的流程：

```
OrderController.create
  └─ OrderService.create
       ├─ ProductClient.detail(productId)   → product-service 校验价格/库存
       ├─ ProductClient.deduct(changes)     → 扣库存（失败抛业务异常）
       ├─ 写 order + order_item
       └─ 写库异常时 ProductClient.restore  → 补偿回滚库存
```

Feign 客户端只写服务名，不写 IP 和端口：

```java
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/internal/products/{id}")
    Result<ProductSnapshot> detail(@PathVariable("id") Long id);
}
```

`product-service` 起两个实例时，LoadBalancer 会自动轮询，这是注册中心的直接收益。

---

## 五、本地启动步骤

> 前提：MySQL 已启动且 `nebula-store` 库已初始化（见 README），JDK 17 + Maven 已装好。

### 1. 准备配置

```bash
cd cloud
cp cloud-config.example.yml cloud-config.yml
# 编辑 cloud-config.yml，填上本机 MySQL 密码，并把 jwt.secret 换成一串 32 位以上的随机字符
```

`cloud-config.yml` 已在 `.gitignore` 中，**不要提交**。

### 2. 编译一次

```bash
cd cloud
mvn -B -DskipTests package
```

### 3. 按顺序启动（每个窗口一个）

```bash
# 窗口 1：注册中心
mvn -pl eureka-server spring-boot:run

# 窗口 2-5：四个业务服务（等注册中心起来再启动）
mvn -pl user-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl review-service spring-boot:run

# 窗口 6：网关（业务服务都注册好了再启动，避免第一次请求 503）
mvn -pl gateway spring-boot:run
```

打开 <http://localhost:8761> 应该能看到 5 个实例：`GATEWAY`、`USER-SERVICE`、`PRODUCT-SERVICE`、`ORDER-SERVICE`、`REVIEW-SERVICE`。

### 4. 前端指向网关

把 `fronted/vite.config.js` 里的代理目标从 `http://localhost:8080` 改成 `http://localhost:8090`，重启 `npm run dev`。
（前提是单体版后端 8080 没在跑，否则两套代码同时读写一个库容易看花眼。）

---

## 六、验证清单

```bash
# 1. 注册（走网关）
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"cloudtest","password":"123456"}'

# 2. 登录拿 token
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cloudtest","password":"123456"}'

# 3. 商品列表不需要登录
curl "http://localhost:8090/api/products?page=1&size=5"

# 4. 下单（带 token；商品 ID 用库里真实存在的）
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"items":[{"productId":1,"quantity":1}]}'

# 5. 没带 token 访问订单，应返回 401
curl "http://localhost:8090/api/orders"
```

| 现象 | 说明 |
| --- | --- |
| `/api/products` 返回商品分页 | 网关 → product-service 路由正常 |
| 下单后商品库存减少 | Feign 扣库存成功 |
| 取消订单后库存恢复 | 补偿接口正常 |
| 不带 token 访问 `/api/orders` 返回 401 | 网关鉴权生效 |
| 带 token 访问 `/api/users/me` 返回用户名 | 网关注入 `X-User-Id`、服务端 `CurrentUser` 生效 |

---

## 七、和单体版的差异（作业说明用）

| 维度 | 单体版 `backend/` | 微服务版 `cloud/` |
| --- | --- | --- |
| 部署单元 | 1 个 jar | 注册中心 + 网关 + 4 个服务，共 6 个进程 |
| 鉴权 | Spring Security 过滤器 | 网关 GlobalFilter + 内部信任头 |
| 服务通信 | 同进程方法调用 | OpenFeign + Eureka + LoadBalancer |
| 数据 | 一个库，全部表 | 一个库（演示），按服务划分表归属 |
| 事务 | 本地 `@Transactional` | 先扣库存再写订单，失败走补偿接口（最终一致） |
| 功能范围 | 全量功能 | 买家主流程：注册登录、浏览、下单支付取消、评价 |

> 微服务版是功能 7 的演示，**不替代**单体版。日常演示和提交作业以单体版为准，
> 需要讲解「服务拆分 + 注册发现 + 网关鉴权 + Feign」时启动 `cloud/`。

---

## 八、常见问题

| 问题 | 原因 / 解决 |
| --- | --- |
| 网关返回 503 `Unable to find instance` | 业务服务还没注册上，等 5-10 秒；或确认 Eureka 页面里实例状态是 UP |
| 启动报 `Could not resolve placeholder 'jwt.secret'` | 没建 `cloud/cloud-config.yml`，或文件里缺 `jwt.secret` |
| 登录成功但业务接口 401 | 网关和 user-service 的 `jwt.secret` 不一致，检查是否都从同一份 cloud-config 读 |
| 下单报「商品不存在」 | 商品 ID 写错，或该商品 `status = 0`（已下架） |
| 下单报「库存不足」 | `product.stock` 小于购买数量，先补库存 |
| Feign 调用超时 | 服务启动慢，`feign.client.config.default.read-timeout` 默认 5 秒，可适当调大 |
| 端口被占用 | 8081-8084 / 8090 / 8761 有冲突，先关掉占用进程再启动 |
