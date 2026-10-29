<template>
  <div class="refunds-page">
    <div class="container">
      <h1 class="page-title">退款/售后</h1>

      <div v-if="refunds.length === 0" class="empty">
        <p>暂无退款记录</p>
      </div>

      <div v-for="refund in refunds" :key="refund.id" class="refund-card">
        <div class="refund-header">
          <div class="refund-info">
            <span class="refund-no">退款单号：{{ refund.refundNo }}</span>
            <span class="refund-date">申请时间：{{ refund.createdAt }}</span>
          </div>
          <span class="refund-status" :class="`status-${refund.status}`">{{ refund.statusText }}</span>
        </div>

        <div class="refund-body">
          <div class="refund-row">
            <span class="label">关联订单：</span>
            <span>{{ refund.orderNo }}</span>
          </div>
          <div class="refund-row">
            <span class="label">退款类型：</span>
            <span>{{ refund.typeText }}</span>
          </div>
          <div class="refund-row">
            <span class="label">退款原因：</span>
            <span>{{ refund.reason }}</span>
          </div>
          <div v-if="refund.description" class="refund-row">
            <span class="label">问题描述：</span>
            <span>{{ refund.description }}</span>
          </div>
          <div class="refund-row">
            <span class="label">退款金额：</span>
            <span class="refund-amount">¥{{ refund.refundAmount.toFixed(2) }}</span>
          </div>
          <div v-if="refund.adminRemark" class="refund-row">
            <span class="label">商家备注：</span>
            <span class="admin-remark">{{ refund.adminRemark }}</span>
          </div>
        </div>

        <div class="refund-actions">
          <button v-if="refund.status === 0" @click="cancelRefund(refund.id)" class="btn btn-danger btn-small">取消申请</button>
          <router-link :to="`/order/${refund.orderId}`" class="btn btn-secondary btn-small">查看订单</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const refunds = ref([])

const fetchRefunds = async () => {
  try {
    const res = await api.get('/api/refunds')
    if (res.data.code === 200) {
      refunds.value = res.data.data
    }
  } catch (e) {
    console.error('获取退款列表失败', e)
  }
}

const cancelRefund = async (id) => {
  if (!confirm('确定取消该退款申请吗？')) return
  try {
    const res = await api.put(`/api/refunds/${id}/cancel`)
    if (res.data.code === 200) {
      alert('已取消')
      fetchRefunds()
    }
  } catch (e) {
    alert(e.response?.data?.message || '操作失败')
  }
}

onMounted(fetchRefunds)
</script>

<style scoped>
.page-title {
  font-size: 28px;
  margin: 30px 0 20px;
  color: #333;
}

.empty {
  text-align: center;
  padding: 60px;
  color: #999;
}

.refund-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.refund-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.refund-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.refund-no {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.refund-date {
  font-size: 12px;
  color: #999;
}

.refund-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-0 { background: #fff3cd; color: #856404; }
.status-1 { background: #d1ecf1; color: #0c5460; }
.status-2 { background: #f8d7da; color: #721c24; }
.status-3 { background: #d1ecf1; color: #0c5460; }
.status-4 { background: #d4edda; color: #155724; }

.refund-body {
  margin-bottom: 16px;
}

.refund-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
}

.refund-row .label {
  color: #999;
  min-width: 80px;
}

.refund-amount {
  color: #e63946;
  font-weight: bold;
  font-size: 16px;
}

.admin-remark {
  color: #e67e22;
  font-style: italic;
}

.refund-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.btn-small {
  padding: 6px 16px;
  font-size: 13px;
}
</style>
