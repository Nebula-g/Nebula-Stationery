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

        <p v-if="nextStepHint" class="order-hint">{{ nextStepHint }}</p>

        <div class="order-actions">
          <router-link to="/orders" class="btn btn-secondary">返回订单列表</router-link>
          <button v-if="order.status === 0" @click="payOrder" class="btn btn-primary">立即付款</button>
          <button v-if="order.status === 0" @click="cancelOrder" class="btn btn-danger">取消订单</button>
          <button v-if="order.status === 1 || order.status === 2" @click="confirmOrder" class="btn btn-primary">确认收货</button>
          <button v-if="order.status === 3" @click="goReviews" class="btn btn-warning">评价商品</button>
          <button v-if="order.status === 1 || order.status === 3" @click="openRefundDialog" class="btn btn-secondary">申请退款</button>
        </div>
      </div>

      <!-- 物流信息 -->
      <div v-if="order.logistics" class="logistics-card">
        <div class="logistics-head">
          <div>
            <h2 class="section-title">物流信息</h2>
            <p class="logistics-meta">
              {{ order.logistics.companyName }} · 运单号 {{ order.logistics.trackingNo }}
              <span class="logistics-status" :class="{ done: order.logistics.status === 1 }">{{ order.logistics.statusText }}</span>
            </p>
          </div>
          <div class="logistics-actions">
            <span class="provider-tag">{{ order.logistics.provider }}</span>
            <button class="refresh-btn" :disabled="refreshingLogistics" @click="refreshLogistics">
              {{ refreshingLogistics ? '刷新中…' : '刷新物流' }}
            </button>
          </div>
        </div>
        <ul v-if="order.logistics.tracks?.length" class="timeline">
          <li v-for="(track, idx) in order.logistics.tracks" :key="idx" :class="{ latest: idx === 0 }">
            <span class="dot"></span>
            <div class="track-body">
              <p class="track-desc">{{ track.description }}</p>
              <p class="track-meta">{{ track.time }}<span v-if="track.location"> · {{ track.location }}</span></p>
            </div>
          </li>
        </ul>
        <p v-else class="track-empty">快递还没揽收，稍后刷新看看。</p>
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
      <div v-if="order.status === 3" id="reviews" class="review-section" :class="{ 'flash-target': flashReviews }">
        <h2 class="section-title">评价商品</h2>
        <div v-for="item in order.items" :key="item.id" class="review-item">
          <div class="review-product">
            <span>{{ item.productName }}</span>
            <span>¥{{ item.unitPrice.toFixed(2) }} x {{ item.quantity }}</span>
          </div>

          <!-- 已评价：展示内容 + 追评入口 -->
          <div v-if="reviewOf(item.productId)" class="review-done">
            <div class="done-head">
              <span class="done-stars">
                <span v-for="i in 5" :key="i" class="star" :class="{ active: i <= reviewOf(item.productId).rating }">★</span>
              </span>
              <span class="done-time">{{ reviewOf(item.productId).createdAt }}</span>
            </div>
            <p class="done-content">{{ reviewOf(item.productId).content || '（未填写文字评价）' }}</p>
            <div v-if="reviewOf(item.productId).imageUrls?.length" class="done-images">
              <img v-for="url in reviewOf(item.productId).imageUrls" :key="url" :src="url" alt="评价图片" />
            </div>

            <div v-if="reviewOf(item.productId).append" class="append-done">
              <span class="append-tag">追评</span>
              <span class="done-time">{{ reviewOf(item.productId).append.createdAt }}</span>
              <p class="done-content">{{ reviewOf(item.productId).append.content }}</p>
              <div v-if="reviewOf(item.productId).append.imageUrls?.length" class="done-images">
                <img v-for="url in reviewOf(item.productId).append.imageUrls" :key="url" :src="url" alt="追评图片" />
              </div>
            </div>
            <div v-else class="append-form">
              <button type="button" class="append-toggle" @click="toggleAppend(item.productId)">
                {{ appendOpen[item.productId] ? '收起追评' : '＋ 写追评' }}
              </button>
              <div v-if="appendOpen[item.productId]" class="append-inner">
                <textarea
                  v-model="appendContents[item.productId]"
                  class="review-input"
                  placeholder="使用一段时间后的感受，最多 500 字"
                ></textarea>
                <ImageUploader v-model="appendImages[item.productId]" />
                <button
                  type="button"
                  class="btn btn-primary btn-small"
                  :disabled="submitting === 'append-' + item.productId"
                  @click="submitAppend(item.productId)"
                >提交追评</button>
              </div>
            </div>
          </div>

          <!-- 未评价：撰写评价 -->
          <div v-else class="review-form">
            <div class="rating-selector">
              <span
                v-for="i in 5"
                :key="i"
                @click="setRating(item.productId, i)"
                class="star"
                :class="{ active: (ratings[item.productId] || 0) >= i }"
              >★</span>
            </div>
            <textarea
              v-model="contents[item.productId]"
              placeholder="请输入评价内容，不含敏感词的内容才会完整展示"
              class="review-input"
            ></textarea>
            <ImageUploader v-model="images[item.productId]" />
            <button
              type="button"
              @click="submitReview(item.productId)"
              class="btn btn-primary btn-small"
              :disabled="submitting === 'review-' + item.productId"
            >提交评价</button>
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
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import ImageUploader from '../components/ImageUploader.vue'
import api from '../api'
import toast from '../utils/toast'
import confirmAction from '../utils/confirm'

