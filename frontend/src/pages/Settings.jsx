import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  updateUsername, requestPasswordOtp, changePassword, getSessions, revokeSession,
} from '../api/settings'
import { clearTokens } from '../utils/auth'
import PasswordStrength from '../components/PasswordStrength'
import Spinner from '../components/Spinner'

export default function Settings() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [usernameLoading, setUsernameLoading] = useState(false)
  const [usernameMsg, setUsernameMsg] = useState('')

  const [pwStep, setPwStep] = useState('idle')
  const [pwForm, setPwForm] = useState({ code: '', newPassword: '', newPasswordConfirm: '' })
  const [pwLoading, setPwLoading] = useState(false)
  const [pwMsg, setPwMsg] = useState('')
  const [pwError, setPwError] = useState('')

  const [sessions, setSessions] = useState([])
  const [sessionsLoading, setSessionsLoading] = useState(true)

  useEffect(() => {
    getSessions()
      .then((res) => setSessions(res.data))
      .catch(() => {})
      .finally(() => setSessionsLoading(false))
  }, [])

  const handleUsernameSave = async () => {
    setUsernameMsg('')
    if (!username.trim() || username.length < 3) {
      setUsernameMsg('Geçersiz kullanıcı adı')
      return
    }
    setUsernameLoading(true)
    try {
      const res = await updateUsername(username.trim())
      setUsernameMsg(res.data.message)
    } catch (err) {
      setUsernameMsg(err.response?.data?.message || 'Güncelleme başarısız')
    } finally {
      setUsernameLoading(false)
    }
  }

  const handleRequestPwOtp = async () => {
    setPwError('')
    setPwLoading(true)
    try {
      await requestPasswordOtp()
      setPwStep('form')
      setPwMsg('OTP e-postanıza gönderildi')
    } catch (err) {
      setPwError(err.response?.data?.message || 'OTP gönderilemedi')
    } finally {
      setPwLoading(false)
    }
  }

  const handleChangePassword = async () => {
    setPwError('')
    if (!/^\d{6}$/.test(pwForm.code)) {
      setPwError('6 haneli OTP girin')
      return
    }
    if (pwForm.newPassword.length < 8) {
      setPwError('Şifre en az 8 karakter olmalı')
      return
    }
    if (pwForm.newPassword !== pwForm.newPasswordConfirm) {
      setPwError('Şifreler eşleşmiyor')
      return
    }

    setPwLoading(true)
    try {
      await changePassword(pwForm)
      clearTokens()
      navigate('/login')
    } catch (err) {
      setPwError(err.response?.data?.message || 'Şifre değiştirilemedi')
    } finally {
      setPwLoading(false)
    }
  }

  const handleRevokeSession = async (sessionId) => {
    try {
      await revokeSession(sessionId)
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    } catch {
      alert('Oturum kapatılamadı')
    }
  }

  const formatDate = (d) => new Date(d).toLocaleString('tr-TR')

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold text-gray-800">Ayarlar</h1>

      <div className="card space-y-4">
        <h2 className="text-lg font-semibold">Kullanıcı Adı</h2>
        <input
          className="input-field"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Yeni kullanıcı adı"
        />
        {usernameMsg && (
          <p className={`text-sm ${usernameMsg.includes('success') || usernameMsg.includes('başarı') ? 'text-green-600' : 'text-gray-600'}`}>
            {usernameMsg}
          </p>
        )}
        <button onClick={handleUsernameSave} className="btn-primary" disabled={usernameLoading}>
          {usernameLoading ? <Spinner size="sm" /> : 'Kaydet'}
        </button>
      </div>

      <div className="card space-y-4">
        <h2 className="text-lg font-semibold">Şifre Değiştir</h2>

        {pwStep === 'idle' ? (
          <button onClick={handleRequestPwOtp} className="btn-secondary" disabled={pwLoading}>
            {pwLoading ? <Spinner size="sm" /> : 'Change Password'}
          </button>
        ) : (
          <div className="space-y-3">
            {pwMsg && <p className="text-sm text-green-600">{pwMsg}</p>}
            <input
              className="input-field"
              placeholder="6 haneli OTP"
              maxLength={6}
              value={pwForm.code}
              onChange={(e) => setPwForm({ ...pwForm, code: e.target.value.replace(/\D/g, '') })}
            />
            <input
              type="password"
              className="input-field"
              placeholder="Yeni şifre"
              value={pwForm.newPassword}
              onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
            />
            <PasswordStrength password={pwForm.newPassword} />
            <input
              type="password"
              className="input-field"
              placeholder="Yeni şifre tekrar"
              value={pwForm.newPasswordConfirm}
              onChange={(e) => setPwForm({ ...pwForm, newPasswordConfirm: e.target.value })}
            />
            {pwError && <p className="text-sm text-red-600">{pwError}</p>}
            <button onClick={handleChangePassword} className="btn-primary" disabled={pwLoading}>
              {pwLoading ? <Spinner size="sm" /> : 'Şifreyi Güncelle'}
            </button>
          </div>
        )}
      </div>

      <div className="card space-y-4">
        <h2 className="text-lg font-semibold">Aktif Oturumlar</h2>
        {sessionsLoading ? (
          <Spinner />
        ) : sessions.length === 0 ? (
          <p className="text-gray-500">Aktif oturum yok</p>
        ) : (
          <div className="space-y-3">
            {sessions.map((s) => (
              <div key={s.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-sm">{s.deviceInfo || 'Unknown Device'}</p>
                  <p className="text-xs text-gray-500">{formatDate(s.createdAt)}</p>
                  {s.current && <span className="text-xs text-mint-koyu">Mevcut oturum</span>}
                </div>
                {!s.current && (
                  <button onClick={() => handleRevokeSession(s.id)}
                          className="text-sm text-red-600 hover:underline">
                    Kapat
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
