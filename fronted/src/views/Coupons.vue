<template>
  <div class="coupons-page container">
    <h2 class="page-title">优惠券中心</h2>

    <div class="tabs">
      <button :class="{ active: tab === 'available' }" @click="tab = 'available'">可领取</button>
      <button :class="{ active: tab === 'my' }" @click="tab = 'my'">我的优惠券</button>
    </div>

    <div v-if="tab === 'available'" class="coupon-list">
      <div v-for="c in availableCoupons" :key="c.id" class="coupon-card">
        <div class="coupon-info">
          <h3>{{ c.name }}</h3>
          <p class="coupon-desc">
            {{ c.type === 1 ? '满' + c.minAmount + '减' + c.discountValue : c.discountValue * 10 + '折' }}
            | 有效期至 {{ formatDate(c.endTime) }}
          </p>
        </div>
        <button class="btn btn-primary" @click="claimCoupon(c.id)">领取</button>
      </div>
      <div v-if="availableCoupons.length === 0" class="empty">暂无可领取优惠券</div>
    </div>

    <div v-if="tab === 'my'" class="coupon-list">
      <div class="filter-bar">
        <button :class="{ active: filterStatus === null }" @click="filterStatus = null; loadMyCoupons()">全部</button>
        <button :class="{ active: filterStatus === 0 }" @click="filterStatus = 0; loadMyCoupons()">未使用</button>
        <button :class="{ active: filterStatus === 1 }" @click="filterStatus = 1; loadMyCoupons()">已使用</button>
      </div>
      <div v-for="c in myCoupons" :key="c.id" class="coupon-card my-coupon" :class="'status-' + c.status">
        <div class="coupon-info">
          <h3>{{ c.couponName }}</h3>
          <p class="coupon-desc">
            {{ c.type === 1 ? '满' + c.minAmount + '减' + c.discountValue : c.discountValue * 10 + '折' }}
          </p>
          <p class="coupon-status">{{ ['未使用', '已使用', '已过期'][c.status] }}</p>
        </div>
      </div>
      <div v-if="myCoupons.length === 0" class="empty">暂无优惠券</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const tab = ref('available')
const availableCoupons = ref([])
const myCoupons = ref([])
const filterStatus = ref(null)

const formatDate = (d) => d ? d.split('T')[0] : ''

const loadAvailable = async () => {
  try {
    const res = await api.get('/api/coupons')
    if (res.data.code === 200) availableCoupons.value = res.data.data
  } catch (e) { console.error(e) }
}

const loadMyCoupons = async () => {
  try {
    const params = {}
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await api.get('/api/coupons/my', { params })
    if (res.data.code === 200) myCoupons.value = res.data.data
  } catch (e) { console.error(e) }
}

const claimCoupon = async (couponId) => {
  try {
    const res = await api.post(`/api/coupons/claim/${couponId}`)
    if (res.data.code === 200) {
      alert('领取成功')
      loadAvailable()
      loadMyCoupons()
    } else {
      alert(res.data.message)
    }
  } catch (e) { alert(e.response?.data?.message || '领取失败') }
}

onMounted(() => {
  loadAvailable()
  loadMyCoupons()
})
</script>

<style scoped>
.coupons-page { padding: 30px 20px; max-width: 800px; margin: 0 auto; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.tabs { display: flex; gap: 10px; margin-bottom: 20px; }
.tabs button { padding: 8px 20px; border: 1px solid #ddd; background: white; cursor: pointer; border-radius: 4px; }
.tabs button.active { background: #000000; color: white; border-color: #000000; }
.coupon-list { display: flex; flex-direction: column; gap: 12px; }
.coupon-card { display: flex; justify-content: space-between; align-items: center; padding: 20px; border: 1px solid #eee; border-radius: 8px; background: white; }
.coupon-info h3 { margin: 0 0 5px; font-size: 16px; }
.coupon-desc { margin: 0; color: #666; font-size: 14px; }
.coupon-status { margin: 5px 0 0; font-size: 12px; color: #999; }
.my-coupon.status-0 { border-left: 4px solid #000000; }
.my-coupon.status-1 { border-left: 4px solid #999; opacity: 0.6; }
.my-coupon.status-2 { border-left: 4px solid #ccc; opacity: 0.4; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 15px; }
.filter-bar button { padding: 6px 16px; border: 1px solid #ddd; background: white; cursor: pointer; border-radius: 4px; }
.filter-bar button.active { background: #000000; color: white; border-color: #000000; }
.empty { text-align: center; padding: 40px; color: #999; }
</style>
