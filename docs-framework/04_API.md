# 星云商城（Nebula Stationery）接口契约

> 版本：1.0 | 更新日期：2026-10-29

---

## 一、接口规范

### 1.1 基础路径
- 所有接口统一前缀：`/api`
- 示例：`GET /api/products`

### 1.2 请求格式
- Content-Type: `application/json`
- 认证接口：请求头带 `Authorization: Bearer <token>`

### 1.3 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 1.4 状态码规范

| 状态码 | 含义 | 使用场景 |
| --- | --- | --- |
| 200 | 成功 | 正常返回数据 |
| 400 | 参数错误/业务规则违反 | 库存不足、重复评价、用户名已存在、数量不合法 |
| 401 | 未登录 | 未携带 token 或 token 过期 |
| 403 | 无权限 | 普通用户访问后台接口、访问他人订单 |
| 500 | 服务端异常 | 系统内部错误 |

---

## 二、认证接口

### 2.1 用户注册

**POST** `/api/auth/register`

**请求体**：
```json
{
  "username": "string",
  "password": "string"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

**业务规则**：
- 用户名唯一，重复返回 400
- 密码不能明文存库（BCrypt 加密）
- 注册后角色默认为普通用户（role = 0）

---

### 2.2 用户登录

**POST** `/api/auth/login`

**请求体**：
```json
{
  "username": "string",
  "password": "string"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "string",
    "role": 0
  }
}
```

**业务规则**：
- 验证用户名和密码
- 返回 JWT token 和用户角色
- 前端存储 token 到 localStorage

---

### 2.3 用户登出

**POST** `/api/auth/logout`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

**业务规则**：
- 前端清除 localStorage 中的 token
- 后续请求不带 token 返回 401

---

## 三、商品接口

### 3.1 获取商品列表（前台）

**GET** `/api/products`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| categoryId | Long | 否 | 分类 ID 筛选 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页数量，默认 10 |

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "商品名称",
        "price": 29.90,
        "stock": 100,
        "categoryId": 5,
        "categoryName": "二级分类名",
        "description": "商品描述",
        "imageUrl": "https://...",
        "status": 1
      }
    ],
    "total": 50,
    "page": 1,
    "size": 10
  }
}
```

**业务规则**：
- 只返回上架商品（status = 1）
- 下架商品不出现在列表

---

### 3.2 获取商品详情

**GET** `/api/products/{id}`

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "商品名称",
    "price": 29.90,
    "stock": 100,
    "categoryId": 5,
    "categoryName": "二级分类名",
    "description": "商品描述",
    "imageUrl": "https://...",
    "status": 1,
    "reviews": [
      {
        "id": 1,
        "userId": 2,
        "username": "user1",
        "rating": 5,
        "content": "很好用",
        "createdAt": "2026-10-29 10:00:00"
      }
    ]
  }
}
```

**业务规则**：
- 下架商品返回 404
- 包含该商品的评价列表

---

### 3.3 获取分类列表

**GET** `/api/categories`

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "一级分类",
      "parentId": null,
      "children": [
        {
          "id": 5,
          "name": "二级分类",
          "parentId": 1,
          "children": []
        }
      ]
    }
  ]
}
```

---

### 3.4 新增商品（后台）

**POST** `/api/admin/products`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "name": "商品名称",
  "price": 29.90,
  "stock": 100,
  "categoryId": 5,
  "description": "商品描述",
  "imageUrl": "https://..."
}
```

**响应**：
```json
{
  "code": 200,
  "message": "新增成功",
  "data": {
    "id": 13
  }
}
```

**权限**：仅管理员

---

### 3.5 编辑商品（后台）

**PUT** `/api/admin/products/{id}`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：同新增商品

**响应**：
```json
{
  "code": 200,
  "message": "编辑成功",
  "data": null
}
```

**权限**：仅管理员

---

### 3.6 商品上下架（后台）

**PUT** `/api/admin/products/{id}/status`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "status": 0
}
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

**业务规则**：
- status = 0 下架，status = 1 上架
- 下架后前台不显示、不能加购、购物车已有记录不能结算

**权限**：仅管理员

---

## 四、购物车接口

### 4.1 添加商品到购物车

**POST** `/api/cart`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "productId": 1,
  "quantity": 2
}
```

**响应**：
```json
{
  "code": 200,
  "message": "添加成功",
  "data": null
}
```

**业务规则**：
- 同一商品重复加入只加数量
- 加购数量不能超过库存
- 未登录返回 401

---

### 4.2 获取购物车列表