const route = useRoute()

const order = ref(null)
const ratings = ref({})
const contents = ref({})
const images = ref({})
const myReviews = ref([])
const appendOpen = ref({})
const appendContents = ref({})
const appendImages = ref({})
const submitting = ref('')
const now = ref(Date.now())
const showRefundDialog = ref(false)
const refundForm = ref({ type: 1, reason: '', refundAmount: 0, description: '' })
const flashReviews = ref(false)
let timer = null

const reviewOf = (productId) => myReviews.value.find((r) => r.productId === productId)

const nextStepHint = computed(() => {
  if (!order.value) return ''
  if (order.value.status === 1) return '收到货后点「确认收货」，订单完成即可评价（商家发货后会显示物流单号）'
  if (order.value.status === 2) return '商品已发出，收到后点「确认收货」即可评价'
  if (order.value.status === 3) return '订单已完成，每件商品可评价一次，最多上传 6 张图片'
  return ''
})

const scrollToReviews = async () => {
  await nextTick()
  document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  flashReviews.value = true
  setTimeout(() => { flashReviews.value = false }, 2000)
}

const goReviews = () => scrollToReviews()

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

const refreshingLogistics = ref(false)

const refreshLogistics = async () => {
  refreshingLogistics.value = true
  try {
    const res = await api.get(`/api/orders/${route.params.id}/logistics`)
    if (res.data.code === 200 && res.data.data) {
      order.value.logistics = res.data.data
      toast.success('物流信息已更新')
    }
  } catch (e) {
    toast.error(e.message || '物流查询失败')
  } finally {
    refreshingLogistics.value = false
  }
}

const payOrder = async () => {
  if (!(await confirmAction('确定付款吗？', { confirmText: '确认付款' }))) return
  try {
    const res = await api.put(`/api/orders/${order.value.id}/pay`)
    if (res.data.code === 200) {
      toast.success('付款成功')
      fetchOrder()
    }
  } catch (e) {
    toast.error(e.message || '付款失败')
  }
}

const cancelOrder = async () => {
  if (!(await confirmAction('确定取消该订单吗？取消后不可恢复', { confirmText: '确认取消', cancelText: '暂不取消', danger: true }))) return
  try {
    const res = await api.put(`/api/orders/${order.value.id}/cancel`)
    if (res.data.code === 200) {
      toast.success('取消成功')
      fetchOrder()
    }
  } catch (e) {
    toast.error(e.message || '取消失败')
  }
}

