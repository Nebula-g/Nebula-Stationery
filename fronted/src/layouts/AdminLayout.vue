<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="container">
        <div class="header-content">
          <router-link to="/admin" class="logo">
            <img src="/logo.png" alt="星云商城" class="logo-img" />
            <span class="logo-text">星云商城 - 后台管理</span>
          </router-link>
          <div class="header-actions">
            <router-link to="/" class="btn btn-secondary">返回前台</router-link>
            <button @click="handleLogout" class="btn btn-danger">登出</button>
          </div>
        </div>
      </div>
    </header>
    <div class="admin-body">
      <aside class="admin-sidebar">
        <nav class="sidebar-nav">
          <router-link to="/admin/stats" class="sidebar-link">
            <span>📊</span> 数据统计
          </router-link>
          <router-link to="/admin/orders" class="sidebar-link">
            <span>📦</span> 订单管理
          </router-link>
          <router-link to="/admin/products" class="sidebar-link">
            <span>🗂️</span> 商品管理
          </router-link>
          <router-link to="/admin/ops" class="sidebar-link">
            <span>⚙️</span> 中间件监控
          </router-link>
        </nav>
      </aside>
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-header {
  background: #1a1a2e;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: white;
}

.logo-img {
  height: 35px;
  width: 35px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.admin-body {
  display: flex;
  min-height: calc(100vh - 60px);
}

.admin-sidebar {
  width: 200px;
  background: #16213e;
  padding: 20px 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  color: #ccc;
  transition: all 0.3s;
}

.sidebar-link:hover,
.sidebar-link.router-link-active {
  background: #0f3460;
  color: white;
}

.admin-main {
  flex: 1;
  padding: 20px;
  background: #f5f5f5;
}
</style>
