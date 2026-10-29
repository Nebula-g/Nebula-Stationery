<template>
  <div class="ops-page">
    <div class="page-head">
      <h1 class="page-title">中间件监控</h1>
      <div class="head-actions">
        <label class="auto-refresh">
          <input type="checkbox" v-model="autoRefresh" />
          自动刷新（5 秒）
        </label>
        <button class="btn-mini" @click="refresh">立即刷新</button>
      </div>
    </div>

    <div v-if="!cache.backend && !queue.backend" class="panel">
      <p class="hint">正在读取监控数据…</p>
    </div>

    <template v-else>
      <section class="panel">
        <div class="panel-head">
          <h2>Redis 缓存</h2>
          <span class="badge" :class="isRedis(cache.backend) ? 'good' : 'warn'">{{ cache.backend }}</span>
        </div>
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-label">命中率</div>
            <div class="kpi-value accent">{{ cache.hitRate }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">命中次数</div>
            <div class="kpi-value">{{ cache.hit ?? '-' }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">未命中次数</div>
            <div class="kpi-value">{{ cache.miss ?? '-' }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">降级次数</div>
            <div class="kpi-value danger">{{ cache.failure ?? 0 }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">本地缓存条目</div>
            <div class="kpi-value">{{ cache.localEntries ?? 0 }}</div>
          </div>
        </div>
        <p class="hint">
          商品列表 / 详情 / 分类 / 搜索联想 / 热门词走缓存；后台改商品、下单扣库存、新增评价都会自动失效。
          没装 Redis 时自动改用进程内缓存顶上，页面不会报错。
        </p>
        <button class="btn-danger-mini" :disabled="flushing" @click="clearCache">
          {{ flushing ? '清空中…' : '清空全部缓存' }}
        </button>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h2>消息队列</h2>
          <span class="badge" :class="isRedis(queue.backend) ? 'good' : 'warn'">{{ queue.backend }}</span>
        </div>
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-label">已投递</div>
            <div class="kpi-value">{{ queue.published ?? 0 }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">已消费</div>
            <div class="kpi-value accent">{{ queue.consumed ?? 0 }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">处理失败</div>
            <div class="kpi-value warn">{{ queue.failed ?? 0 }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">死信</div>
            <div class="kpi-value danger">{{ queue.dead ?? 0 }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">延迟消息</div>
            <div class="kpi-value">{{ queue.delayed ?? 0 }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">进程内待处理</div>
            <div class="kpi-value">{{ queue.localPending ?? 0 }}</div>
          </div>
        </div>

        <table v-if="streamRows.length" class="data-table">
          <thead>
            <tr><th>队列（主题）</th><th>消息总数</th><th>待确认</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in streamRows" :key="row.name">
              <td>{{ row.name }}</td>
              <td>{{ row.length }}</td>
              <td :class="row.groupPending ? 'warn' : ''">{{ row.groupPending }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="hint">当前没有可统计的 Redis Stream（可能处于降级模式）。</p>

        <template v-if="deadLetters.length">
          <h3 class="sub-title">最近的死信</h3>
          <table class="data-table">
            <thead>
              <tr><th>时间</th><th>类型</th><th>重试</th><th>原因</th></tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in deadLetters" :key="idx">
                <td>{{ item.at }}</td>
                <td>{{ item.type }}</td>
                <td>{{ item.retries }}</td>
                <td class="danger">{{ item.error }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../../api'
import toast from '../../utils/toast'
import confirmAction from '../../utils/confirm'

const cache = ref({})
const queue = ref({})
const autoRefresh = ref(true)
const flushing = ref(false)

const isRedis = (backend) => !!backend && !String(backend).includes('降级')

const streamRows = computed(() => {
  const streams = queue.value.streams || {}
  return Object.keys(streams).map((name) => ({
    name,
    length: streams[name]?.length ?? 0,
    groupPending: streams[name]?.groupPending ?? 0
  }))
})

const deadLetters = computed(() => queue.value.recentDeadLetters || [])

const refresh = async () => {
  try {
    const [cacheRes, queueRes] = await Promise.all([
      api.get('/api/admin/ops/cache'),
      api.get('/api/admin/ops/queue')
    ])
    cache.value = cacheRes.data.data || {}
    queue.value = queueRes.data.data || {}
  } catch {
    // 拦截器已提示，保持上一次数据
  }
}

const clearCache = async () => {
  if (!(await confirmAction('清空后商品缓存会重新回源数据库，确认清空吗？', { confirmText: '确认清空', danger: true }))) {
    return
  }
  flushing.value = true
  try {
    const res = await api.delete('/api/admin/ops/cache')
    toast.success(`已清除 ${res.data.data?.removed ?? 0} 个缓存键`)
    await refresh()
  } finally {
    flushing.value = false
  }
}

let timer = null

onMounted(() => {
  refresh()
  timer = setInterval(() => {
    if (autoRefresh.value) refresh()
  }, 5000)
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  color: #333;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.panel {
  background: white;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel-head h2 {
  font-size: 16px;
  color: #333;
}

.badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 10px;
}

.badge.good {
  background: #e9f7ef;
  color: #1e7d4d;
}

.badge.warn {
  background: #fdf3e6;
  color: #b26a1b;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.kpi-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 14px;
}

.kpi-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}

.kpi-value {
  font-size: 22px;
  font-weight: bold;
  color: #333;
}

.kpi-value.accent {
  color: #2a9d8f;
}

.kpi-value.warn {
  color: #f4a261;
}

.kpi-value.danger {
  color: #e63946;
}

.sub-title {
  font-size: 14px;
  color: #333;
  margin: 18px 0 8px;
}

.hint {
  font-size: 13px;
  color: #999;
  line-height: 1.7;
  margin-bottom: 10px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  background: #fafafa;
  color: #666;
  font-weight: normal;
  font-size: 13px;
}

.data-table .warn {
  color: #f4a261;
  font-weight: bold;
}

.data-table .danger {
  color: #e63946;
}

.btn-mini,
.btn-danger-mini {
  border-radius: 6px;
  padding: 7px 14px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #2a9d8f;
  background: white;
  color: #2a9d8f;
}

.btn-mini:hover {
  background: #2a9d8f;
  color: white;
}

.btn-danger-mini {
  border-color: #e63946;
  color: #e63946;
}

.btn-danger-mini:hover:not(:disabled) {
  background: #e63946;
  color: white;
}

.btn-danger-mini:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