const confirmOrder = async () => {
  if (!(await confirmAction('请确认已收到全部商品，确认后订单完成并可评价。', { confirmText: '确认收货' }))) return
  try {
    const res = await api.put(`/api/orders/${order.value.id}/confirm`)
    if (res.data.code === 200) {
      toast.success('确认收货成功')
      fetchOrder()
    }
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

const setRating = (productId, rating) => {
  ratings.value[productId] = rating
}

const fetchMyReviews = async () => {
  try {
    const res = await api.get('/api/reviews/mine', { params: { orderId: route.params.id } })
    if (res.data.code === 200) myReviews.value = res.data.data
  } catch (e) {
    myReviews.value = []
  }
}

/** 提交前询问敏感词，后端也会做兜底屏蔽 */
const confirmSensitive = async (text) => {
  if (!text || !text.trim()) return true
  try {
    const res = await api.post('/api/reviews/sensitive-check', { text })
    if (res.data.code !== 200 || !res.data.data.sensitive) return true
    return confirmAction(`评价内容包含不当用词，提交后将被屏蔽为：\n\n${res.data.data.filtered}\n\n仍要提交吗？`, {
      confirmText: '仍要提交',
      cancelText: '返回修改',
      danger: true
    })
  } catch (e) {
    return true
  }
}

const submitReview = async (productId) => {
  const rating = ratings.value[productId]
  const content = (contents.value[productId] || '').trim()

  if (!rating) {
    toast.error('请选择评分')
    return
  }
  if (!(await confirmSensitive(content))) return

  submitting.value = 'review-' + productId
  try {
    const res = await api.post('/api/reviews', {
      orderId: order.value.id,
      productId,
      rating,
      content,
      imageUrls: images.value[productId] || []
    })
    if (res.data.code === 200) {
      toast.success('评价成功')
      contents.value[productId] = ''
      images.value[productId] = []
      ratings.value[productId] = 0
      await fetchMyReviews()
    }
  } catch (e) {
    toast.error(e.response?.data?.message || '评价失败')
  } finally {
    submitting.value = ''
  }
}

const toggleAppend = (productId) => {
  appendOpen.value[productId] = !appendOpen.value[productId]
}

const submitAppend = async (productId) => {
  const review = reviewOf(productId)
  const content = (appendContents.value[productId] || '').trim()

  if (!content) {
    toast.error('请填写追评内容')
    return
  }
  if (!(await confirmSensitive(content))) return

  submitting.value = 'append-' + productId
  try {
    const res = await api.post('/api/reviews/append', {
      reviewId: review.id,
      content,
      imageUrls: appendImages.value[productId] || []
    })
    if (res.data.code === 200) {
      toast.success('追评成功')
      appendContents.value[productId] = ''
      appendImages.value[productId] = []
      appendOpen.value[productId] = false
      await fetchMyReviews()
    }
  } catch (e) {
    toast.error(e.response?.data?.message || '追评失败')
  } finally {
    submitting.value = ''
  }
}

const openRefundDialog = () => {
  refundForm.value = { type: 1, reason: '', refundAmount: order.value.totalAmount, description: '' }
  showRefundDialog.value = true
}

const submitRefund = async () => {
  if (!refundForm.value.reason) { toast.error('请选择退款原因'); return }
  if (refundForm.value.refundAmount > order.value.totalAmount) { toast.error('退款金额不能超过订单总额'); return }
  try {
    const res = await api.post('/api/refunds', {
      orderId: order.value.id,
      type: refundForm.value.type,
      reason: refundForm.value.reason,
      refundAmount: refundForm.value.refundAmount,
      description: refundForm.value.description
    })
    if (res.data.code === 200) {
      toast.success('退款申请已提交')
      showRefundDialog.value = false
    }
  } catch (e) {
    toast.error(e.response?.data?.message || '申请失败')
  }
}

onMounted(async () => {
  await fetchOrder()
  if (order.value?.status === 3) {
    await fetchMyReviews()
    if (route.hash === '#reviews') {
      await scrollToReviews()
    }
  }
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

.order-hint {
  margin: 16px 0 0;
  font-size: 13px;
  color: #999;
  text-align: right;
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
  margin-top: 10px;
}

.review-done {
  background: #fafafa;
  border-radius: 6px;
  padding: 14px;
}

.done-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.done-stars .star {
  font-size: 18px;
  cursor: default;
}

.done-time {
  font-size: 12px;
  color: #999;
}

.done-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin: 0 0 8px;
}

.done-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.done-images img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
  background: #eee;
}

.append-done {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ddd;
}

.append-tag {
  display: inline-block;
  background: #e63946;
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-right: 8px;
}

.append-form {
  margin-top: 12px;
}

.append-toggle {
  border: none;
  background: none;
  color: #457b9d;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.append-toggle:hover {
  text-decoration: underline;
}

.append-inner .review-input {
  margin-top: 10px;
}

/* 从订单列表点「评价商品」跳进来时，闪一下橙色边框指路 */
.review-section.flash-target {
  animation: reviews-flash 2s ease-out 1;
}

@keyframes reviews-flash {
  0%, 40% {
    background: #fff8ec;
    box-shadow: 0 0 0 2px #e67e22;
    border-radius: 8px;
  }
  100% {
    background: transparent;
    box-shadow: none;
  }
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

.logistics-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-top: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.logistics-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.logistics-card .section-title {
  margin: 0 0 6px;
  font-size: 16px;
}

.logistics-meta {
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

.logistics-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.provider-tag {
  font-size: 12px;
  color: #999;
  border: 1px dashed #ddd;
  border-radius: 10px;
  padding: 2px 8px;
}

.refresh-btn {
  border: 1px solid #ddd;
  background: white;
  color: #555;
  border-radius: 6px;
  padding: 5px 12px;
  font-size: 13px;
  cursor: pointer;
}

.refresh-btn:hover:not(:disabled) {
  border-color: #333;
  color: #333;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.timeline {
  list-style: none;
  margin: 18px 0 0;
  padding: 0 0 0 6px;
}

.timeline li {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 0 0 20px 0;
}

.timeline li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: 0;
  width: 1px;
  background: #eee;
}

.timeline .dot {
  flex: 0 0 11px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #ddd;
  margin-top: 4px;
  z-index: 1;
}

.timeline li.latest .dot {
  background: #2a9d8f;
  box-shadow: 0 0 0 3px rgba(42, 157, 143, 0.15);
}

.track-body {
  flex: 1;
}

.track-desc {
  font-size: 14px;
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
  margin-top: 4px;
}

.track-empty {
  font-size: 13px;
  color: #999;
  margin-top: 14px;
}
</style>
