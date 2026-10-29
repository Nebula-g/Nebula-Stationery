<template>
  <div class="login-page">
    <div class="login-container">
      <h1 class="login-title">登录星云商城</h1>

      <div class="login-tabs">
        <button class="login-tab" :class="{ active: mode === 'password' }" @click="switchMode('password')">账号密码</button>
        <button class="login-tab" :class="{ active: mode === 'sms' }" @click="switchMode('sms')">手机验证码</button>
      </div>

      <form v-if="mode === 'password'" @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" class="input" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" class="input" placeholder="请输入密码" required />
        </div>
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else @submit.prevent="handleSmsLogin" class="login-form">
        <div class="form-group">
          <label>手机号</label>
          <input v-model="phone" type="tel" maxlength="11" class="input" placeholder="请输入 11 位手机号" required />
        </div>
        <div class="form-group">
          <label>验证码</label>
          <div class="code-row">
            <input v-model="smsCode" type="text" maxlength="6" class="input" placeholder="6 位验证码" required />
            <button type="button" class="btn-code" :disabled="countdown > 0 || sending" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : sending ? '发送中…' : '发送验证码' }}
            </button>
          </div>
        </div>
        <p v-if="codeHint" class="code-hint">{{ codeHint }}</p>
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? '登录中...' : '登录 / 自动注册' }}
        </button>
        <p class="sms-tip">未注册的手机号验证通过后会自动创建账号</p>
      </form>

      <p class="login-footer">
        还没有账号？<router-link to="/register" class="link">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import toast from '../utils/toast'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('password')
const username = ref('')
const password = ref('')
const phone = ref('')
const smsCode = ref('')
const codeHint = ref('')
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer = null

const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const switchMode = (next) => {
  mode.value = next
  codeHint.value = ''
}

const sendCode = async () => {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    toast.error('请输入正确的 11 位手机号')
    return
  }
  sending.value = true
  try {
    const data = await userStore.sendSmsCode(phone.value)
    countdown.value = data.resendIn || 60
    stopTimer()
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) stopTimer()
    }, 1000)

    if (data.mock && data.devCode) {
      smsCode.value = data.devCode
      codeHint.value = `模拟短信通道：验证码 ${data.devCode} 已自动填入（接真实短信通道后不再回显）`
      toast.success('验证码已发送（模拟通道，已自动填入）')
    } else {
      codeHint.value = '验证码已发送，5 分钟内有效'
      toast.success('验证码已发送')
    }
  } catch (e) {
    toast.error(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const jumpByRole = () => {
  router.push(userStore.role === '1' ? '/admin' : '/')
}

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(username.value, password.value)
    jumpByRole()
  } catch (e) {
    toast.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleSmsLogin = async () => {
  loading.value = true
  try {
    await userStore.loginBySms(phone.value, smsCode.value)
    jumpByRole()
  } catch (e) {
    toast.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(stopTimer)
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #000000 0%, #333333 100%);
}

.login-container {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 400px;
}

.login-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 22px;
  border-bottom: 1px solid #eee;
}

.login-tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: none;
  font-size: 14px;
  color: #999;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.login-tab.active {
  color: #333;
  font-weight: bold;
  border-bottom-color: #333;
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

.code-row {
  display: flex;
  gap: 8px;
}

.code-row .input {
  flex: 1;
}

.btn-code {
  flex: 0 0 116px;
  border: 1px solid #333;
  background: white;
  color: #333;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  padding: 0 8px;
}

.btn-code:disabled {
  border-color: #ddd;
  color: #bbb;
  cursor: not-allowed;
}

.code-hint {
  font-size: 12px;
  color: #2a9d8f;
  background: #e9f7ef;
  border-radius: 6px;
  padding: 8px 10px;
  margin: -8px 0 16px;
  line-height: 1.6;
}

.sms-tip {
  text-align: center;
  font-size: 12px;
  color: #aaa;
  margin-top: 12px;
}

.btn-block {
  width: 100%;
  padding: 12px;
  font-size: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.link {
  color: #000000;
}

.link:hover {
  text-decoration: underline;
}
</style>
