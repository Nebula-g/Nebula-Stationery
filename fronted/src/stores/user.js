import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === '1')

  const setToken = (newToken, userRole, userName) => {
    token.value = newToken
    role.value = userRole
    username.value = userName
    localStorage.setItem('token', newToken)
    localStorage.setItem('role', userRole)
    localStorage.setItem('username', userName)
  }

  const logout = () => {
    token.value = ''
    role.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
  }

  const login = async (username, password) => {
    const res = await api.post('/api/auth/login', { username, password })
    if (res.data.code === 200) {
      setToken(res.data.data.token, res.data.data.role, username)
      return true
    }
    throw new Error(res.data.message)
  }

  const register = async (username, password) => {
    const res = await api.post('/api/auth/register', { username, password })
    if (res.data.code === 200) {
      return true
    }
    throw new Error(res.data.message)
  }

  return {
    token,
    role,
    username,
    isLoggedIn,
    isAdmin,
    setToken,
    logout,
    login,
    register
  }
})
