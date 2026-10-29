<template>
  <div class="front-layout">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <router-link to="/" class="logo">
            <img src="/logo.png" alt="星云 · Nebula" class="logo-img" />
            <span class="logo-text">星云 · <span class="logo-text-en">Nebula</span></span>
          </router-link>
          <SearchBox class="header-search" />
          <nav class="nav">
            <router-link to="/" class="nav-link">首页</router-link>
            <router-link to="/category/1" class="nav-link">书写工具</router-link>
            <router-link to="/category/2" class="nav-link">纸本文具</router-link>
            <router-link to="/category/3" class="nav-link">办公收纳</router-link>
            <router-link to="/category/4" class="nav-link">美术用品</router-link>
            <router-link to="/flash-sale" class="nav-link">秒杀</router-link>
            <router-link to="/group-buy" class="nav-link">拼团</router-link>
          </nav>
          <div class="header-actions">
            <template v-if="userStore.isLoggedIn">
              <div class="user-dropdown" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
                <span class="user-name-trigger">{{ username }} ▾</span>
                <div v-show="showDropdown" class="dropdown-menu">
                  <router-link to="/addresses" class="dropdown-item">📍 收货地址管理</router-link>
                  <router-link to="/refunds" class="dropdown-item">🔄 退款/售后</router-link>
                  <router-link to="/orders" class="dropdown-item">📦 我的订单</router-link>
                  <router-link to="/orders?status=3" class="dropdown-item">📝 去评价</router-link>
                  <router-link to="/coupons" class="dropdown-item">🎫 优惠券</router-link>
                  <router-link to="/points" class="dropdown-item">⭐ 我的积分</router-link>
                  <div class="dropdown-divider"></div>
                  <a @click="handleLogout" class="dropdown-item logout-item">退出登录</a>
                </div>
              </div>
            </template>
            <template v-else>
              <router-link to="/login" class="btn btn-primary">登录</router-link>
              <router-link to="/register" class="btn btn-secondary">注册</router-link>
            </template>
          </div>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="container">
        <p>&copy; 2026 星云商城 Nebula Stationery. All rights reserved.</p>
      </div>
    </footer>

    <div v-if="userStore.isLoggedIn" class="right-sidebar">
      <router-link to="/cart" class="sidebar-item">
        <span class="sidebar-icon">🛒</span>
        <span class="sidebar-text">购物车</span>
      </router-link>
      <div class="sidebar-item" @click="showMessages = !showMessages">
        <span class="sidebar-icon">💬</span>
        <span class="sidebar-text">消息</span>
        <span v-if="unreadCount > 0" class="sidebar-badge">{{ unreadCount }}</span>
      </div>
    </div>

    <div v-if="showMessages" class="message-panel" @mouseleave="showMessages = false">
      <div class="message-panel-header">
        <h3>消息中心</h3>
        <button @click="showMessages = false" class="close-btn">✕</button>
      </div>
      <div class="message-panel-body">
        <div v-if="messages.length === 0" class="no-messages">暂无新消息</div>
        <div v-for="msg in messages" :key="msg.id" class="message-item">
          <span class="message-type">{{ msg.type }}</span>
          <span class="message-content">{{ msg.content }}</span>
          <span class="message-time">{{ msg.time }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import SearchBox from '../components/SearchBox.vue'

const router = useRouter()
const userStore = useUserStore()

const username = computed(() => userStore.username || '用户')
const showDropdown = ref(false)
const showMessages = ref(false)
const unreadCount = ref(0)
const messages = ref([])

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  font-size: 28px;
  font-weight: bold;
  color: #000000;
  text-decoration: none;
}

.logo-img {
  height: 32px;
  width: 32px;
}

.logo-text-en {
  font-weight: normal;
}

.nav {
  display: flex;
  gap: 16px;
}

.header-content .header-search {
  flex: 0 1 260px;
  min-width: 150px;
  max-width: 260px;
  margin-left: 24px;
}

.nav-link {
  color: #333;
  font-size: 18px;
  transition: color 0.3s;
  text-decoration: none;
  white-space: nowrap;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #000000;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-shrink: 0;
}

.user-dropdown {
  position: relative;
  cursor: pointer;
}

.user-name-trigger {
  color: #333;
  font-size: 16px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-name-trigger:hover {
  background: #f5f5f5;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  padding: 8px 0;
  z-index: 200;
}

.dropdown-item {
  display: block;
  padding: 10px 16px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.2s;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-divider {
  height: 1px;
  background: #eee;
  margin: 4px 0;
}

.logout-item {
  color: #e63946;
}

.main {
  min-height: calc(100vh - 70px - 60px);
}

.footer {
  background: #333;
  color: #999;
  text-align: center;
  padding: 10px 0;
  margin-top: 40px;
  font-size: 12px;
  font-style: italic;
  line-height: 1.5;
}

.right-sidebar {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 90;
}

.sidebar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  color: #333;
  position: relative;
}

.sidebar-item:hover {
  background: #000;
  color: white;
  transform: scale(1.05);
}

.sidebar-icon {
  font-size: 20px;
  line-height: 1;
}

.sidebar-text {
  font-size: 11px;
  margin-top: 2px;
}

.sidebar-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #e63946;
  color: white;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.message-panel {
  position: fixed;
  right: 90px;
  top: 50%;
  transform: translateY(-50%);
  width: 300px;
  max-height: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 95;
  overflow: hidden;
}

.message-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.message-panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #999;
}

.message-panel-body {
  max-height: 340px;
  overflow-y: auto;
  padding: 8px;
}

.no-messages {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}

.message-item {
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 4px;
  background: #f9f9f9;
}

.message-type {
  font-size: 12px;
  color: #000;
  font-weight: bold;
}

.message-content {
  display: block;
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

.message-time {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
</style>
