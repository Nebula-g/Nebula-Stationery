<template>
  <div class="register-page">
    <div class="register-container">
      <h1 class="register-title">注册星云商城</h1>
      <form @submit.prevent="handleRegister" class="register-form">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" class="input" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" class="input" placeholder="请输入密码" required />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input v-model="confirmPassword" type="password" class="input" placeholder="请再次输入密码" required />
        </div>
        <div class="form-group">
          <label>手机号（选填，绑定后可用验证码登录）</label>
          <input v-model="phone" type="tel" maxlength="11" class="input" placeholder="请输入 11 位手机号" />
        </div>
        <div v-if="phone" class="form-group">
          <label>短信验证码</label>
          <div class="code-row">
            <input v-model="code" type="text" maxlength="6" class="input" placeholder="6 位验证码" />
            <button type="button" class="btn-code" :disabled="countdown > 0 || sending" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : sending ? '发送中…' : '发送验证码' }}
            </button>
          </div>
        </div>
        <p v-if="codeHint" class="code-hint">{{ codeHint }}</p>
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="register-footer">
        已有账号？<router-link to="/login" class="link">立即登录</router-link>
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

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const phone = ref('')
const code = ref('')
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
      code.value = data.devCode
      codeHint.value = `模拟短信通道：验证码 ${data.devCode} 已自动填入（接真实短信通道后不再回显）`
    } else {
      codeHint.value = '验证码已发送，5 分钟内有效'
    }
  } catch (e) {
    toast.error(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const handleRegister = async () => {
  if (password.value !== confirmPassword.value) {
    toast.error('两次输入的密码不一致')
    return
  }
  if (phone.value && !code.value) {
    toast.error('请填写短信验证码')
    return
  }

  loading.value = true
  try {
    await userStore.register(username.value, password.value, phone.value.trim(), code.value.trim())
    toast.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    toast.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(stopTimer)
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #000000 0%, #333333 100%);
}

.register-container {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 400px;
}

.register-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
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

.btn-block {
  width: 100%;
  padding: 12px;
  font-size: 16px;
}

.register-footer {
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
