import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register, verifyOtp, resendOtp } from '../api/auth'
import { saveTokens } from '../utils/auth'
import { isValidEmail, isValidUsername } from '../utils/validation'
import DeerIcon from '../components/DeerIcon'
import PasswordStrength from '../components/PasswordStrength'
import Spinner from '../components/Spinner'

export default function Register() {
  const navigate = useNavigate()
  const [step, setStep] = useState('form')
  const [form, setForm] = useState({ username: '', email: '', password: '', passwordConfirm: '' })
  const [otp, setOtp] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [resendTimer, setResendTimer] = useState(0)

  useEffect(() => {
    if (resendTimer <= 0) return
    const t = setTimeout(() => setResendTimer(resendTimer - 1), 1000)
    return () => clearTimeout(t)
  }, [resendTimer])

  const handleRegister = async (e) => {
    e.preventDefault()
    setError('')

    if (!isValidUsername(form.username)) {
      setError('Kullanıcı adı 3-50 karakter, sadece harf/rakam/_ olmalı')
      return
    }
    if (!isValidEmail(form.email)) {
      setError('Sadece Gmail adresleri kabul edilir')
      return
    }
    if (form.password.length < 8) {
      setError('Şifre en az 8 karakter olmalı')
      return
    }
    if (form.password !== form.passwordConfirm) {
      setError('Şifreler eşleşmiyor')
      return
    }

    setLoading(true)
    try {
      await register(form)
      setStep('otp')
      setResendTimer(60)
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.errors?.[0] || 'Kayıt başarısız')
    } finally {
      setLoading(false)
    }
  }

  const handleVerify = async (e) => {
    e.preventDefault()
    setError('')
    if (!/^\d{6}$/.test(otp)) {
      setError('6 haneli OTP kodu girin')
      return
    }

    setLoading(true)
    try {
      const { data } = await verifyOtp(form.email, otp)
      saveTokens(data.accessToken, data.refreshToken)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Geçersiz veya süresi dolmuş OTP')
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    if (resendTimer > 0) return
    setError('')
    try {
      await resendOtp(form.email)
      setResendTimer(60)
    } catch (err) {
      setError(err.response?.data?.message || 'OTP gönderilemedi')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-toz-pembe p-4"
         style={{
           backgroundImage: 'radial-gradient(ellipse at bottom, var(--mint-koyu) 0%, transparent 60%)',
         }}>
      <div className="card w-full max-w-md bg-mint/90 backdrop-blur">
        <div className="flex flex-col items-center mb-6">
          <DeerIcon className="w-16 h-16 mb-3" />
          <h1 className="text-2xl font-bold text-gray-800">
            {step === 'form' ? 'Kayıt Ol' : 'E-posta Doğrulama'}
          </h1>
        </div>

        {step === 'form' ? (
          <form onSubmit={handleRegister} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Kullanıcı Adı</label>
              <input className="input-field" value={form.username}
                     onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Gmail Adresi</label>
              <input type="email" className="input-field" value={form.email}
                     onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Şifre</label>
              <input type="password" className="input-field" value={form.password}
                     onChange={(e) => setForm({ ...form, password: e.target.value })} />
              <PasswordStrength password={form.password} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Şifre Tekrar</label>
              <input type="password" className="input-field" value={form.passwordConfirm}
                     onChange={(e) => setForm({ ...form, passwordConfirm: e.target.value })} />
            </div>

            {error && <div className="bg-red-50 text-red-700 px-4 py-2 rounded-lg text-sm">{error}</div>}

            <button type="submit" className="btn-primary w-full flex items-center justify-center gap-2" disabled={loading}>
              {loading ? <Spinner size="sm" /> : 'Kayıt Ol'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerify} className="space-y-4">
            <p className="text-sm text-gray-600 text-center">
              <strong>{form.email}</strong> adresine 6 haneli kod gönderildi.
            </p>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">OTP Kodu</label>
              <input className="input-field text-center text-2xl tracking-widest" maxLength={6}
                     value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} />
            </div>

            {error && <div className="bg-red-50 text-red-700 px-4 py-2 rounded-lg text-sm">{error}</div>}

            <button type="submit" className="btn-primary w-full flex items-center justify-center gap-2" disabled={loading}>
              {loading ? <Spinner size="sm" /> : 'Doğrula'}
            </button>

            <button type="button" onClick={handleResend} disabled={resendTimer > 0}
                    className="w-full text-sm text-gray-600 hover:text-mint-koyu disabled:opacity-50">
              {resendTimer > 0 ? `Yeniden Gönder (${resendTimer}s)` : 'Yeniden Gönder'}
            </button>
          </form>
        )}

        <p className="text-center mt-6 text-sm text-gray-600">
          Zaten hesabın var mı?{' '}
          <Link to="/login" className="text-mint-koyu font-semibold hover:underline">Giriş yap</Link>
        </p>
      </div>
    </div>
  )
}
