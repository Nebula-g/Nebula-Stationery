<template>
  <div class="group-buy-page container">
    <h2 class="page-title">拼团专区</h2>

    <div class="group-grid">
      <div v-for="item in groupBuys" :key="item.id" class="group-card">
        <img :src="item.imageUrl" :alt="item.productName" class="group-img" />
        <div class="group-info">
          <h3>{{ item.productName }}</h3>
          <div class="price-row">
            <span class="group-price">&yen;{{ item.groupPrice }}</span>
            <span class="original-price">&yen;{{ item.originalPrice }}</span>
          </div>
          <p class="group-desc">{{ item.minGroupSize }}人成团</p>
          <button class="btn btn-primary btn-block" @click="viewSessions(item.id)">查看拼团</button>
        </div>
      </div>
    </div>

    <div v-if="groupBuys.length === 0" class="empty">暂无拼团活动</div>

    <!-- 拼团会话弹窗 -->
    <div v-if="showSessions" class="modal-overlay" @click.self="showSessions = false">
      <div class="modal">
        <h3>{{ selectedProduct?.productName }} - 拼团列表</h3>
        <p class="warning-text">注意：拼团队伍最长存在24小时，超时将自动取消</p>
        <button class="btn btn-primary" @click="createSession">开新团</button>
        <div class="session-list">
          <div v-for="s in sessions" :key="s.id" class="session-card">
            <div class="session-info">
              <span>已参团 {{ s.currentSize }}/{{ s.minSize }} 人</span>
              <span :class="'status-' + s.status">{{ ['拼团中', '已成团', '已失败'][s.status] }}</span>
            </div>
            <p class="expire-text">截止: {{ s.expireTime?.replace('T', ' ')?.split('.')[0] }}</p>
            <button v-if="s.status === 0" class="btn btn-primary" @click="joinSession(s.id)">加入拼团</button>
          </div>
          <div v-if="sessions.length === 0" class="empty">暂无进行中的拼团</div>
        </div>
        <button class="btn btn-secondary" @click="showSessions = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import toast from '../utils/toast'

const groupBuys = ref([])
const sessions = ref([])
const showSessions = ref(false)
const selectedProduct = ref(null)
const selectedGroupBuyId = ref(null)

const loadGroupBuys = async () => {
  try {
    const res = await api.get('/api/group-buy')
    if (res.data.code === 200) groupBuys.value = res.data.data
  } catch (e) { console.error(e) }
}

const viewSessions = async (groupBuyId) => {
  selectedGroupBuyId.value = groupBuyId
  selectedProduct.value = groupBuys.value.find(g => g.id === groupBuyId)
  try {
    const res = await api.get(`/api/group-buy/sessions/${groupBuyId}`)
    if (res.data.code === 200) sessions.value = res.data.data
  } catch (e) { console.error(e) }
  showSessions.value = true
}

const createSession = async () => {
  try {
    const res = await api.post('/api/group-buy/sessions', null, { params: { groupBuyId: selectedGroupBuyId.value } })
    if (res.data.code === 200) {
      toast.success('开团成功！请在24小时内邀请好友参团')
      viewSessions(selectedGroupBuyId.value)
    } else {
      toast.success(res.data.message)
    }
  } catch (e) { toast.error(e.response?.data?.message || '开团失败') }
}

const joinSession = async (sessionId) => {
  try {
    const res = await api.post(`/api/group-buy/sessions/${sessionId}/join`)
    if (res.data.code === 200) {
      if (res.data.data.status === 1) {
        toast.success('拼团成功！')
      } else {
        toast.success('加入成功！当前' + res.data.data.currentSize + '人')
      }
      viewSessions(selectedGroupBuyId.value)
    } else {
      toast.success(res.data.message)
    }
  } catch (e) { toast.error(e.response?.data?.message || '参团失败') }
}

onMounted(loadGroupBuys)
</script>

<style scoped>
.group-buy-page { padding: 30px 20px; max-width: 1000px; margin: 0 auto; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.group-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.group-card { border: 1px solid #eee; border-radius: 8px; overflow: hidden; background: white; }
.group-img { width: 100%; height: 200px; object-fit: cover; }
.group-info { padding: 15px; }
.group-info h3 { margin: 0 0 10px; font-size: 16px; }
.price-row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 10px; }
.group-price { font-size: 24px; color: #e74c3c; font-weight: bold; }
.original-price { font-size: 14px; color: #999; text-decoration: line-through; }
.group-desc { font-size: 14px; color: #666; margin: 0 0 10px; }
.btn-block { width: 100%; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal { background: white; padding: 30px; border-radius: 8px; max-width: 500px; width: 90%; max-height: 80vh; overflow-y: auto; }
.modal h3 { margin: 0 0 15px; }
.modal .btn { margin-bottom: 15px; }
.warning-text { font-size: 13px; color: #e74c3c; margin-bottom: 15px; padding: 8px 12px; background: #fff5f5; border-radius: 4px; }
.session-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px; }
.session-card { padding: 15px; border: 1px solid #eee; border-radius: 8px; }
.session-info { display: flex; justify-content: space-between; margin-bottom: 5px; }
.status-0 { color: #e74c3c; }
.status-1 { color: #27ae60; }
.status-2 { color: #999; }
.expire-text { font-size: 12px; color: #999; margin: 0 0 10px; }
.empty { text-align: center; padding: 40px; color: #999; }
</style>
