<template>
  <div class="stats-page">
    <div class="page-head">
      <h1 class="page-title">数据统计</h1>
      <div class="export-bar">
        <button class="export-btn" :disabled="exporting" @click="doExport('products')">
          {{ exporting === 'products' ? '导出中…' : '📗 导出商品销量' }}
        </button>
        <button class="export-btn" :disabled="exporting" @click="doExport('sales')">
          {{ exporting === 'sales' ? '导出中…' : '📘 导出销售报表' }}
        </button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">今日订单</div>
        <div class="kpi-value">{{ overview.todayOrderCount ?? '-' }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">今日成交额</div>
        <div class="kpi-value accent">¥{{ money(overview.todayRevenue) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">累计成交额</div>
        <div class="kpi-value">¥{{ money(overview.totalRevenue) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">累计订单</div>
        <div class="kpi-value">{{ overview.totalOrderCount ?? '-' }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">待发货</div>
        <div class="kpi-value warn">{{ overview.pendingShipCount ?? '-' }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">低库存预警</div>
        <div class="kpi-value danger">{{ lowStock.length }}</div>
      </div>
    </div>

    <section class="panel">
      <div class="panel-head">
        <h2>销售趋势</h2>
        <div class="range-switch">
          <button
            v-for="d in dayOptions"
            :key="d"
            class="range-btn"
            :class="{ active: days === d }"
            @click="switchDays(d)"
          >近{{ d }}天</button>
        </div>
      </div>
      <div ref="trendRef" class="chart chart-lg"></div>
    </section>

    <div class="panel-row">
      <section class="panel">
        <div class="panel-head"><h2>分类销量占比</h2></div>
        <div ref="categoryRef" class="chart"></div>
      </section>
      <section class="panel">
        <div class="panel-head"><h2>商品销量排行 TOP10</h2></div>
        <div ref="productRef" class="chart"></div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-head">
        <h2>订单导出</h2>
        <div class="export-filters">
          <select v-model="orderStatus" class="filter-select">
            <option :value="null">全部状态</option>
            <option :value="0">待支付</option>
            <option :value="1">已支付</option>
            <option :value="2">已发货</option>
            <option :value="3">已完成</option>
            <option :value="4">已取消</option>
          </select>
          <input type="date" v-model="startDate" class="filter-date" />
          <span class="tilde">至</span>
          <input type="date" v-model="endDate" class="filter-date" />
          <button class="export-btn" :disabled="exporting" @click="doExport('orders')">
            {{ exporting === 'orders' ? '导出中…' : '📙 导出订单明细' }}
          </button>
        </div>
      </div>
      <p class="hint">导出的 .xlsx 含订单号、下单用户、商品明细、金额、状态与下单时间。</p>
    </section>

    <section class="panel">
      <div class="panel-head"><h2>低库存商品（≤10 件）</h2></div>
      <table v-if="lowStock.length" class="data-table">
        <thead>
          <tr><th>商品名称</th><th>分类</th><th>当前库存</th><th>累计销量</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in lowStock" :key="item.id">
            <td>{{ item.name }}</td>
            <td>{{ item.categoryName }}</td>
            <td :class="item.stock === 0 ? 'danger' : 'warn'">{{ item.stock }}</td>
            <td>{{ item.soldCount }}</td>
            <td><router-link :to="`/admin/products/${item.id}/edit`" class="link">去补货</router-link></td>
          </tr>
        </tbody>
      </table>
      <p v-else class="hint">库存充足，暂无预警商品。</p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import api from '../../api'
import { downloadExcel } from '../../utils/download'
import toast from '../../utils/toast'

echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const dayOptions = [7, 15, 30]

const overview = ref({})
const lowStock = ref([])
const days = ref(7)
const exporting = ref('')

const orderStatus = ref(null)
const startDate = ref('')
const endDate = ref('')

const trendRef = ref(null)
const categoryRef = ref(null)
const productRef = ref(null)

const charts = {}
const PALETTE = ['#e63946', '#2a9d8f', '#457b9d', '#f4a261', '#6d597a', '#90a955']

const money = (value) => (value == null ? '-' : Number(value).toFixed(2))

const initChart = (key, el) => {
  if (!el) return null
  charts[key] = echarts.init(el)
  return charts[key]
}

const resizeAll = () => Object.values(charts).forEach((c) => c && c.resize())

const loadOverview = async () => {
  const res = await api.get('/api/admin/stats/overview')
  if (res.data.code === 200) overview.value = res.data.data
}

const loadLowStock = async () => {
  const res = await api.get('/api/admin/stats/low-stock')
  if (res.data.code === 200) lowStock.value = res.data.data
}

const renderTrend = async (trend) => {
  await nextTick()
  let chart = charts.trend || initChart('trend', trendRef.value)
  if (!chart) return
  chart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '成交额'], right: 0 },
    grid: { left: 55, right: 55, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trend.map((d) => d.date.slice(5)),
      axisLine: { lineStyle: { color: '#ccc' } }
    },
    yAxis: [
      { type: 'value', name: '订单数', minInterval: 1, splitLine: { lineStyle: { color: '#f0f0f0' } } },
      { type: 'value', name: '成交额', splitLine: { show: false } }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: { color: '#457b9d', borderRadius: [4, 4, 0, 0] },
        data: trend.map((d) => d.orderCount)
      },
      {
        name: '成交额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        itemStyle: { color: '#e63946' },
        areaStyle: { color: 'rgba(230, 57, 70, 0.10)' },
        data: trend.map((d) => Number(d.revenue))
      }
    ]
  })
}

const renderCategory = async (rows) => {
  await nextTick()
  let chart = charts.category || initChart('category', categoryRef.value)
  if (!chart) return
  chart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}<br/>销量 {c}（{d}%）' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%' },
        data: rows.map((r) => ({ name: r.name, value: r.soldCount }))
      }
    ]
  })
}

