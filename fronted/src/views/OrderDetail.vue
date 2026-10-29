<template>
  <div class="order-detail" v-if="order">
    <div class="container">
      <h1 class="page-title">订单详情</h1>

      <div class="order-card">
        <div class="order-header">
          <div>
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-date">下单时间：{{ order.createdAt }}</span>
          </div>
          <div class="order-header-right">
            <span v-if="order.status === 0" class="countdown" :class="{ urgent: remainingMs < 600000 }">
              剩余 {{ formatCountdown(remainingMs) }}
            </span>
            <span class="order-status" :class="`status-${order.status}`">{{ order.statusText }}</span>
          </div>
        </div>

        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="item-info">
              <h3 class="item-name">{{ item.productName }}</h3>
              <p v-if="item.specText" class="item-spec">规格：{{ item.specText }}</p>
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
          <router-link to="/orders" class="btn btn-secondary">返回订单列表</router-link>
          <button v-if="order.status === 0" @click="payOrder" class="btn btn-primary">立即付款</button>
          <button v-if="order.status === 0" @click="cancelOrder" class="btn btn-danger">取消订单</button>
          <button v-if="order.status === 2" @click="confirmOrder" class="btn btn-primary">确认收货</button>
          <button v-if="order.status === 1 || order.status === 3" @click="openRefundDialog" class="btn btn-warning">申请退款</button>
        </div>
      </div>

      <!-- 退款弹窗 -->
      <div v-if="showRefundDialog" class="modal-overlay" @click.self="showRefundDialog = false">
        <div class="modal">
          <h2 class="modal-title">申请退款</h2>
          <form @submit.prevent="submitRefund">
            <div class="form-group">
              <label>退款类型</label>
              <select v-model="refundForm.type">
                <option :value="1">仅退款</option>
                <option :value="2">退货退款</option>
              </select>
            </div>
            <div class="form-group">
              <label>退款原因</label>
              <select v-model="refundForm.reason">
                <option value="">请选择</option>
                <option value="不想要了">不想要了</option>
                <option value="商品与描述不符">商品与描述不符</option>
                <option value="质量问题">质量问题</option>
                <option value="物流问题">物流问题</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-group">
              <label>退款金额（最多 ¥{{ order.totalAmount.toFixed(2) }}）</label>
              <input type="number" v-model="refundForm.refundAmount" step="0.01" :max="order.totalAmount" min="0.01" required />
            </div>
            <div class="form-group">
              <label>问题描述（选填）</label>
              <textarea v-model="refundForm.description" placeholder="请描述您的问题" rows="3"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" @click="showRefundDialog = false" class="btn btn-secondary">取消</button>
              <button type="submit" class="btn btn-primary">提交申请</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 评价区域 -->
      <div v-if="order.status === 3" class="review-section">
        <h2 class="section-title">评价商品</h2>
        <div v-for="item in order.items" :key="item.id" class="review-item">
          <div class="review-product">
            <span>{{ item.productName }}</span>
            <span>¥{{ item.unitPrice.toFixed(2) }} x {{ item.quantity }}</span>
          </div>
          <div class="review-form">
            <div class="rating-selector">
              <span v-for="i in 5" :key="i" @click="setRating(item.productId, i)" class="star" :class="{ active: (ratings[item.productId] || 0) >= i }">★</span>
            </div>
            <textarea v-model="contents[item.productId]" placeholder="请输入评价内容" class="review-input"></textarea>
            <button @click="submitReview(item.productId)" class="btn btn-primary btn-small">提交评价</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading">
    <p>加载中...</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const ratings = ref({})
const contents = ref({})
const now = ref(Date.now())
const showRefundDialog = ref(false)
const refundForm = ref({ type: 1, reason: '', refundAmount: 0, description: '' })
let timer = null

const remainingMs = computed(() => {
  if (!order.value || order.value.status !== 0) return 0
  const created = new Date(order.value.createdAt).getTime()
  const remaining = created + 12 * 60 * 60 * 1000 - now.value
  return remaining > 0 ? remaining : 0
})

