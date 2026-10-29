<template>
  <div class="admin-order-detail" v-if="order">
    <h1 class="page-title">订单详情</h1>

    <div class="order-card">
      <div class="order-header">
        <div>
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="order-date">下单时间：{{ order.createdAt }}</span>
        </div>
        <span class="order-status" :class="`status-${order.status}`">{{ order.statusText }}</span>
      </div>

      <div class="order-items">
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <div class="item-info">
            <h3 class="item-name">{{ item.productName }}</h3>
            <p class="item-price">¥{{ item.unitPrice.toFixed(2) }}</p>
          </div>
          <div class="item-quantity">x {{ item.quantity }}</div>
          <div class="item-subtotal">¥{{ item.subtotal.toFixed(2) }}</div>
        </div>
      </div>

      <div class="order-summary">
        <div class="summary-row">
          <span>订单总额：</span>
          <span class="total-amount">¥{{ order.totalAmount.toFixed(2) }}</span>
        </div>
      </div>

      <div class="order-actions">
        <router-link to="/admin/orders" class="btn btn-secondary">返回订单列表</router-link>
        <button v-if="order.status === 1" @click="shipOrder" class="btn btn-primary">标记发货</button>
      </div>
    </div>
  </div>
  <div v-else class="loading">
    <p>加载中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api'

const route = useRoute()
const router = useRouter()

const order = ref(null)

const fetchOrder = async () => {
  try {
    const res = await api.get(`/api/admin/orders/${route.params.id}`)
    if (res.data.code === 200) {
      order.value = res.data.data
    }
  } catch (e) {
    console.error('获取订单失败', e)
  }
}

const shipOrder = async () => {
  if (!confirm('确定标记发货吗？')) return
  try {
    const res = await api.put(`/api/admin/orders/${order.value.id}/ship`)
    if (res.data.code === 200) {
      alert('发货成功')
      fetchOrder()
    }
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(fetchOrder)
</script>

<style scoped>
.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.order-card {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.order-no {
  font-size: 16px;
  color: #333;
  margin-right: 20px;
}

.order-date {
  font-size: 14px;
  color: #999;
}

.order-status {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 14px;
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
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.item-name {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.item-price {
  color: #e63946;
  font-weight: bold;
}

.item-quantity {
  color: #666;
  font-size: 14px;
}

.item-subtotal {
  font-size: 18px;
  font-weight: bold;
  color: #e63946;
}

.order-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  font-size: 18px;
}

.total-amount {
  font-size: 24px;
  font-weight: bold;
  color: #e63946;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.loading {
  text-align: center;
  padding: 60px;
  color: #999;
}
</style>
