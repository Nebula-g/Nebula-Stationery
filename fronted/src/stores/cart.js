import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  const totalAmount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.subtotal, 0)
  })

  const fetchCart = async () => {
    loading.value = true
    try {
      const res = await api.get('/api/cart')
      if (res.data.code === 200) {
        items.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  const addToCart = async (productId, quantity, skuId = null) => {
    const body = { productId, quantity }
    if (skuId) body.skuId = skuId
    const res = await api.post('/api/cart', body)
    if (res.data.code === 200) {
      await fetchCart()
      return true
    }
    throw new Error(res.data.message)
  }

  const updateQuantity = async (itemId, quantity) => {
    const res = await api.put(`/api/cart/${itemId}`, null, { params: { quantity } })
    if (res.data.code === 200) {
      await fetchCart()
      return true
    }
    throw new Error(res.data.message)
  }

  const deleteItem = async (itemId) => {
    const res = await api.delete(`/api/cart/${itemId}`)
    if (res.data.code === 200) {
      await fetchCart()
      return true
    }
    throw new Error(res.data.message)
  }

  const clearCart = async () => {
    const res = await api.delete('/api/cart/clear')
    if (res.data.code === 200) {
      items.value = []
      return true
    }
    throw new Error(res.data.message)
  }

  return {
    items,
    loading,
    totalAmount,
    fetchCart,
    addToCart,
    updateQuantity,
    deleteItem,
    clearCart
  }
})
