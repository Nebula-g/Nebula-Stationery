<template>
  <div class="orders-page">
    <div class="container">
      <h1 class="page-title">我的订单</h1>

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
      <div v-if="orders.length === 0" class="empty-orders">
        <p>暂无订单</p>
      </div>

      <div v-for="order in orders" :key="`${order.orderType}-${order.id}`" class="order-card">
        <div class="order-header">
          <div class="order-header-left">
            <span v-if="order.orderType === 'flash_sale'" class="order-tag">秒杀</span>
            <span class="order-no">订单号：{{ order.orderNo }}</span>
          </div>
          <div class="order-header-right">
            <span v-if="order.status === 0" class="countdown" :class="{ urgent: getRemainingMs(order) < 600000 }">
              剩余 {{ formatCountdown(getRemainingMs(order)) }}
            </span>
            <span class="order-status" :class="`status-${order.status}`">{{ order.statusText }}</span>
          </div>
        </div>
        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <span class="item-name">{{ item.productName }}<span v-if="item.specText" class="item-spec">（{{ item.specText }}）</span></span>
            <span class="item-price">¥{{ item.unitPrice.toFixed(2) }} x {{ item.quantity }}</span>
          </div>
        </div>
        <p v-if="reviewHint(order)" class="order-hint">{{ reviewHint(order) }}</p>
        <div class="order-footer">
          <span class="order-amount">合计：¥{{ order.totalAmount.toFixed(2) }}</span>
          <span class="order-date">{{ order.createdAt }}</span>
          <div class="order-actions">
            <router-link v-if="order.orderType !== 'flash_sale'" :to="`/order/${order.id}`" class="btn btn-secondary btn-small">查看详情</router-link>
            <button v-if="order.status === 0" @click="payOrder(order)" class="btn btn-primary btn-small">付款</button>
            <button v-if="order.status === 0" @click="cancelOrder(order)" class="btn btn-danger btn-small">取消订单</button>
            <button v-if="(order.status === 1 || order.status === 2) && order.orderType !== 'flash_sale'" @click="confirmOrder(order.id)" class="btn btn-primary btn-small">确认收货</button>
            <router-link v-if="order.status === 3 && order.orderType !== 'flash_sale'" :to="`/order/${order.id}#reviews`" class="btn btn-warning btn-small">评价商品</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import toast from '../utils/toast'
import confirmAction from '../utils/confirm'

const route = useRoute()
const orders = ref([])
const selectedStatus = ref(null)
const now = ref(Date.now())
let timer = null

// 状态可能来自地址栏 ?status=，也可能来自筛选按钮
const statusFromQuery = () => {
  const value = Number(route.query.status)
  return Number.isInteger(value) && value >= 0 && value <= 4 ? value : null
}

onMounted(() => {
  selectedStatus.value = statusFromQuery()
  fetchOrders()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})

watch(() => route.query.status, () => {
  selectedStatus.value = statusFromQuery()
  fetchOrders()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const getRemainingMs = (order) => {
  if (order.status !== 0) return 0
  const created = new Date(order.createdAt).getTime()
  const limitMs = order.orderType === 'flash_sale' ? 30 * 60 * 1000 : 12 * 60 * 60 * 1000
  const remaining = created + limitMs - now.value
  return remaining > 0 ? remaining : 0
}

const formatCountdown = (ms) => {
  if (ms <= 0) return '已超时'
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  if (hours > 0) {
    return `${hours}时${String(minutes).padStart(2, '0')}分${String(seconds).padStart(2, '0')}秒`
  }
  return `${minutes}分${String(seconds).padStart(2, '0')}秒`
}

/** 把「离评价还差哪一步」写在订单卡片上，免得找不到入口 */
const reviewHint = (order) => {
  if (order.orderType === 'flash_sale') {
    return order.status === 1 ? '秒杀订单没有发货环节，不参与评价' : ''
  }
  if (order.status === 1) return '收到货后点「确认收货」，这里会出现「评价商品」'
  if (order.status === 2) return '点「确认收货」后，这里会出现「评价商品」'
  if (order.status === 3) return '每件商品可评价一次，最多 6 张图片'
  return ''
}

const fetchOrders = async () => {
  try {
    const params = { page: 1, size: 100 }
    if (selectedStatus.value !== null) {
      params.status = selectedStatus.value
    }
    const res = await api.get('/api/orders', { params })
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

const payOrder = async (order) => {
  if (!(await confirmAction('确定付款吗？', { confirmText: '确认付款' }))) return
  try {
    let res
    if (order.orderType === 'flash_sale') {
      res = await api.post(`/api/flash-sale/pay/${order.id}`)
    } else {
      res = await api.put(`/api/orders/${order.id}/pay`)
    }
    if (res.data.code === 200) {
      toast.success('付款成功')
      fetchOrders()
    }
  } catch (e) {
    toast.error(e.response?.data?.message || '付款失败')
  }
}

const cancelOrder = async (order) => {
  if (!(await confirmAction('确定取消该订单吗？取消后不可恢复', { confirmText: '确认取消', cancelText: '暂不取消', danger: true }))) return
  try {
    let res
    if (order.orderType === 'flash_sale') {
      res = await api.post(`/api/flash-sale/cancel/${order.id}`)
    } else {
      res = await api.put(`/api/orders/${order.id}/cancel`)
    }
    if (res.data.code === 200) {
      toast.success('取消成功')
      fetchOrders()
    }
  } catch (e) {
    toast.error(e.response?.data?.message || '取消失败')
  }
}

const confirmOrder = async (orderId) => {
  if (!(await confirmAction('请确认已收到全部商品，确认后订单完成并可评价。', { confirmText: '确认收货' }))) return
  try {
    const res = await api.put(`/api/orders/${orderId}/confirm`)
    if (res.data.code === 200) {
      toast.success('确认收货成功')
      fetchOrders()
    }
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.page-title {
  font-size: 28px;
  margin: 30px 0 20px;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
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

.empty-orders {
  text-align: center;
  padding: 60px;
  color: #999;
}

.order-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
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

.order-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.countdown {
  font-size: 12px;
  color: #e67e22;
  font-weight: bold;
  background: #fff8f0;
  padding: 3px 10px;
  border-radius: 10px;
}

.countdown.urgent {
  color: #e63946;
  background: #fff0f0;
  animation: blink 1s infinite;
}

@keyframes blink {
  50% { opacity: 0.5; }
}

.order-tag {
  background: #e63946;
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
}

.order-no {
  font-size: 14px;
  color: #666;
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
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.item-name {
  color: #333;
}

.item-spec {
  color: #999;
  font-size: 12px;
}

.item-price {
  color: #666;
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

.order-date {
  font-size: 12px;
  color: #999;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.order-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: #999;
}

.btn-small {
  padding: 6px 16px;
  font-size: 13px;
}
</style>
