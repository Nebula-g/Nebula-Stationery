<template>
  <div class="search-box" ref="rootEl">
    <div class="search-input-wrap">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        placeholder="搜索文具，如：中性笔"
        @focus="openPanel"
        @input="onInput"
        @keydown.enter="goSearch"
        @keydown.down.prevent="moveHighlight(1)"
        @keydown.up.prevent="moveHighlight(-1)"
      />
      <button class="search-btn" @click="goSearch">搜索</button>
    </div>

    <div v-if="panelOpen" class="search-panel">
      <template v-if="suggestions.length">
        <div
          v-for="(item, idx) in suggestions"
          :key="item.id"
          class="suggest-item"
          :class="{ active: idx === highlightIndex }"
          @mouseenter="highlightIndex = idx"
          @mousedown.prevent="pickProduct(item)"
        >
          <img :src="item.imageUrl" :alt="item.name" class="suggest-thumb" />
          <div class="suggest-main">
            <span class="suggest-name" v-html="highlight(item.name)"></span>
            <span class="suggest-meta">已售 {{ item.soldCount || 0 }}</span>
          </div>
          <span class="suggest-price">¥{{ Number(item.price).toFixed(2) }}</span>
        </div>
      </template>

      <template v-else-if="keyword.trim()">
        <div class="suggest-empty">没有匹配的商品，回车查看全部结果</div>
      </template>

      <template v-else>
        <div v-if="history.length" class="suggest-group">
          <div class="suggest-group-head">
            <span>搜索历史</span>
            <button class="clear-btn" @mousedown.prevent="clearHistory">清空</button>
          </div>
          <div class="chip-row">
            <button
              v-for="word in history"
              :key="'h-' + word"
              class="chip"
              @mousedown.prevent="pickKeyword(word)"
            >{{ word }}</button>
          </div>
        </div>
        <div v-if="hotWords.length" class="suggest-group">
          <div class="suggest-group-head"><span>热门搜索</span></div>
          <div class="chip-row">
            <button
              v-for="(word, idx) in hotWords"
              :key="'t-' + word"
              class="chip hot"
              @mousedown.prevent="pickKeyword(word)"
            >
              <span class="hot-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>{{ word }}
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const HISTORY_KEY = 'search-history'
const MAX_HISTORY = 8

const router = useRouter()
const rootEl = ref(null)
const keyword = ref('')
const suggestions = ref([])
const hotWords = ref([])
const history = ref([])
const panelOpen = ref(false)
const highlightIndex = ref(-1)

let debounceTimer = null

const readHistory = () => {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []
  } catch (e) {
    return []
  }
}

const saveHistory = (list) => {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
  history.value = list
}

const openPanel = () => {
  panelOpen.value = true
  if (!hotWords.value.length) loadHotWords()
}

const loadHotWords = async () => {
  try {
    const res = await api.get('/api/products/hot-keywords', { params: { limit: 8 } })
    if (res.data.code === 200) hotWords.value = res.data.data
  } catch (e) {
    hotWords.value = []
  }
}

const fetchSuggestions = async () => {
  const kw = keyword.value.trim()
  if (!kw) {
    suggestions.value = []
    return
  }
  try {
    const res = await api.get('/api/products/suggest', { params: { keyword: kw } })
    if (res.data.code === 200) suggestions.value = res.data.data
  } catch (e) {
    suggestions.value = []
  }
}

const onInput = () => {
  highlightIndex.value = -1
  panelOpen.value = true
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchSuggestions, 250)
}

const moveHighlight = (step) => {
  if (!suggestions.value.length) return
  const next = highlightIndex.value + step
  highlightIndex.value = (next + suggestions.value.length) % suggestions.value.length
}

const escapeHtml = (text) =>
  text.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))

const highlight = (name) => {
  const kw = keyword.value.trim()
  const safe = escapeHtml(name)
  if (!kw) return safe
  const idx = name.indexOf(kw)
  if (idx === -1) return safe
  return (
    escapeHtml(name.slice(0, idx)) +
    '<em class="kw">' + escapeHtml(name.slice(idx, idx + kw.length)) + '</em>' +
    escapeHtml(name.slice(idx + kw.length))
  )
}

const remember = (word) => {
  const list = [word, ...readHistory().filter((w) => w !== word)].slice(0, MAX_HISTORY)
  saveHistory(list)
}

const pickProduct = (item) => {
  remember(item.name)
  panelOpen.value = false
  router.push(`/product/${item.id}`)
}

const pickKeyword = (word) => {
  keyword.value = word
  panelOpen.value = false
  goSearch()
}

const clearHistory = () => {
  saveHistory([])
}

const goSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) return
  remember(kw)
  panelOpen.value = false
  router.push({ path: '/search', query: { keyword: kw } })
}

const closePanel = (event) => {
  if (rootEl.value && !rootEl.value.contains(event.target)) panelOpen.value = false
}

onMounted(() => {
  history.value = readHistory()
  loadHotWords()
  document.addEventListener('click', closePanel)
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  document.removeEventListener('click', closePanel)
})
</script>

<style scoped>
.search-box {
  position: relative;
  flex: 1;
  max-width: 420px;
}

.search-input-wrap {
  display: flex;
  border: 2px solid #000;
  border-radius: 20px;
  overflow: hidden;
  background: white;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 8px 14px;
  font-size: 14px;
  min-width: 0;
}

.search-btn {
  border: none;
  background: #000;
  color: white;
  padding: 0 18px;
  font-size: 14px;
  cursor: pointer;
}

.search-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: white;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  padding: 6px;
  z-index: 300;
  max-height: 420px;
  overflow-y: auto;
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.suggest-item.active {
  background: #f2f2f2;
}

.suggest-thumb {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: 4px;
  background: #f5f5f5;
}

.suggest-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.suggest-name {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.suggest-name :deep(.kw) {
  font-style: normal;
  color: #e63946;
}

.suggest-meta {
  font-size: 12px;
  color: #999;
}

.suggest-price {
  font-size: 14px;
  color: #e63946;
  font-weight: bold;
}

.suggest-empty {
  padding: 14px 10px;
  font-size: 13px;
  color: #999;
  text-align: center;
}

.suggest-group {
  padding: 8px 10px;
}

.suggest-group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.clear-btn {
  border: none;
  background: none;
  color: #999;
  font-size: 12px;
  cursor: pointer;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  border: 1px solid #e5e5e5;
  background: #fafafa;
  border-radius: 14px;
  padding: 4px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.chip:hover {
  border-color: #000;
  color: #000;
}

.hot-rank {
  font-size: 11px;
  color: #bbb;
  font-weight: bold;
}

.hot-rank.top {
  color: #e63946;
}
</style>
