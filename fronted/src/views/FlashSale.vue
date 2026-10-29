<template>
  <div class="flash-sale-page container">
    <h2 class="page-title">限时秒杀</h2>

    <div class="sale-grid">
      <div v-for="item in flashSales" :key="item.id" class="sale-card">
        <img :src="item.imageUrl" :alt="item.productName" class="sale-img" />
        <div class="sale-info">
          <h3>{{ item.productName }}</h3>
          <div class="price-row">
            <span class="flash-price">&yen;{{ item.flashPrice }}</span>
            <span class="original-price">&yen;{{ item.originalPrice }}</span>
          </div>
          <div class="stock-bar">
            <div class="stock-fill" :style="{ width: (item.soldCount / item.flashStock * 100) + '%' }"></div>
          </div>
          <p class="stock-text">已售 {{ item.soldCount }} / {{ item.flashStock }}</p>
          <p class="time-text">{{ item.startTime?.split('T')[0] }} ~ {{ item.endTime?.split('T')[0] }}</p>
          <button class="btn btn-primary btn-block" @click="participate(item.id)">立即抢购</button>
        </div>
      </div>
    </div>

    <div v-if="flashSales.length === 0" class="empty">暂无秒杀活动</div>

    <div v-if="myOrders.length > 0" class="my-orders">
      <h3>我的秒杀订单 <span class="hint">（也可在"我的订单"中管理）</span></h3>
      <p class="warning-text">注意：秒杀订单需在30分钟内完成支付，超时将自动取消</p>
      <div v-for="o in myOrders" :key="o.id" class="order-card">
        <span>订单 #{{ o.id }}</span>
        <span>金额: &yen;{{ o.totalAmount }}</span>
        <span v-if="o.status === 0" class="countdown" :class="{ urgent: getRemainingMs(o) < 600000 }">
          剩余 {{ formatCountdown(getRemainingMs(o)) }}
        </span>
        <span :class="'status-' + o.status">{{ ['待支付', '已支付', '已取消'][o.status] }}</span>
        <button v-if="o.status === 0" class="btn btn-primary" @click="payOrder(o.id)">支付</button>
        <button v-if="o.status === 0" class="btn btn-danger btn-small" @click="cancelOrder(o.id)">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'

const flashSales = ref([])
const myOrders = ref([])
const now = ref(Date.now())
let timer = null

onMounted(() => {
  loadFlashSales()
  loadMyOrders()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const getRemainingMs = (order) => {
  if (order.status !== 0) return 0
  const created = new Date(order.createdAt).getTime()
  const remaining = created + 30 * 60 * 1000 - now.value
  return remaining > 0 ? remaining : 0
}

const formatCountdown = (ms) => {
  if (ms <= 0) return '已超时'
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}分${String(seconds).padStart(2, '0')}秒`
}

const loadFlashSales = async () => {
  try {
    const res = await api.get('/api/flash-sale')
    if (res.data.code === 200) flashSales.value = res.data.data
  } catch (e) { console.error(e) }
}

const loadMyOrders = async () => {
  try {
    const res = await api.get('/api/flash-sale/my-orders')
    if (res.data.code === 200) myOrders.value = res.data.data
  } catch (e) { console.error(e) }
}

const participate = async (flashSaleId) => {
  try {
    const res = await api.post('/api/flash-sale/participate', null, { params: { flashSaleId } })
    if (res.data.code === 200) {
      alert('秒杀成功！订单金额: ¥' + res.data.data.totalAmount)
      loadFlashSales()
      loadMyOrders()
    } else {
      alert(res.data.message)
    }
  } catch (e) { alert(e.response?.data?.message || '秒杀失败') }
}

const payOrder = async (orderId) => {
  try {
    const res = await api.post(`/api/flash-sale/pay/${orderId}`)
    if (res.data.code === 200) {
      alert('支付成功')
      loadMyOrders()
    } else {
      alert(res.data.message)
    }
  } catch (e) { alert(e.response?.data?.message || '支付失败') }
}

const cancelOrder = async (orderId) => {
  if (!confirm('确定取消订单吗？')) return
  try {
    const res = await api.post(`/api/flash-sale/cancel/${orderId}`)
    if (res.data.code === 200) {
      alert('取消成功')
      loadMyOrders()
    } else {
      alert(res.data.message)
    }
  } catch (e) { alert(e.response?.data?.message || '取消失败') }
}
</script>

<style scoped>
.flash-sale-page { padding: 30px 20px; max-width: 1000px; margin: 0 auto; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.sale-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.sale-card { border: 1px solid #eee; border-radius: 8px; overflow: hidden; background: white; }
.sale-img { width: 100%; height: 200px; object-fit: cover; }
.sale-info { padding: 15px; }
.sale-info h3 { margin: 0 0 10px; font-size: 16px; }
.price-row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 10px; }
.flash-price { font-size: 24px; color: #e74c3c; font-weight: bold; }
.original-price { font-size: 14px; color: #999; text-decoration: line-through; }
.stock-bar { height: 8px; background: #eee; border-radius: 4px; overflow: hidden; margin-bottom: 5px; }
.stock-fill { height: 100%; background: #e74c3c; border-radius: 4px; }
.stock-text { font-size: 12px; color: #666; margin: 0 0 5px; }
.time-text { font-size: 12px; color: #999; margin: 0 0 10px; }
.btn-block { width: 100%; }
.my-orders { margin-top: 40px; }
.my-orders h3 { margin-bottom: 15px; }
.my-orders .hint { font-size: 12px; color: #999; font-weight: normal; }
.warning-text { font-size: 13px; color: #e74c3c; margin-bottom: 15px; padding: 8px 12px; background: #fff5f5; border-radius: 4px; }
.order-card { display: flex; align-items: center; gap: 15px; padding: 12px; border: 1px solid #eee; border-radius: 8px; margin-bottom: 10px; background: white; flex-wrap: wrap; }
.countdown { font-size: 12px; color: #e67e22; font-weight: bold; background: #fff8f0; padding: 3px 10px; border-radius: 10px; }
.countdown.urgent { color: #e63946; background: #fff0f0; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.5; } }
.status-0 { color: #e74c3c; }
.status-1 { color: #27ae60; }
.status-2 { color: #999; }
.empty { text-align: center; padding: 40px; color: #999; }
</style>
