import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { saveTokens } from '../utils/auth'
import DeerIcon from '../components/DeerIcon'
import Spinner from '../components/Spinner'

export default function Login() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!username.trim() || !password) {
      setError('Kullanıcı adı ve şifre gereklidir')
      return
    }

    setLoading(true)
    try {
      const { data } = await login(username.trim(), password)
      saveTokens(data.accessToken, data.refreshToken)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Kullanıcı adı veya şifre hatalı')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-toz-pembe p-4"
         style={{
           backgroundImage: 'radial-gradient(ellipse at bottom, var(--mint-koyu) 0%, transparent 60%)',
         }}>
      <div className="card w-full max-w-md bg-mint/90 backdrop-blur">
        <div className="flex flex-col items-center mb-8">
          <DeerIcon className="w-20 h-20 mb-4" />
          <h1 className="text-2xl font-bold text-gray-800">Fawnly</h1>
          <p className="text-gray-600 text-sm mt-1">Static Application Security Testing</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Kullanıcı Adı</label>
            <input
              type="text"
              className="input-field"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Şifre</label>
            <input
              type="password"
              className="input-field"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </div>

          {error && (
            <div className="bg-red-50 text-red-700 px-4 py-2 rounded-lg text-sm">{error}</div>
          )}

          <button type="submit" className="btn-primary w-full flex items-center justify-center gap-2" disabled={loading}>
            {loading ? <Spinner size="sm" /> : 'Giriş Yap'}
          </button>
        </form>

        <p className="text-center mt-6 text-sm text-gray-600">
          Hesabın yok mu?{' '}
          <Link to="/register" className="text-mint-koyu font-semibold hover:underline">
            Kayıt ol
          </Link>
        </p>
      </div>
    </div>
  )
}