const formatCountdown = (ms) => {
  if (ms <= 0) return '已超时'
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${hours}时${String(minutes).padStart(2, '0')}分${String(seconds).padStart(2, '0')}秒`
}

const fetchOrder = async () => {
  try {
    const res = await api.get(`/api/orders/${route.params.id}`)
    if (res.data.code === 200) {
      order.value = res.data.data
    }
  } catch (e) {
    console.error('获取订单失败', e)
  }
}

const payOrder = async () => {
  if (!confirm('确定付款吗？')) return
  try {
    const res = await api.put(`/api/orders/${order.value.id}/pay`)
    if (res.data.code === 200) {
      alert('付款成功')
      fetchOrder()
    }
  } catch (e) {
    alert(e.message || '付款失败')
  }
}

const cancelOrder = async () => {
  if (!confirm('确定取消订单吗？')) return
  try {
    const res = await api.put(`/api/orders/${order.value.id}/cancel`)
    if (res.data.code === 200) {
      alert('取消成功')
      fetchOrder()
    }
  } catch (e) {
    alert(e.message || '取消失败')
  }
}

const confirmOrder = async () => {
  if (!confirm('确定已收到商品吗？')) return
  try {
    const res = await api.put(`/api/orders/${order.value.id}/confirm`)
    if (res.data.code === 200) {
      alert('确认收货成功')
      fetchOrder()
    }
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

const setRating = (productId, rating) => {
  ratings.value[productId] = rating
}

const submitReview = async (productId) => {
  const rating = ratings.value[productId]
  const content = contents.value[productId]

  if (!rating) {
    alert('请选择评分')
    return
  }

  try {
    const res = await api.post('/api/reviews', {
      orderId: order.value.id,
      productId,
      rating,
      content
    })
    if (res.data.code === 200) {
      alert('评价成功')
      contents.value[productId] = ''
      ratings.value[productId] = 0
    }
  } catch (e) {
    alert(e.message || '评价失败')
  }
}

const openRefundDialog = () => {
  refundForm.value = { type: 1, reason: '', refundAmount: order.value.totalAmount, description: '' }
  showRefundDialog.value = true
}

const submitRefund = async () => {
  if (!refundForm.value.reason) { alert('请选择退款原因'); return }
  if (refundForm.value.refundAmount > order.value.totalAmount) { alert('退款金额不能超过订单总额'); return }
  try {
    const res = await api.post('/api/refunds', {
      orderId: order.value.id,
      type: refundForm.value.type,
      reason: refundForm.value.reason,
      refundAmount: refundForm.value.refundAmount,
      description: refundForm.value.description
    })
    if (res.data.code === 200) {
      alert('退款申请已提交')
      showRefundDialog.value = false
    }
  } catch (e) {
    alert(e.response?.data?.message || '申请失败')
  }
}

onMounted(() => {
  fetchOrder()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.page-title {
  font-size: 28px;
  margin: 30px 0 20px;
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

.order-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.countdown {
  font-size: 13px;
  color: #e67e22;
  font-weight: bold;
  background: #fff8f0;
  padding: 4px 12px;
  border-radius: 12px;
}

.countdown.urgent {
  color: #e63946;
  background: #fff0f0;
  animation: blink 1s infinite;
}

@keyframes blink {
  50% { opacity: 0.5; }
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

.item-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
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

.section-title {
  font-size: 24px;
  margin: 40px 0 20px;
  color: #333;
}

.review-item {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.review-product {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 14px;
}

.rating-selector {
  margin-bottom: 10px;
}

.star {
  font-size: 24px;
  color: #ddd;
  cursor: pointer;
  transition: color 0.3s;
}

.star.active {
  color: #ffc107;
}

.review-input {
  width: 100%;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 10px;
  font-family: inherit;
  resize: vertical;
}

.btn-small {
  padding: 8px 20px;
  font-size: 14px;
}

.btn-warning {
  background: #e67e22;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-warning:hover {
  background: #d35400;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 12px;
  padding: 30px;
  width: 450px;
  max-width: 90vw;
}

.modal-title {
  font-size: 20px;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
}

.form-group select,
.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  font-family: inherit;
}

.form-actions {
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
