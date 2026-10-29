import axios from 'axios'
import router from '../router'

// 简单的消息提示
const showMessage = (message, type = 'error') => {
  // 创建 toast 提示
  const toast = document.createElement('div')
  toast.className = `toast toast-${type}`
  toast.textContent = message
  toast.style.cssText = `
    position: fixed;
    top: 20px;
    left: 50%;
    transform: translateX(-50%);
    padding: 12px 24px;
    border-radius: 4px;
    color: white;
    font-size: 14px;
    z-index: 9999;
    background-color: ${type === 'error' ? '#dc3545' : '#28a745'};
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  `
  document.body.appendChild(toast)
  setTimeout(() => toast.remove(), 3000)
}

const api = axios.create({
  baseURL: '',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        router.push('/login')
        showMessage('登录已过期，请重新登录')
      } else if (status === 403) {
        showMessage('无权限访问')
      } else if (data && data.message) {
        showMessage(data.message)
      } else {
        showMessage('请求失败')
      }
    } else {
      showMessage('网络错误')
    }

    return Promise.reject(error)
  }
)

export default api