const renderTopProducts = async (rows) => {
  await nextTick()
  let chart = charts.product || initChart('product', productRef.value)
  if (!chart) return
  const sorted = [...rows].reverse()
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 30, top: 20, bottom: 20, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    yAxis: {
      type: 'category',
      data: sorted.map((r) => r.name),
      axisLabel: {
        width: 96,
        overflow: 'truncate'
      }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#2a9d8f', borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right' },
        data: sorted.map((r) => r.soldCount)
      }
    ]
  })
}

const loadCharts = async () => {
  const [trend, category, top] = await Promise.all([
    api.get('/api/admin/stats/sales-trend', { params: { days: days.value } }),
    api.get('/api/admin/stats/category-sales'),
    api.get('/api/admin/stats/top-products', { params: { limit: 10 } })
  ])
  if (trend.data.code === 200) renderTrend(trend.data.data)
  if (category.data.code === 200) renderCategory(category.data.data)
  if (top.data.code === 200) renderTopProducts(top.data.data)
}

const switchDays = (d) => {
  days.value = d
  loadCharts()
}

const doExport = async (kind) => {
  exporting.value = kind
  try {
    if (kind === 'orders') {
      await downloadExcel('/api/admin/export/orders', {
        status: orderStatus.value ?? undefined,
        startDate: startDate.value || undefined,
        endDate: endDate.value || undefined
      }, '订单明细.xlsx')
    } else if (kind === 'products') {
      await downloadExcel('/api/admin/export/products', {}, '商品销量.xlsx')
    } else {
      await downloadExcel('/api/admin/export/sales-report', { days: 30 }, '销售报表.xlsx')
    }
  } catch (e) {
    toast.error(e.message || '导出失败，请重试')
  } finally {
    exporting.value = ''
  }
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadLowStock()])
  await loadCharts()
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  Object.values(charts).forEach((c) => c && c.dispose())
})
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

.export-bar {
  display: flex;
  gap: 10px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.kpi-card {
  background: white;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.kpi-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 24px;
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

.panel {
  background: white;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.panel-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.panel-row .panel {
  margin-bottom: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.panel-head h2 {
  font-size: 16px;
  color: #333;
}

.range-switch {
  display: flex;
  gap: 6px;
}

.range-btn {
  border: 1px solid #ddd;
  background: white;
  border-radius: 14px;
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
}

.range-btn.active {
  background: #333;
  color: white;
  border-color: #333;
}

.chart {
  width: 100%;
  height: 300px;
}

.chart-lg {
  height: 320px;
}

.export-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-select,
.filter-date {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 13px;
  background: white;
  color: #333;
}

.tilde {
  font-size: 13px;
  color: #999;
}

.export-btn {
  border: 1px solid #2a9d8f;
  background: #2a9d8f;
  color: white;
  border-radius: 6px;
  padding: 7px 14px;
  font-size: 13px;
  cursor: pointer;
}

.export-btn:hover {
  background: #22867a;
}

.export-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hint {
  font-size: 13px;
  color: #999;
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
  font-weight: bold;
}

.link {
  color: #457b9d;
  text-decoration: none;
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .panel-row {
    grid-template-columns: 1fr;
  }
}
</style>
