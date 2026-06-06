export function isAuthenticated() {
  return !!localStorage.getItem('accessToken')
}

export function clearTokens() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

export function saveTokens(accessToken, refreshToken) {
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
}
