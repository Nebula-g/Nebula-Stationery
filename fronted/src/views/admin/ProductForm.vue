<template>
  <div class="admin-product-form">
    <h1 class="page-title">{{ isEdit ? '编辑商品' : '新增商品' }}</h1>

    <form @submit.prevent="handleSubmit" class="form-card">
      <div class="form-group">
        <label>商品名称 *</label>
        <input v-model="form.name" type="text" class="input" placeholder="请输入商品名称" required />
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>价格 *</label>
          <input v-model.number="form.price" type="number" step="0.01" min="0" class="input" placeholder="请输入价格" required />
        </div>
        <div class="form-group">
          <label>库存 *</label>
          <input v-model.number="form.stock" type="number" min="0" class="input" placeholder="请输入库存" required />
        </div>
      </div>

      <div class="form-group">
        <label>所属分类 *</label>
        <select v-model="form.categoryId" class="input" required>
          <option value="">请选择分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.name }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>商品描述</label>
        <textarea v-model="form.description" class="input" rows="4" placeholder="请输入商品描述"></textarea>
      </div>

      <div class="form-group">
        <label>图片地址</label>
        <input v-model="form.imageUrl" type="text" class="input" placeholder="请输入图片 URL" />
        <img v-if="form.imageUrl" :src="form.imageUrl" class="image-preview" />
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? '保存中...' : '保存' }}
        </button>
        <router-link to="/admin/products" class="btn btn-secondary">取消</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)

const form = ref({
  name: '',
  price: 0,
  stock: 0,
  categoryId: '',
  description: '',
  imageUrl: ''
})

const categories = ref([])
const loading = ref(false)

const fetchCategories = async () => {
  try {
    const res = await api.get('/api/categories')
    if (res.data.code === 200) {
      // 扁平化分类树
      const flat = []
      const flatten = (cats) => {
        cats.forEach(cat => {
          if (cat.children && cat.children.length > 0) {
            flatten(cat.children)
          } else {
            flat.push(cat)
          }
        })
      }
      flatten(res.data.data)
      categories.value = flat
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const fetchProduct = async () => {
  if (!isEdit.value) return
  try {
    const res = await api.get(`/api/products/${route.params.id}`)
    if (res.data.code === 200) {
      const p = res.data.data
      form.value = {
        name: p.name,
        price: p.price,
        stock: p.stock,
        categoryId: p.categoryId,
        description: p.description || '',
        imageUrl: p.imageUrl || ''
      }
    }
  } catch (e) {
    console.error('获取商品失败', e)
  }
}

const handleSubmit = async () => {
  loading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await api.put(`/api/admin/products/${route.params.id}`, form.value)
    } else {
      res = await api.post('/api/admin/products', form.value)
    }

    if (res.data.code === 200) {
      alert(isEdit.value ? '编辑成功' : '新增成功')
      router.push('/admin/products')
    }
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchCategories()
  await fetchProduct()
})
</script>

<style scoped>
.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.form-card {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
}

.input:focus {
  border-color: #000000;
  outline: none;
}

textarea.input {
  resize: vertical;
}

.image-preview {
  max-width: 200px;
  max-height: 200px;
  margin-top: 10px;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 30px;
}
</style>
