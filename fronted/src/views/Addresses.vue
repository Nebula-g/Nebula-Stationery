<template>
  <div class="addresses-page">
    <div class="container">
      <h1 class="page-title">收货地址管理</h1>

      <button @click="openAddForm" class="btn btn-primary add-btn">+ 新增地址</button>

      <div v-if="addresses.length === 0" class="empty">
        <p>暂无收货地址，请添加</p>
      </div>

      <div v-for="addr in addresses" :key="addr.id" class="address-card" :class="{ 'is-default': addr.isDefault === 1 }">
        <div class="address-info">
          <div class="address-top">
            <span class="receiver-name">{{ addr.receiverName }}</span>
            <span class="receiver-phone">{{ addr.receiverPhone }}</span>
            <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
          </div>
          <div class="address-detail">{{ addr.fullAddress }}</div>
        </div>
        <div class="address-actions">
          <button v-if="addr.isDefault !== 1" @click="setDefaultDefault(addr.id)" class="btn-link">设为默认</button>
          <button @click="openEditForm(addr)" class="btn-link">编辑</button>
          <button @click="deleteAddress(addr.id)" class="btn-link danger">删除</button>
        </div>
      </div>

      <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
        <div class="modal">
          <h2 class="modal-title">{{ editingId ? '编辑地址' : '新增地址' }}</h2>
          <form @submit.prevent="saveAddress">
            <div class="form-group">
              <label>收货人姓名</label>
              <input v-model="form.receiverName" placeholder="请输入姓名" required />
            </div>
            <div class="form-group">
              <label>手机号码</label>
              <input v-model="form.receiverPhone" placeholder="请输入手机号" required />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>省份</label>
                <input v-model="form.province" placeholder="省" required />
              </div>
              <div class="form-group">
                <label>城市</label>
                <input v-model="form.city" placeholder="市" required />
              </div>
              <div class="form-group">
                <label>区/县</label>
                <input v-model="form.district" placeholder="区" required />
              </div>
            </div>
            <div class="form-group">
              <label>详细地址</label>
              <input v-model="form.detailAddress" placeholder="街道、楼号等" required />
            </div>
            <div class="form-group checkbox-group">
              <label><input type="checkbox" v-model="form.isDefault" :true-value="1" :false-value="0" /> 设为默认地址</label>
            </div>
            <div class="form-actions">
              <button type="button" @click="showForm = false" class="btn btn-secondary">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const addresses = ref([])
const showForm = ref(false)
const editingId = ref(null)
const form = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const fetchAddresses = async () => {
  try {
    const res = await api.get('/api/addresses')
    if (res.data.code === 200) {
      addresses.value = res.data.data
    }
  } catch (e) {
    console.error('获取地址失败', e)
  }
}

const openAddForm = () => {
  editingId.value = null
  form.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 }
  showForm.value = true
}

const openEditForm = (addr) => {
  editingId.value = addr.id
  form.value = {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    province: addr.province,
    city: addr.city,
    district: addr.district,
    detailAddress: addr.detailAddress,
    isDefault: addr.isDefault
  }
  showForm.value = true
}

const saveAddress = async () => {
  try {
    if (editingId.value) {
      const res = await api.put(`/api/addresses/${editingId.value}`, form.value)
      if (res.data.code === 200) {
        alert('修改成功')
      }
    } else {
      const res = await api.post('/api/addresses', form.value)
      if (res.data.code === 200) {
        alert('添加成功')
      }
    }
    showForm.value = false
    fetchAddresses()
  } catch (e) {
    alert(e.response?.data?.message || '操作失败')
  }
}

const deleteAddress = async (id) => {
  if (!confirm('确定删除该地址吗？')) return
  try {
    const res = await api.delete(`/api/addresses/${id}`)
    if (res.data.code === 200) {
      alert('删除成功')
      fetchAddresses()
    }
  } catch (e) {
    alert(e.response?.data?.message || '删除失败')
  }
}

const setDefaultDefault = async (id) => {
  try {
    const res = await api.put(`/api/addresses/${id}/default`)
    if (res.data.code === 200) {
      fetchAddresses()
    }
  } catch (e) {
    alert(e.response?.data?.message || '操作失败')
  }
}

onMounted(fetchAddresses)
</script>

<style scoped>
.page-title {
  font-size: 28px;
  margin: 30px 0 20px;
  color: #333;
}

.add-btn {
  margin-bottom: 20px;
}

.empty {
  text-align: center;
  padding: 60px;
  color: #999;
}

.address-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 2px solid transparent;
}

.address-card.is-default {
  border-color: #000;
}

.address-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.receiver-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.receiver-phone {
  font-size: 14px;
  color: #666;
}

.default-tag {
  background: #000;
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.address-detail {
  font-size: 14px;
  color: #666;
}

.address-actions {
  display: flex;
  gap: 12px;
}

.btn-link {
  background: none;
  border: none;
  color: #000;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-link:hover {
  background: #f0f0f0;
}

.btn-link.danger {
  color: #e63946;
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
  width: 500px;
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

.form-group input[type="text"],
.form-group input[type="tel"] {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
</style>
