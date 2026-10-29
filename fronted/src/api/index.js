import axios from 'axios'
import router from '../router'
import toast from '../utils/toast'

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
        toast.error('登录已过期，请重新登录')
      } else if (status === 403) {
        toast.error('无权限访问')
      } else if (data && data.message) {
        toast.error(data.message)
      } else {
        toast.error('请求失败')
      }
    } else {
      toast.error('网络错误')
    }

    return Promise.reject(error)
  }
)

export default api
