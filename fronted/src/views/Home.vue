<template>
  <div class="home">
    <!-- Banner -->
    <section class="banner">
      <div class="container">
        <div class="banner-content">
          <h1 class="banner-title">星云商城</h1>
          <p class="banner-subtitle">Nebula Stationery - 精选文具，品质生活</p>
          <router-link to="/category/1" class="btn btn-primary btn-large">立即选购</router-link>
        </div>
      </div>
    </section>

    <!-- 分类入口 -->
    <section class="categories">
      <div class="container">
        <h2 class="section-title">商品分类</h2>
        <div class="category-grid">
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="`/category/${cat.id}`"
            class="category-card"
          >
            <img v-if="cat.imageUrl" :src="cat.imageUrl" :alt="cat.name" class="category-image" />
            <div v-else class="category-icon">{{ getCategoryIcon(cat.id) }}</div>
            <h3>{{ cat.name }}</h3>
            <p>{{ cat.children?.length || 0 }} 个子分类</p>
          </router-link>
        </div>
      </div>
    </section>

    <!-- 推荐商品 -->
    <section class="products">
      <div class="container">
        <div class="section-head">
          <h2 class="section-title">推荐商品</h2>
          <button class="shuffle-btn" :disabled="loadingProducts" @click="shuffleProducts">
            <span class="shuffle-icon" :class="{ spinning: loadingProducts }">🔄</span>换一换
          </button>
        </div>
        <div class="product-grid">
          <div v-for="(product, index) in products" :key="`${product.id}-${index}`" class="product-card">
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const categories = ref([])
const products = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const loadingProducts = ref(false)
const PAGE_SIZE = 8

const getCategoryIcon = (id) => {
  const icons = { 1: '✏️', 2: '📝', 3: '📁', 4: '🎨' }
  return icons[id] || ''
}

/** 换一换永远占满 8 格：本页不足时先补上一批里没出现过的，还差就循环重复尾部的几个 */
const fillToPageSize = (records, previous) => {
  const filled = [...records]
  const seen = new Set(filled.map((p) => p.id))

  for (const p of previous) {
    if (filled.length >= PAGE_SIZE) break
    if (!seen.has(p.id)) {
      filled.push(p)
      seen.add(p.id)
    }
  }
  for (let i = 0; filled.length < PAGE_SIZE && i < filled.length; i++) {
    filled.push(filled[i])
  }
  return filled
}

const fetchProducts = async (page) => {
  loadingProducts.value = true
  try {
    const res = await api.get('/api/products', { params: { page, size: PAGE_SIZE } })
    if (res.data.code === 200) {
      products.value = fillToPageSize(res.data.data.records || [], products.value)
      currentPage.value = res.data.data.current
      totalPages.value = Math.max(1, res.data.data.pages)
    }
  } catch (e) {
    console.error('获取商品失败', e)
  } finally {
    loadingProducts.value = false
  }
}

const shuffleProducts = () => {
  fetchProducts(currentPage.value >= totalPages.value ? 1 : currentPage.value + 1)
}

onMounted(async () => {
  // 获取分类
  try {
    const catRes = await api.get('/api/categories')
    if (catRes.data.code === 200) {
      categories.value = catRes.data.data
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }

  fetchProducts(1)
})
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.85) 0%, rgba(51, 51, 51, 0.85) 100%),
              url('/images/banners/banner-hero-bg.png') center/cover no-repeat;
  color: white;
  padding: 80px 0;
  text-align: center;
}

.banner-title {
  font-size: 48px;
  margin-bottom: 10px;
}

.banner-subtitle {
  font-size: 18px;
  margin-bottom: 30px;
  opacity: 0.9;
}

.btn-large {
  padding: 14px 32px;
  font-size: 16px;
  background: white;
  color: #000000;
}

.btn-large:hover {
  background: #f0f0f0;
}

.section-title {
  text-align: center;
  font-size: 28px;
  margin: 40px 0 30px;
  color: #333;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.shuffle-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 40px 0 30px;
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 999px;
  background: white;
  color: #555;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.shuffle-btn:hover:not(:disabled) {
  color: #e63946;
  border-color: #e63946;
}

.shuffle-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.shuffle-icon {
  display: inline-block;
}

.shuffle-icon.spinning {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.category-card {
  background: white;
  padding: 30px 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.category-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.category-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.category-image {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 10px;
}

.category-card h3 {
  font-size: 18px;
  margin-bottom: 5px;
  color: #333;
}

.category-card p {
  color: #666;
  font-size: 14px;
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

@media (max-width: 768px) {
  .category-grid,
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .banner-title {
    font-size: 32px;
  }
}
</style>
