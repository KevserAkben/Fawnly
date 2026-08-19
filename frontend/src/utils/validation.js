const GITHUB_REGEX = /^https:\/\/(?:www\.)?github\.com\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+(?:\/.*)?\/?$/i

export function isValidGithubUrl(url) {
  return GITHUB_REGEX.test(url?.trim())
}

export function getPasswordStrength(password) {
  if (!password) return { level: 0, label: '', color: '' }
  let score = 0
  if (password.length >= 8) score++
  if (password.length >= 12) score++
  if (/[A-Z]/.test(password)) score++
  if (/[a-z]/.test(password)) score++
  if (/[0-9]/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++

  if (score <= 2) return { level: 1, label: 'Zayıf', color: '#ef4444' }
  if (score <= 4) return { level: 2, label: 'Orta', color: '#f97316' }
  return { level: 3, label: 'Güçlü', color: '#5ecf8a' }
}

export function isValidEmail(email) {
  return /^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(email?.trim())
}

export function isValidUsername(username) {
  return /^[a-zA-Z0-9_]{3,50}$/.test(username?.trim())
}
