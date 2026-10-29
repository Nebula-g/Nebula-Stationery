<template>
  <div class="search-page">
    <div class="container">
      <h1 class="page-title">
        搜索「{{ keyword }}」
        <span class="result-count" v-if="loaded">共 {{ total }} 个结果</span>
      </h1>

      <div v-if="loaded && products.length === 0" class="empty">
        <p>没有找到相关商品，换个关键词试试～</p>
        <div v-if="hotWords.length" class="hot-row">
          <span class="hot-label">热搜：</span>
          <router-link
            v-for="word in hotWords"
            :key="word"
            :to="{ path: '/search', query: { keyword: word } }"
            class="hot-link"
          >{{ word }}</router-link>
        </div>
      </div>

      <div v-else class="product-grid">
        <div v-for="product in products" :key="product.id" class="product-card">
          <router-link :to="`/product/${product.id}`" class="product-link">
            <img :src="product.imageUrl" :alt="product.name" class="product-image" />
            <div class="product-info">
              <h3 class="product-name" v-html="highlight(product.name)"></h3>
              <p class="product-category">{{ product.categoryName }}</p>
              <div class="product-footer">
                <span class="product-price">¥{{ product.price.toFixed(2) }}</span>
                <span class="product-stock">库存：{{ product.stock }}</span>
              </div>
            </div>
          </router-link>
        </div>
      </div>

      <div class="pagination" v-if="total > size">
        <button @click="changePage(page - 1)" :disabled="page <= 1">上一页</button>
        <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
        <button @click="changePage(page + 1)" :disabled="page >= Math.ceil(total / size)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()

const products = ref([])
const hotWords = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loaded = ref(false)

const keyword = ref('')

const escapeHtml = (text) =>
  String(text).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))

const highlight = (name) => {
  const kw = keyword.value
  if (!kw) return escapeHtml(name)
  const idx = name.indexOf(kw)
  if (idx === -1) return escapeHtml(name)
  return (
    escapeHtml(name.slice(0, idx)) +
    '<em class="kw">' + escapeHtml(name.slice(idx, idx + kw.length)) + '</em>' +
    escapeHtml(name.slice(idx + kw.length))
  )
}

const fetchProducts = async () => {
  loaded.value = false
  try {
    const res = await api.get('/api/products', {
      params: { keyword: keyword.value, page: page.value, size: size.value }
    })
    if (res.data.code === 200) {
      products.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (e) {
    console.error('搜索失败', e)
  } finally {
    loaded.value = true
  }
}

const fetchHotWords = async () => {
  try {
    const res = await api.get('/api/products/hot-keywords', { params: { limit: 6 } })
    if (res.data.code === 200) hotWords.value = res.data.data
  } catch (e) {
    hotWords.value = []
  }
}

const changePage = (newPage) => {
  page.value = newPage
  fetchProducts()
  window.scrollTo(0, 0)
}

const syncKeyword = () => {
  keyword.value = String(route.query.keyword || '').trim()
  page.value = 1
  if (keyword.value) fetchProducts()
  else products.value = []
}

onMounted(() => {
  fetchHotWords()
  syncKeyword()
})

watch(() => route.query.keyword, syncKeyword)
</script>

<style scoped>
.page-title {
  font-size: 24px;
  margin: 30px 0 20px;
  color: #333;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.result-count {
  font-size: 13px;
  font-weight: normal;
  color: #999;
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

.product-name :deep(.kw) {
  font-style: normal;
  color: #e63946;
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

.empty {
  background: white;
  border-radius: 8px;
  padding: 60px 20px;
  text-align: center;
  color: #666;
}

.hot-row {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hot-label {
  font-size: 13px;
  color: #999;
}

.hot-link {
  font-size: 13px;
  color: #333;
  border: 1px solid #e5e5e5;
  border-radius: 14px;
  padding: 4px 12px;
}

.hot-link:hover {
  border-color: #000;
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
