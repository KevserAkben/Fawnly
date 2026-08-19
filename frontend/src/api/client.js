import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const client = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

const skipRefresh = (url = '') =>
  /\/api\/auth\/(login|register|verify-otp|resend-otp|refresh)$/.test(url)

client.interceptors.request.use((config) => {
  if (config.data instanceof FormData) {
    if (typeof config.headers?.delete === 'function') {
      config.headers.delete('Content-Type')
    } else {
      delete config.headers['Content-Type']
    }
  }

  const token = localStorage.getItem('accessToken')
  const refreshToken = localStorage.getItem('refreshToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (refreshToken) {
    config.headers['X-Refresh-Token'] = refreshToken
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (!original || error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }
    if (skipRefresh(original.url)) {
      return Promise.reject(error)
    }

    original._retry = true
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      try {
        const { data } = await axios.post(`${API_BASE}/api/auth/refresh`, { refreshToken })
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return client(original)
      } catch {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
