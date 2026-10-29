<template>
  <div class="admin-orders">
    <h1 class="page-title">订单管理</h1>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <button @click="filterByStatus(null)" :class="['filter-btn', { active: selectedStatus === null }]">全部</button>
      <button @click="filterByStatus(0)" :class="['filter-btn', { active: selectedStatus === 0 }]">待支付</button>
      <button @click="filterByStatus(1)" :class="['filter-btn', { active: selectedStatus === 1 }]">已支付</button>
      <button @click="filterByStatus(2)" :class="['filter-btn', { active: selectedStatus === 2 }]">已发货</button>
      <button @click="filterByStatus(3)" :class="['filter-btn', { active: selectedStatus === 3 }]">已完成</button>
      <button @click="filterByStatus(4)" :class="['filter-btn', { active: selectedStatus === 4 }]">已取消</button>
    </div>

    <!-- 订单列表 -->
    <div v-if="orders.length === 0" class="empty">
      <p>暂无订单</p>
    </div>

    <div v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <div>
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="order-date">{{ order.createdAt }}</span>
        </div>
        <span class="order-status" :class="`status-${order.status}`">{{ order.statusText }}</span>
      </div>

      <div class="order-items">
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <span class="item-name">{{ item.productName }}</span>
          <span class="item-price">¥{{ item.unitPrice.toFixed(2) }} x {{ item.quantity }}</span>
          <span class="item-subtotal">¥{{ item.subtotal.toFixed(2) }}</span>
        </div>
      </div>

      <div class="order-footer">
        <span class="order-amount">合计：¥{{ order.totalAmount.toFixed(2) }}</span>
        <div class="order-actions">
          <router-link :to="`/admin/orders/${order.id}`" class="btn btn-secondary btn-small">查看详情</router-link>
          <button v-if="order.status === 1" @click="shipOrder(order.id)" class="btn btn-primary btn-small">标记发货</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

const orders = ref([])
const selectedStatus = ref(null)

const fetchOrders = async () => {
  try {
    const params = { page: 1, size: 100 }
    if (selectedStatus.value !== null) {
      params.status = selectedStatus.value
    }
    const res = await api.get('/api/admin/orders', { params })
    if (res.data.code === 200) {
      orders.value = res.data.data.records
    }
  } catch (e) {
    console.error('获取订单失败', e)
  }
}

const filterByStatus = (status) => {
  selectedStatus.value = status
  fetchOrders()
}

const shipOrder = async (orderId) => {
  if (!confirm('确定标记发货吗？')) return
  try {
    const res = await api.put(`/api/admin/orders/${orderId}/ship`)
    if (res.data.code === 200) {
      alert('发货成功')
      fetchOrders()
    }
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filter-btn {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn:hover,
.filter-btn.active {
  background: #000000;
  color: white;
  border-color: #000000;
}

.empty {
  text-align: center;
  padding: 60px;
  color: #999;
}

.order-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.order-no {
  font-size: 14px;
  color: #333;
  margin-right: 20px;
}

.order-date {
  font-size: 12px;
  color: #999;
}

.order-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-0 { background: #fff3cd; color: #856404; }
.status-1 { background: #d1ecf1; color: #0c5460; }
.status-2 { background: #d4edda; color: #155724; }
.status-3 { background: #d4edda; color: #155724; }
.status-4 { background: #f8d7da; color: #721c24; }

.order-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 20px;
  padding: 8px 0;
  font-size: 14px;
}

.item-name {
  color: #333;
}

.item-price {
  color: #666;
}

.item-subtotal {
  font-weight: bold;
  color: #e63946;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.order-amount {
  font-size: 18px;
  font-weight: bold;
  color: #e63946;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.btn-small {
  padding: 6px 16px;
  font-size: 13px;
}
</style>
