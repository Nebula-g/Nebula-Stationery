<template>
  <div class="review-list">
    <div v-if="reviews.length === 0" class="empty-reviews">
      <p>暂无评价</p>
    </div>

    <div v-for="review in reviews" :key="review.id" class="review-card">
      <div class="review-header">
        <span class="review-user">{{ review.username }}</span>
        <span class="review-rating">
          <span v-for="i in 5" :key="i" class="star" :class="{ active: i <= review.rating }">★</span>
        </span>
      </div>

      <p class="review-content">{{ review.content }}</p>

      <div v-if="review.imageUrls?.length" class="review-images">
        <img
          v-for="url in review.imageUrls"
          :key="url"
          :src="url"
          alt="评价图片"
          @click="preview(url)"
        />
      </div>

      <div v-if="review.append" class="review-append">
        <span class="append-tag">追评</span>
        <span class="append-time">{{ review.append.createdAt }}</span>
        <p class="append-content">{{ review.append.content }}</p>
        <div v-if="review.append.imageUrls?.length" class="review-images">
          <img
            v-for="url in review.append.imageUrls"
            :key="'a-' + url"
            :src="url"
            alt="追评图片"
            @click="preview(url)"
          />
        </div>
      </div>

      <div class="review-footer">
        <span class="review-date">{{ review.createdAt }}</span>
        <button
          class="like-btn"
          :class="{ liked: review.liked }"
          :disabled="likingId === review.id"
          @click="toggleLike(review)"
        >
          <span class="like-icon">👍</span>
          <span>{{ review.likeCount || 0 }}</span>
        </button>
      </div>
    </div>

    <div v-if="previewUrl" class="preview-mask" @click="previewUrl = ''">
      <img :src="previewUrl" alt="预览" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import toast from '../utils/toast'

const props = defineProps({
  reviews: { type: Array, default: () => [] },
  isLoggedIn: { type: Boolean, default: false }
})

const router = useRouter()
const likingId = ref(null)
const previewUrl = ref('')

const preview = (url) => {
  previewUrl.value = url
}

const toggleLike = async (review) => {
  if (!props.isLoggedIn) {
    toast.info('请先登录后点赞')
    router.push('/login')
    return
  }
  likingId.value = review.id
  try {
    const res = await api.put(`/api/reviews/${review.id}/like`)
    if (res.data.code === 200) {
      review.liked = res.data.data.liked
      review.likeCount = Math.max(0, (review.likeCount || 0) + (review.liked ? 1 : -1))
    } else {
      toast.error(res.data.message || '点赞失败')
    }
  } catch (e) {
    console.error('点赞失败', e)
    toast.error(e.response?.data?.message || '点赞失败，请确认后端已重启')
  } finally {
    likingId.value = null
  }
}
</script>

<style scoped>
.review-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.review-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.review-user {
  font-weight: bold;
  color: #333;
}

.star {
  color: #ddd;
  font-size: 18px;
}

.star.active {
  color: #ffc107;
}

.review-content {
  color: #666;
  line-height: 1.6;
  margin: 0 0 10px;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.review-images img {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 6px;
  cursor: zoom-in;
  background: #f5f5f5;
}

.review-append {
  background: #f7f8fa;
  border-left: 3px solid #e63946;
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.append-tag {
  display: inline-block;
  background: #e63946;
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-right: 8px;
}

.append-time {
  font-size: 11px;
  color: #999;
}

.append-content {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  margin: 8px 0 0;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-date {
  font-size: 12px;
  color: #999;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid #e5e5e5;
  background: white;
  color: #666;
  border-radius: 14px;
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.like-btn:hover:not(:disabled) {
  border-color: #e63946;
  color: #e63946;
}

.like-btn.liked {
  background: #fdecee;
  border-color: #e63946;
  color: #e63946;
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.like-icon {
  font-size: 14px;
}

.empty-reviews {
  text-align: center;
  padding: 40px;
  color: #999;
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
