<template>
  <div v-if="modelValue" class="ship-overlay" @click.self="close">
    <div class="ship-modal">
      <h2 class="ship-title">订单发货</h2>
      <p class="ship-sub">订单号 {{ order?.orderNo }}</p>

      <form @submit.prevent="submit">
        <div class="ship-field">
          <label>快递公司</label>
          <select v-model="companyCode">
            <option v-for="c in companies" :key="c.code" :value="c.code">{{ c.name }}</option>
          </select>
        </div>
        <div class="ship-field">
          <label>运单号</label>
          <input v-model="trackingNo" placeholder="留空自动生成演示运单号" maxlength="50" />
        </div>
        <p class="ship-hint">模拟通道会按发货时间自动生成运输轨迹；接上快递100 后显示真实轨迹。</p>

        <div class="ship-actions">
          <button type="button" class="btn-cancel" @click="close">取消</button>
          <button type="submit" class="btn-confirm" :disabled="submitting">
            {{ submitting ? '提交中…' : '确认发货' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../api'
import toast from '../utils/toast'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'shipped'])

const FALLBACK = [
  { code: 'SF', name: '顺丰速运' },
  { code: 'YTO', name: '圆通速递' },
  { code: 'ZTO', name: '中通快递' },
  { code: 'YUNDA', name: '韵达快递' },
  { code: 'STO', name: '申通快递' },
  { code: 'JD', name: '京东物流' }
]

const companies = ref(FALLBACK)
const companyCode = ref('SF')
const trackingNo = ref('')
const submitting = ref(false)
const providerName = computed(() => (companies.value.length ? '模拟 / 快递100 可切换' : '模拟'))

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    trackingNo.value = ''
    companyCode.value = 'SF'
    try {
      const res = await api.get('/api/admin/logistics/companies')
      if (res.data.code === 200 && Array.isArray(res.data.data)) {
        companies.value = res.data.data.map((item) => {
          const [code, name] = item.split(':')
          return { code, name }
        })
      }
    } catch {
      companies.value = FALLBACK
    }
  }
)

const close = () => emit('update:modelValue', false)

const submit = async () => {
  submitting.value = true
  try {
    const res = await api.put(`/api/admin/orders/${props.order.id}/ship`, {
      companyCode: companyCode.value,
      trackingNo: trackingNo.value.trim()
    })
    if (res.data.code === 200) {
      toast.success('发货成功，物流单已生成')
      emit('shipped', res.data.data)
      close()
    }
  } catch (e) {
    toast.error(e.message || '发货失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.ship-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.ship-modal {
  background: white;
  border-radius: 10px;
  padding: 24px;
  width: 92vw;
  max-width: 420px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.ship-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 4px;
}

.ship-sub {
  font-size: 13px;
  color: #999;
  margin-bottom: 18px;
}

.ship-field {
  margin-bottom: 16px;
}

.ship-field label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.ship-field select,
.ship-field input {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 9px 10px;
  font-size: 14px;
  font-family: inherit;
  background: white;
}

.ship-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.6;
  margin-bottom: 18px;
}

.ship-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel,
.btn-confirm {
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #ddd;
  background: white;
  color: #555;
}

.btn-confirm {
  background: #2a9d8f;
  border-color: #2a9d8f;
  color: white;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
