import axios from 'axios'

const api = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    if (!config.url?.startsWith('/api/auth')) {
      const token = localStorage.getItem('pv_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('pv_token')
      localStorage.removeItem('pv_userId')
      localStorage.removeItem('pv_username')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