**GET** `/api/cart`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "商品名称",
        "price": 29.90,
        "quantity": 2,
        "subtotal": 59.80,
        "imageUrl": "https://...",
        "status": 1
      }
    ],
    "totalAmount": 59.80
  }
}
```

**业务规则**：
- 返回当前用户的购物车
- 包含商品是否下架状态

---

### 4.3 更新购物车数量

**PUT** `/api/cart/{id}`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "quantity": 3
}
```

**响应**：
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

**业务规则**：
- 数量不能超过库存
- 数量必须大于 0

---

### 4.4 删除购物车项

**DELETE** `/api/cart/{id}`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

### 4.5 清空购物车

**DELETE** `/api/cart/clear`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "清空成功",
  "data": null
}
```

---

## 五、订单接口

### 5.1 创建订单

**POST** `/api/orders`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "cartItemIds": [1, 2, 3]
}
```

**响应**：
```json
{
  "code": 200,
  "message": "下单成功",
  "data": {
    "orderId": 1,
    "orderNo": "202610291000001",
    "totalAmount": 159.70
  }
}
```

**业务规则**：
- 订单金额由服务端按当时商品价格算
- 扣库存、写订单、写订单明细在同一事务里
- 任一失败全部回滚
- 购物车中下架商品不能结算

---

### 5.2 获取订单列表

**GET** `/api/orders`

**请求头**：
```
Authorization: Bearer <token>
```

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | Integer | 否 | 订单状态筛选 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "orderNo": "202610291000001",
        "totalAmount": 159.70,
        "status": 0,
        "statusText": "待支付",
        "createdAt": "2026-10-29 10:00:00",
        "items": [
          {
            "productId": 1,
            "productName": "商品名称",
            "unitPrice": 29.90,
            "quantity": 2,
            "subtotal": 59.80
          }
        ]
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```

**业务规则**：
- 只返回当前用户的订单
- 即使用户修改请求参数也不能看到别人的订单

---

### 5.3 获取订单详情

**GET** `/api/orders/{id}`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "202610291000001",
    "totalAmount": 159.70,
    "status": 0,
    "statusText": "待支付",
    "createdAt": "2026-10-29 10:00:00",
    "items": [
      {
        "productId": 1,
        "productName": "商品名称",
        "unitPrice": 29.90,
        "quantity": 2,
        "subtotal": 59.80
      }
    ]
  }
}
```

**业务规则**：
- 只能查看自己的订单

---

### 5.4 模拟付款

**PUT** `/api/orders/{id}/pay`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "付款成功",
  "data": null
}
```

**业务规则**：
- 模拟付款，直接修改状态为已支付
- 只有待支付订单可以付款

---

### 5.5 取消订单

**PUT** `/api/orders/{id}/cancel`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "取消成功",
  "data": null
}
```

**业务规则**：
- 只有待支付订单可以取消
- 取消时库存回补
- 已完成和已取消是终态，不能取消

---

### 5.6 确认收货

**PUT** `/api/orders/{id}/confirm`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "确认收货成功",
  "data": null
}
```

**业务规则**：
- 只有已发货订单可以确认收货
- 确认后状态变为已完成

---

## 六、后台订单接口

### 6.1 获取全部订单列表（后台）

**GET** `/api/admin/orders`

**请求头**：
```
Authorization: Bearer <token>
```

**查询参数**：同 5.2

**响应**：同 5.2，但返回所有用户的订单

**权限**：仅管理员

---

### 6.2 标记发货（后台）

**PUT** `/api/admin/orders/{id}/ship`

**请求头**：
```
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "发货成功",
  "data": null
}
```

**业务规则**：
- 只有已支付订单可以标记发货
- 发货后状态变为已发货

**权限**：仅管理员

---

## 七、评价接口

### 7.1 创建评价

**POST** `/api/reviews`

**请求头**：
```
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "orderId": 1,
  "productId": 1,
  "rating": 5,
  "content": "很好用"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "评价成功",
  "data": null
}
```

**业务规则**：
- 只有已完成订单里的商品能被评价
- 同一订单同一商品只能评一次
- 评分 1-5 星

---

## 八、错误响应示例

### 8.1 未登录
```json
{
  "code": 401,
  "message": "未登录，请先登录",
  "data": null
}
```

### 8.2 无权限
```json
{
  "code": 403,
  "message": "无权限访问",
  "data": null
}
```

### 8.3 库存不足
```json
{
  "code": 400,
  "message": "库存不足",
  "data": null
}
```

### 8.4 业务规则违反
```json
{
  "code": 400,
  "message": "该商品已下架，无法结算",
  "data": null
}
```
