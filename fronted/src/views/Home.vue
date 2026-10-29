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
        <h2 class="section-title">推荐商品</h2>
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const categories = ref([])
const products = ref([])

const getCategoryIcon = (id) => {
  const icons = { 1: '✏️', 2: '📝', 3: '📁', 4: '🎨' }
  return icons[id] || ''
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

  // 获取推荐商品
  try {
    const prodRes = await api.get('/api/products', { params: { page: 1, size: 8 } })
    if (prodRes.data.code === 200) {
      products.value = prodRes.data.data.records
    }
  } catch (e) {
    console.error('获取商品失败', e)
  }
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
