<template>
  <div class="product-detail" v-if="product">
    <div class="container">
      <div class="product-layout">
        <div class="product-image-section">
          <img :src="currentImage" :alt="product.name" class="product-image" />
        </div>

        <div class="product-info-section">
          <h1 class="product-name">{{ product.name }}</h1>
          <p class="product-category">分类：{{ product.categoryName }}</p>
          <div class="product-price-section">
            <span class="product-price">¥{{ currentPrice.toFixed(2) }}</span>
            <span class="product-stock">库存：{{ currentStock }}</span>
          </div>

          <div v-if="product.hasSpec && product.specs" class="spec-section">
            <div v-for="spec in product.specs" :key="spec.id" class="spec-group">
              <span class="spec-label">{{ spec.specName }}：</span>
              <div class="spec-values">
                <button
                  v-for="val in spec.specValues"
                  :key="val"
                  @click="selectSpec(spec.specName, val)"
                  class="spec-btn"
                  :class="{ active: selectedSpecs[spec.specName] === val, disabled: !isSpecAvailable(spec.specName, val) }"
                >{{ val }}</button>
              </div>
            </div>
          </div>

          <p class="product-description">{{ product.description }}</p>

          <div class="cart-section">
            <div class="quantity-selector">
              <button @click="decreaseQuantity">-</button>
              <input type="number" v-model="quantity" min="1" :max="currentStock" />
              <button @click="increaseQuantity">+</button>
            </div>
            <button @click="addToCart" class="btn btn-primary btn-large" :disabled="!canAddToCart">
              {{ addBtnText }}
            </button>
          </div>
        </div>
      </div>

      <div class="reviews-section">
        <h2 class="section-title">商品评价 <span v-if="reviews.length" class="review-count">{{ reviews.length }} 条</span></h2>
        <ReviewList :reviews="reviews" :is-logged-in="userStore.isLoggedIn" />
      </div>
    </div>
  </div>
  <div v-else class="loading">
    <p>加载中...</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import ReviewList from '../components/ReviewList.vue'
import api from '../api'
import toast from '../utils/toast'

const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref(null)
const reviews = ref([])
const quantity = ref(1)
const selectedSpecs = ref({})

const selectedSku = computed(() => {
  if (!product.value?.hasSpec || !product.value?.skus) return null
  const specs = product.value.specs
  if (Object.keys(selectedSpecs.value).length !== specs.length) return null
  return product.value.skus.find(sku => {
    const skuSpecs = JSON.parse(sku.specValues)
    return specs.every(spec => skuSpecs[spec.specName] === selectedSpecs.value[spec.specName])
  }) || null
})

const currentPrice = computed(() => {
  if (selectedSku.value) return selectedSku.value.price
  return product.value?.price || 0
})

const currentStock = computed(() => {
  if (selectedSku.value) return selectedSku.value.stock
  return product.value?.stock || 0
})

const currentImage = computed(() => {
  if (selectedSku.value?.imageUrl) return selectedSku.value.imageUrl
  return product.value?.imageUrl || ''
})

const canAddToCart = computed(() => {
  if (currentStock.value <= 0) return false
  if (product.value?.hasSpec && !selectedSku.value) return false
  return true
})

const addBtnText = computed(() => {
  if (product.value?.hasSpec && !selectedSku.value) return '请选择规格'
  if (currentStock.value <= 0) return '缺货'
  return '加入购物车'
})

const selectSpec = (specName, value) => {
  if (selectedSpecs.value[specName] === value) {
    delete selectedSpecs.value[specName]
  } else {
    selectedSpecs.value[specName] = value
  }
  selectedSpecs.value = { ...selectedSpecs.value }
  quantity.value = 1
}

const isSpecAvailable = (specName, value) => {
  if (!product.value?.skus) return true
  return product.value.skus.some(sku => {
    if (sku.status !== 1) return false
    const skuSpecs = JSON.parse(sku.specValues)
    return skuSpecs[specName] === value && sku.stock > 0
  })
}

const fetchProduct = async () => {
  try {
    const res = await api.get(`/api/products/${route.params.id}`)
    if (res.data.code === 200) {
      product.value = res.data.data
    }
  } catch (e) {
    console.error('获取商品失败', e)
  }
}

const fetchReviews = async () => {
  try {
    const res = await api.get(`/api/reviews/product/${route.params.id}`)
    if (res.data.code === 200) {
      reviews.value = res.data.data
    }
  } catch (e) {
    console.error('获取评价失败', e)
  }
}

const increaseQuantity = () => {
  if (quantity.value < currentStock.value) {
    quantity.value++
  }
}

const decreaseQuantity = () => {
  if (quantity.value > 1) {
    quantity.value--
  }
}

const addToCart = async () => {
  if (!userStore.isLoggedIn) {
    toast.error('请先登录')
    return
  }
  if (!canAddToCart.value) return
  try {
    const skuId = selectedSku.value ? selectedSku.value.id : null
    await cartStore.addToCart(product.value.id, quantity.value, skuId)
    toast.success('已加入购物车')
  } catch (e) {
    toast.error(e.message || '添加失败')
  }
}

onMounted(async () => {
  await fetchProduct()
  await fetchReviews()
})
</script>

<style scoped>
.product-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin: 30px 0;
}

.product-image {
  width: 100%;
  border-radius: 8px;
}

.product-name {
  font-size: 28px;
  margin-bottom: 10px;
  color: #333;
}

.product-category {
  color: #666;
  margin-bottom: 20px;
}

.product-price-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.product-price {
  font-size: 32px;
  font-weight: bold;
  color: #e63946;
}

.product-stock {
  color: #666;
}

.spec-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.spec-group {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
}

.spec-group:last-child {
  margin-bottom: 0;
}

.spec-label {
  font-size: 14px;
  color: #666;
  min-width: 60px;
  line-height: 32px;
}

.spec-values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spec-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.spec-btn:hover:not(.disabled) {
  border-color: #000;
}

.spec-btn.active {
  background: #000;
  color: white;
  border-color: #000;
}

.spec-btn.disabled {
  color: #ccc;
  cursor: not-allowed;
  text-decoration: line-through;
}

.product-description {
  line-height: 1.6;
  color: #666;
  margin-bottom: 30px;
}

.cart-section {
  display: flex;
  gap: 20px;
  align-items: center;
}

.quantity-selector {
  display: flex;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.quantity-selector button {
  width: 40px;
  height: 40px;
  background: white;
  border: none;
  cursor: pointer;
  font-size: 18px;
}

.quantity-selector input {
  width: 60px;
  height: 40px;
  border: none;
  border-left: 1px solid #ddd;
  border-right: 1px solid #ddd;
  text-align: center;
  font-size: 16px;
}

.btn-large {
  padding: 12px 32px;
  font-size: 16px;
}

.section-title {
  font-size: 24px;
  margin: 40px 0 20px;
  color: #333;
}

.review-count {
  font-size: 14px;
  font-weight: normal;
  color: #999;
}

.loading {
  text-align: center;
  padding: 60px;
  color: #999;
}

@media (max-width: 768px) {
  .product-layout {
    grid-template-columns: 1fr;
  }
}
</style>
