import client from './client'

export const login = (username, password) =>
  client.post('/api/auth/login', { username, password })

export const register = (data) =>
  client.post('/api/auth/register', data)

export const verifyOtp = (email, code) =>
  client.post('/api/auth/verify-otp', { email, code })

export const resendOtp = (email) =>
  client.post('/api/auth/resend-otp', { email })

export const logout = () => {
  const refreshToken = localStorage.getItem('refreshToken')
  return client.post('/api/auth/logout', { refreshToken })
}
