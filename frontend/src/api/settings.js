import client from './client'

export const updateUsername = (username) =>
  client.put('/api/settings/username', { username })

export const requestPasswordOtp = () =>
  client.post('/api/settings/password/request-otp')

export const changePassword = (data) =>
  client.put('/api/settings/password', data)

export const getSessions = () =>
  client.get('/api/settings/sessions')

export const revokeSession = (sessionId) =>
  client.delete(`/api/settings/sessions/${sessionId}`)

export const getDashboard = () =>
  client.get('/api/dashboard')
