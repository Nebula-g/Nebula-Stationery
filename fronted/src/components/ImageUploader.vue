<template>
  <div class="image-uploader">
    <div class="thumb-row">
      <div v-for="(url, idx) in modelValue" :key="url" class="thumb">
        <img :src="url" alt="评价图片" @click="preview(url)" />
        <button type="button" class="remove-btn" @click="remove(idx)">✕</button>
      </div>
      <label v-if="modelValue.length < max" class="add-box">
        <input type="file" accept="image/*" multiple hidden @change="onChange" />
        <span class="add-icon">＋</span>
        <span class="add-text">{{ modelValue.length }}/{{ max }}</span>
      </label>
    </div>
    <p v-if="uploading" class="tip">上传中…</p>
    <p v-else-if="error" class="tip error">{{ error }}</p>
    <p v-else class="tip">支持 jpg/png/gif/webp，单张不超过 5MB</p>

    <div v-if="previewUrl" class="preview-mask" @click="previewUrl = ''">
      <img :src="previewUrl" alt="预览" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 6 }
})
const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const error = ref('')
const previewUrl = ref('')

const preview = (url) => {
  previewUrl.value = url
}

const remove = (idx) => {
  const next = [...props.modelValue]
  next.splice(idx, 1)
  emit('update:modelValue', next)
}

const uploadOne = async (file) => {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post('/api/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  if (res.data.code !== 200) throw new Error(res.data.message || '上传失败')
  return res.data.data
}

const onChange = async (event) => {
  error.value = ''
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return

  const room = props.max - props.modelValue.length
  if (files.length > room) error.value = `最多还能上传 ${room} 张`

  uploading.value = true
  const uploaded = []
  for (const file of files.slice(0, room)) {
    try {
      uploaded.push(await uploadOne(file))
    } catch (e) {
      error.value = e.response?.data?.message || `${file.name} 上传失败`
    }
  }
  uploading.value = false
  if (uploaded.length) emit('update:modelValue', [...props.modelValue, ...uploaded])
}
</script>

<style scoped>
.thumb-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
}

.thumb,
.add-box {
  width: 76px;
  height: 76px;
  border-radius: 6px;
  position: relative;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
  cursor: zoom-in;
  display: block;
}

.remove-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: #e63946;
  color: white;
  font-size: 11px;
  cursor: pointer;
  line-height: 1;
}

.add-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed #ccc;
  background: #fafafa;
  cursor: pointer;
  color: #999;
}

.add-box:hover {
  border-color: #000;
  color: #000;
}

.add-icon {
  font-size: 20px;
  line-height: 1;
}

.add-text {
  font-size: 11px;
  margin-top: 4px;
}

.tip {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.tip.error {
  color: #e63946;
}

.preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.preview-mask img {
  max-width: 88vw;
  max-height: 88vh;
  border-radius: 6px;
}
</style>
