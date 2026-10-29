<template>
  <div class="category-page">
    <div class="container">
      <h1 class="page-title">{{ categoryName }} - 商品列表</h1>

      <!-- 分类筛选 -->
      <div class="filter-bar">
        <button
          v-for="cat in categories"
          :key="cat.id"
          @click="filterByCategory(cat.id)"
          :class="['filter-btn', { active: selectedCategory === cat.id }]"
        >
          {{ cat.name }}
        </button>
      </div>

      <!-- 商品列表 -->
      <div class="product-grid">
        <div v-for="product in products" :key="product.id" class="product-card">
          <router-link :to="`/product/${product.id}`" class="product-link">
            <img :src="product.imageUrl" :alt="product.name" class="product-image" />
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-category">{{ product.categoryName }}</p>
              <div class="product-footer">
                <span class="product-price">¥{{ product.price.toFixed(2) }}</span>
                <span class="product-stock">库存：{{ product.stock }}</span>
              </div>
            </div>
          </router-link>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > size">
        <button @click="changePage(page - 1)" :disabled="page <= 1">上一页</button>
        <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
        <button @click="changePage(page + 1)" :disabled="page >= Math.ceil(total / size)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()

const categories = ref([])
const products = ref([])
const categoryName = ref('')
const selectedCategory = ref(null)
const page = ref(1)
const size = ref(12)
const total = ref(0)

const fetchCategories = async () => {
  try {
    const res = await api.get('/api/categories')
    if (res.data.code === 200) {
      categories.value = res.data.data
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const fetchProducts = async () => {
  try {
    const params = { page: page.value, size: size.value }
    if (selectedCategory.value) {
      params.categoryId = selectedCategory.value
    }
    const res = await api.get('/api/products', { params })
    if (res.data.code === 200) {
      products.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (e) {
    console.error('获取商品失败', e)
  }
}

const filterByCategory = (catId) => {
  selectedCategory.value = catId
  page.value = 1
  fetchProducts()
}

const changePage = (newPage) => {
  page.value = newPage
  fetchProducts()
  window.scrollTo(0, 0)
}

onMounted(async () => {
  await fetchCategories()
  const catId = route.params.id
  if (catId) {
    selectedCategory.value = parseInt(catId)
    const cat = categories.value.find(c => c.id === selectedCategory.value)
    if (cat) categoryName.value = cat.name
  }
  await fetchProducts()
})

watch(() => route.params.id, async (newId) => {
  if (newId) {
    selectedCategory.value = parseInt(newId)
    const cat = categories.value.find(c => c.id === selectedCategory.value)
    if (cat) categoryName.value = cat.name
    page.value = 1
    await fetchProducts()
  }
})
</script>

<style scoped>
.page-title {
  font-size: 28px;
  margin: 30px 0 20px;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
  flex-wrap: wrap;
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

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.product-card:hover {
  transform: translateY(-5px);
}

.product-link {
  display: block;
  color: inherit;
}

.product-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.product-category {
  font-size: 13px;
  color: #999;
  margin-bottom: 10px;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 18px;
  font-weight: bold;
  color: #e63946;
}

.product-stock {
  font-size: 12px;
  color: #999;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin: 40px 0;
}

.pagination button {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
