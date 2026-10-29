# 全局业务流程

> 版本：1.0 | 更新日期：2026-10-29

---

## 一、核心业务流程

### 1.1 购物主链路

```mermaid
graph TD
    A[游客访问首页] --> B[浏览分类和商品]
    B --> C[查看商品详情]
    C --> D{是否登录?}
    D -->|否| E[提示登录]
    D -->|是| F[加入购物车]
    F --> G[进入购物车页面]
    G --> H[勾选商品结算]
    H --> I[创建订单]
    I --> J[模拟付款]
    J --> K[订单状态变为已支付]
    K --> L[管理员标记发货]
    L --> M[用户确认收货]
    M --> N[订单状态变为已完成]
    N --> O[对商品评价]
```

---

### 1.2 订单状态流转

```mermaid
stateDiagram-v2
    [*] --> 待支付 : 创建订单

    待支付 --> 已支付 : 用户付款
    待支付 --> 已取消 : 用户取消

    已支付 --> 已发货 : 管理员标记发货

    已发货 --> 已完成 : 用户确认收货

    已完成 --> [*]
    已取消 --> [*]

    note right of 待支付
        可取消
        取消时库存回补
    end note

    note right of 已完成
        终态
        可评价
    end note

    note right of 已取消
        终态
        不可更改
    end note
```

---

### 1.3 库存扣减与回补

```mermaid
sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant B as 后端
    participant DB as 数据库

    U->>F: 点击结算
    F->>B: POST /api/orders
    B->>DB: 检查库存是否充足
    DB-->>B: 返回库存信息

    alt 库存充足
        B->>DB: 开始事务
        B->>DB: 扣减库存
        B->>DB: 创建订单
        B->>DB: 创建订单明细
        B->>DB: 提交事务
        B-->>F: 下单成功
        F-->>U: 跳转订单页
    else 库存不足
        B-->>F: 返回 400 库存不足
        F-->>U: 提示库存不足
    end

    U->>F: 取消待支付订单
    F->>B: PUT /api/orders/{id}/cancel
    B->>DB: 开始事务
    B->>DB: 回补库存
    B->>DB: 更新订单状态为已取消
    B->>DB: 提交事务
    B-->>F: 取消成功
```

---

## 二、角色权限流程

### 2.1 游客流程

```mermaid
graph TD
    A[访问首页] --> B[浏览分类]
    B --> C[查看商品详情]
    C --> D[点击加入购物车]
    D --> E[提示：请先登录]
    E --> F[跳转登录页]
```

### 2.2 普通用户流程

```mermaid
graph TD
    A[登录] --> B[浏览商品]
    B --> C[加入购物车]
    C --> D[结算下单]
    D --> E[付款]
    E --> F[查看订单]
    F --> G[确认收货]
    G --> H[评价商品]
```

### 2.3 管理员流程

```mermaid
graph TD
    A[登录后台] --> B[查看订单列表]
    B --> C[标记发货]
    A --> D[商品管理]
    D --> E[新增商品]
    D --> F[编辑商品]
    D --> G[上下架商品]
```

---

## 三、商品上下架影响

### 3.1 下架流程

```mermaid
sequenceDiagram
    participant Admin as 管理员
    participant Backend as 后端
    participant DB as 数据库
    participant User as 用户

    Admin->>Backend: PUT /api/admin/products/{id}/status (status=0)
    Backend->>DB: 更新商品状态为下架
    DB-->>Backend: 成功

    Note over User: 用户侧影响

    User->>Backend: GET /api/products
    Backend-->>User: 列表不包含下架商品

    User->>Backend: GET /api/products/{id}
    Backend-->>User: 返回 404

    User->>Backend: POST /api/cart
    Backend-->>User: 返回 400 商品已下架

    User->>Backend: POST /api/orders (购物车含下架商品)
    Backend-->>User: 返回 400 包含下架商品，无法结算
```

---

## 四、评价流程

### 4.1 评价前置条件

```mermaid
graph TD
    A[用户想评价商品] --> B{订单状态是已完成?}
    B -->|否| C[提示：订单未完成，无法评价]
    B -->|是| D{该商品已评价?}
    D -->|是| E[提示：已评价，不能重复评价]
    D -->|否| F[可以评价]
    F --> G[提交评分和文字]
    G --> H[评价成功，显示在商品详情页]
```

---

## 五、数据一致性保证

### 5.1 事务场景

| 场景 | 事务操作 | 失败处理 |
| --- | --- | --- |
| 下单 | 扣库存 + 写订单 + 写订单明细 | 全部回滚 |
| 取消订单 | 回补库存 + 更新订单状态 | 全部回滚 |
| 确认收货 | 更新订单状态 | 回滚 |

### 5.2 价格快照机制

```mermaid
graph LR
    A[商品表 price] -->|下单时| B[订单明细 unit_price]
    B -->|后续| C[商品改价]
    C -.不影响.-> B

    Note over B: 订单明细存快照<br/>历史订单金额不变
```

---

## 六、异常处理流程

### 6.1 未登录访问

```mermaid
graph TD
    A[用户访问需要登录的页面] --> B[前端检查 localStorage token]
    B -->|无 token| C[跳转登录页]
    B -->|有 token| D[请求后端接口]
    D --> E[后端验证 token]
    E -->|无效| F[返回 401]
    F --> G[前端清除 token，跳转登录页]
```

### 6.2 无权限访问

```mermaid
graph TD
    A[普通用户访问后台接口] --> B[后端检查角色]
    B -->|role != 1| C[返回 403]
    C --> D[前端提示无权限]
```

### 6.3 库存不足

```mermaid
graph TD
    A[用户下单] --> B[后端检查库存]
    B -->|库存不足| C[返回 400]
    C --> D[前端提示库存不足]
    D --> E[订单未创建，库存未扣]
```
