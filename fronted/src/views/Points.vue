<template>
  <div class="points-page container">
    <h2 class="page-title">我的积分</h2>

    <div class="points-summary">
      <div class="points-card">
        <div class="points-number">{{ pointsInfo.availablePoints }}</div>
        <div class="points-label">可用积分</div>
      </div>
      <div class="points-card">
        <div class="points-number">{{ pointsInfo.totalPoints }}</div>
        <div class="points-label">累计积分</div>
      </div>
      <div class="points-card">
        <div class="points-number">{{ pointsInfo.usedPoints }}</div>
        <div class="points-label">已用积分</div>
      </div>
    </div>

    <div class="actions">
      <button class="btn btn-primary" @click="signIn">{{ signedIn ? '今日已签到' : '每日签到' }}</button>
      <div class="redeem-box">
        <input v-model.number="redeemPoints" type="number" placeholder="输入积分数量" class="input" />
        <button class="btn btn-secondary" @click="redeem">积分兑换</button>
      </div>
    </div>

    <div class="rules-section">
      <h3>积分规则</h3>
      <ul>
        <li v-for="r in rules" :key="r.ruleKey">{{ r.description }}</li>
      </ul>
    </div>

    <div class="log-section">
      <h3>积分流水</h3>
      <div v-for="log in pointLog" :key="log.id" class="log-item">
        <span :class="log.points > 0 ? 'gain' : 'lose'">{{ log.points > 0 ? '+' : '' }}{{ log.points }}</span>
        <span class="log-desc">{{ log.description }}</span>
        <span class="log-time">{{ log.createdAt?.replace('T', ' ')?.split('.')[0] }}</span>
      </div>
      <div v-if="pointLog.length === 0" class="empty">暂无积分记录</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const pointsInfo = ref({ availablePoints: 0, totalPoints: 0, usedPoints: 0 })
const pointLog = ref([])
const rules = ref([])
const redeemPoints = ref(100)
const signedIn = ref(false)

const loadPoints = async () => {
  try {
    const res = await api.get('/api/points/my')
    if (res.data.code === 200) pointsInfo.value = res.data.data
  } catch (e) { console.error(e) }
}

const loadLog = async () => {
  try {
    const res = await api.get('/api/points/log')
    if (res.data.code === 200) pointLog.value = res.data.data
  } catch (e) { console.error(e) }
}

const loadRules = async () => {
  try {
    const res = await api.get('/api/points/rules')
    if (res.data.code === 200) rules.value = res.data.data
  } catch (e) { console.error(e) }
}

const signIn = async () => {
  if (signedIn.value) return
  try {
    const res = await api.post('/api/points/sign-in')
    if (res.data.code === 200) {
      alert('签到成功！获得' + res.data.data.points + '积分')
      signedIn.value = true
      loadPoints()
      loadLog()
    } else {
      alert(res.data.message)
    }
  } catch (e) { alert(e.response?.data?.message || '签到失败') }
}

const redeem = async () => {
  if (!redeemPoints.value || redeemPoints.value <= 0) {
    alert('请输入有效的积分数量')
    return
  }
  try {
    const res = await api.post('/api/points/redeem', null, { params: { points: redeemPoints.value } })
    if (res.data.code === 200) {
      alert('兑换成功！抵扣¥' + res.data.data.money + '，剩余' + res.data.data.remainingPoints + '积分')
      loadPoints()
      loadLog()
    } else {
      alert(res.data.message)
    }
  } catch (e) { alert(e.response?.data?.message || '兑换失败') }
}

onMounted(() => {
  loadPoints()
  loadLog()
  loadRules()
})
</script>

<style scoped>
.points-page { padding: 30px 20px; max-width: 800px; margin: 0 auto; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.points-summary { display: flex; gap: 20px; margin-bottom: 30px; }
.points-card { flex: 1; text-align: center; padding: 25px; border: 1px solid #eee; border-radius: 8px; background: white; }
.points-number { font-size: 36px; font-weight: bold; color: #000000; }
.points-label { font-size: 14px; color: #666; margin-top: 5px; }
.actions { display: flex; gap: 20px; align-items: center; margin-bottom: 30px; flex-wrap: wrap; }
.redeem-box { display: flex; gap: 10px; align-items: center; }
.input { width: 150px; padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; }
.rules-section { margin-bottom: 30px; }
.rules-section h3 { margin-bottom: 10px; }
.rules-section ul { padding-left: 20px; color: #666; }
.rules-section li { margin-bottom: 5px; }
.log-section h3 { margin-bottom: 10px; }
.log-item { display: flex; align-items: center; gap: 15px; padding: 12px; border-bottom: 1px solid #eee; }
.gain { color: #27ae60; font-weight: bold; min-width: 60px; }
.lose { color: #e74c3c; font-weight: bold; min-width: 60px; }
.log-desc { flex: 1; color: #333; }
.log-time { color: #999; font-size: 12px; }
.empty { text-align: center; padding: 40px; color: #999; }
</style>
