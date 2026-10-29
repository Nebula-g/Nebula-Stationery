<template>
  <div class="admin-products">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <router-link to="/admin/products/create" class="btn btn-primary">新增商品</router-link>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <button @click="filterByStatus(null)" :class="['filter-btn', { active: selectedStatus === null }]">全部</button>
      <button @click="filterByStatus(1)" :class="['filter-btn', { active: selectedStatus === 1 }]">上架</button>
      <button @click="filterByStatus(0)" :class="['filter-btn', { active: selectedStatus === 0 }]">下架</button>
    </div>

    <!-- 商品列表 -->
    <div v-if="products.length === 0" class="empty">
      <p>暂无商品</p>
    </div>

    <div class="product-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>商品名称</th>
            <th>价格</th>
            <th>库存</th>
            <th>分类</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>{{ product.id }}</td>
            <td>{{ product.name }}</td>
            <td>¥{{ product.price.toFixed(2) }}</td>
            <td>{{ product.stock }}</td>
            <td>{{ product.categoryName }}</td>
            <td>
              <span class="status-badge" :class="product.status === 1 ? 'status-on' : 'status-off'">
                {{ product.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td class="actions">
              <router-link :to="`/admin/products/${product.id}/edit`" class="btn btn-secondary btn-small">编辑</router-link>
              <button @click="toggleStatus(product)" class="btn" :class="product.status === 1 ? 'btn-danger' : 'btn-primary'" :style="{ padding: '6px 12px', fontSize: '12px' }">
                {{ product.status === 1 ? '下架' : '上架' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

const products = ref([])
const selectedStatus = ref(null)

const fetchProducts = async () => {
  try {
    const params = { page: 1, size: 100 }
    if (selectedStatus.value !== null) {
      params.status = selectedStatus.value
    }
    const res = await api.get('/api/admin/products', { params })
    if (res.data.code === 200) {
      products.value = res.data.data.records
    }
  } catch (e) {
    console.error('获取商品失败', e)
  }
}

const filterByStatus = (status) => {
  selectedStatus.value = status
  fetchProducts()
}

const toggleStatus = async (product) => {
  const newStatus = product.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  if (!confirm(`确定${action}该商品吗？`)) return

  try {
    const res = await api.put(`/api/admin/products/${product.id}/status`, null, { params: { status: newStatus } })
    if (res.data.code === 200) {
      alert(`${action}成功`)
      fetchProducts()
    }
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(fetchProducts)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filter-btn {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn:hover,
.filter-btn.active {
  background: #000000;
  color: white;
  border-color: #000000;
}

.empty {
  text-align: center;
  padding: 60px;
  color: #999;
}

.product-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  background: #f8f9fa;
  font-weight: bold;
  color: #333;
}

td {
  color: #666;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-on {
  background: #d4edda;
  color: #155724;
}

.status-off {
  background: #f8d7da;
  color: #721c24;
}

.actions {
  display: flex;
  gap: 10px;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}
</style>
