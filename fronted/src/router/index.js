import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 前台路由
  {
    path: '/',
    component: () => import('../layouts/FrontLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue')
      },
      {
        path: 'category/:id',
        name: 'Category',
        component: () => import('../views/Category.vue')
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('../views/ProductDetail.vue')
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/Cart.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/Orders.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('../views/OrderDetail.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'coupons',
        name: 'Coupons',
        component: () => import('../views/Coupons.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'flash-sale',
        name: 'FlashSale',
        component: () => import('../views/FlashSale.vue')
      },
      {
        path: 'group-buy',
        name: 'GroupBuy',
        component: () => import('../views/GroupBuy.vue')
      },
      {
        path: 'points',
        name: 'Points',
        component: () => import('../views/Points.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'addresses',
        name: 'Addresses',
        component: () => import('../views/Addresses.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'refunds',
        name: 'Refunds',
        component: () => import('../views/Refunds.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  // 登录注册
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  // 后台路由
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/orders'
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/OrderList.vue')
      },
      {
        path: 'orders/:id',
        name: 'AdminOrderDetail',
        component: () => import('../views/admin/OrderDetail.vue')
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('../views/admin/ProductList.vue')
      },
      {
        path: 'products/create',
        name: 'AdminProductCreate',
        component: () => import('../views/admin/ProductForm.vue')
      },
      {
        path: 'products/:id/edit',
        name: 'AdminProductEdit',
        component: () => import('../views/admin/ProductForm.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresAdmin && role !== '1') {
    next('/')
  } else {
    next()
  }
})

export default router
