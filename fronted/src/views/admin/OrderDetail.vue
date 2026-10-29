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
        <button v-if="order.status === 1" @click="shipVisible = true" class="btn btn-primary">去发货</button>
      </div>

      <div v-if="order.logistics" class="logistics-block">
        <h3 class="block-title">物流信息</h3>
        <p class="logistics-line">
          {{ order.logistics.companyName }} · 运单号 {{ order.logistics.trackingNo }}
          <span class="logistics-status" :class="{ done: order.logistics.status === 1 }">{{ order.logistics.statusText }}</span>
        </p>
        <ul v-if="order.logistics.tracks?.length" class="timeline">
          <li v-for="(track, idx) in order.logistics.tracks" :key="idx" :class="{ latest: idx === 0 }">
            <span class="dot"></span>
            <div>
              <p class="track-desc">{{ track.description }}</p>
              <p class="track-meta">{{ track.time }}<span v-if="track.location"> · {{ track.location }}</span></p>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <ShipDialog v-model="shipVisible" :order="order" @shipped="fetchOrder" />
  </div>
  <div v-else class="loading">
    <p>加载中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../api'
import ShipDialog from '../../components/ShipDialog.vue'

const route = useRoute()

const order = ref(null)
const shipVisible = ref(false)

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

.logistics-block {
  margin-top: 24px;
  border-top: 1px solid #f0f0f0;
  padding-top: 18px;
}

.block-title {
  font-size: 15px;
  color: #333;
  margin-bottom: 8px;
}

.logistics-line {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.logistics-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fdf3e6;
  color: #b26a1b;
}

.logistics-status.done {
  background: #e9f7ef;
  color: #1e7d4d;
}

.timeline {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
}

.timeline li {
  position: relative;
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
}

.timeline li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 14px;
  bottom: 0;
  width: 1px;
  background: #eee;
}

.timeline .dot {
  flex: 0 0 9px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #ddd;
  margin-top: 4px;
}

.timeline li.latest .dot {
  background: #2a9d8f;
}

.track-desc {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
}

.timeline li.latest .track-desc {
  color: #1e7d4d;
  font-weight: bold;
}

.track-meta {
  font-size: 12px;
  color: #aaa;
  margin-top: 3px;
}
</style>
