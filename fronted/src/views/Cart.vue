<template>
  <div class="cart-page">
    <div class="container">
      <h1 class="page-title">我的购物车</h1>

      <div v-if="cartStore.items.length === 0" class="empty-cart">
        <p>购物车是空的</p>
        <router-link to="/" class="btn btn-primary">去购物</router-link>
      </div>

      <div v-else class="cart-content">
        <div class="cart-items">
          <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
            <img :src="item.imageUrl" :alt="item.productName" class="item-image" />
            <div class="item-info">
              <h3 class="item-name">{{ item.productName }}</h3>
              <p v-if="item.specText" class="item-spec">规格：{{ item.specText }}</p>
              <p class="item-price">¥{{ item.price.toFixed(2) }}</p>
            </div>
            <div class="item-quantity">
              <button @click="updateQuantity(item.id, item.quantity - 1)" :disabled="item.quantity <= 1">-</button>
              <span>{{ item.quantity }}</span>
              <button @click="updateQuantity(item.id, item.quantity + 1)" :disabled="item.quantity >= 100">+</button>
            </div>
            <div class="item-subtotal">
              <span class="subtotal-price">¥{{ item.subtotal.toFixed(2) }}</span>
            </div>
            <button @click="deleteItem(item.id)" class="btn btn-danger btn-small">删除</button>
          </div>
        </div>

        <div class="cart-summary">
          <div class="summary-row">
            <span>合计：</span>
            <span class="total-amount">¥{{ cartStore.totalAmount.toFixed(2) }}</span>
          </div>
          <button @click="checkout" class="btn btn-primary btn-large">结算</button>
          <button @click="clearCart" class="btn btn-secondary">清空购物车</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cartStore = useCartStore()

onMounted(() => {
  cartStore.fetchCart()
})

const updateQuantity = async (itemId, quantity) => {
  if (quantity < 1) return
  try {
    await cartStore.updateQuantity(itemId, quantity)
  } catch (e) {
    alert(e.message || '更新失败')
  }
}

const deleteItem = async (itemId) => {
  if (!confirm('确定删除该商品吗？')) return
  try {
    await cartStore.deleteItem(itemId)
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

const clearCart = async () => {
  if (!confirm('确定清空购物车吗？')) return
  try {
    await cartStore.clearCart()
  } catch (e) {
    alert(e.message || '清空失败')
  }
}

const checkout = async () => {
  const cartItemIds = cartStore.items.map(item => item.id)
  try {
    const api = (await import('../api')).default
    const res = await api.post('/api/orders', { cartItemIds })
    if (res.data.code === 200) {
      alert('下单成功')
      router.push(`/order/${res.data.data.id}`)
    }
  } catch (e) {
    alert(e.message || '下单失败')
  }
}
</script>

<style scoped>
.page-title {
  font-size: 28px;
  margin: 30px 0 20px;
  color: #333;
}

.empty-cart {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-cart p {
  font-size: 18px;
  margin-bottom: 20px;
}

.cart-content {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 30px;
}

.cart-item {
  display: grid;
  grid-template-columns: 100px 1fr auto auto auto;
  gap: 20px;
  align-items: center;
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.item-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
}

.item-name {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.item-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.item-price {
  color: #e63946;
  font-weight: bold;
}

.item-quantity {
  display: flex;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.item-quantity button {
  width: 32px;
  height: 32px;
  background: white;
  border: none;
  cursor: pointer;
}

.item-quantity button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.item-quantity span {
  width: 50px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-left: 1px solid #ddd;
  border-right: 1px solid #ddd;
}

.subtotal-price {
  font-size: 18px;
  font-weight: bold;
  color: #e63946;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

.cart-summary {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: fit-content;
  position: sticky;
  top: 90px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 18px;
}

.total-amount {
  font-size: 24px;
  font-weight: bold;
  color: #e63946;
}

.btn-large {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .cart-content {
    grid-template-columns: 1fr;
  }

  .cart-item {
    grid-template-columns: 80px 1fr;
    gap: 10px;
  }
}
</style>
