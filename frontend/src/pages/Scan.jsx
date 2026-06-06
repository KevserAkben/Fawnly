import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { startGitScan, startZipScan, getScanStatus } from '../api/scans'
import { isValidGithubUrl } from '../utils/validation'
import Spinner from '../components/Spinner'
import ProgressBar from '../components/ProgressBar'

export default function Scan() {
  const navigate = useNavigate()
  const [tab, setTab] = useState('git')
  const [projectName, setProjectName] = useState('')
  const [githubUrl, setGithubUrl] = useState('')
  const [zipFile, setZipFile] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [scanId, setScanId] = useState(null)
  const [progress, setProgress] = useState(0)
  const [scanError, setScanError] = useState('')
  const pollRef = useRef(null)

  useEffect(() => {
    return () => {
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [])

  const startPolling = (id) => {
    setScanning(true)
    setScanId(id)
    setProgress(10)

    pollRef.current = setInterval(async () => {
      try {
        const { data } = await getScanStatus(id)
        setProgress(data.progress)

        if (data.status === 'done') {
          clearInterval(pollRef.current)
          navigate(`/results/${id}`)
        } else if (data.status === 'failed') {
          clearInterval(pollRef.current)
          setScanning(false)
          setScanError(data.errorMessage || 'Tarama başarısız oldu')
        }
      } catch {
        clearInterval(pollRef.current)
        setScanning(false)
        setScanError('Durum sorgulanamadı')
      }
    }, 3000)
  }

  const handleStart = async () => {
    setError('')
    setScanError('')

    if (!projectName.trim()) {
      setError('Proje adı zorunludur')
      return
    }
    if (projectName.length > 100) {
      setError('Proje adı en fazla 100 karakter olabilir')
      return
    }

    if (tab === 'git') {
      if (!isValidGithubUrl(githubUrl)) {
        setError('Geçerli bir GitHub URL girin (github.com/...)')
        return
      }
    } else {
      if (!zipFile) {
        setError('ZIP dosyası seçin')
        return
      }
      if (!zipFile.name.toLowerCase().endsWith('.zip')) {
        setError('Sadece .zip dosyaları kabul edilir')
        return
      }
      if (zipFile.size > 20 * 1024 * 1024) {
        setError('ZIP dosyası en fazla 20 MB olabilir')
        return
      }
    }

    setLoading(true)
    try {
      let response
      if (tab === 'git') {
        response = await startGitScan(projectName.trim(), githubUrl.trim())
      } else {
        response = await startZipScan(projectName.trim(), zipFile)
      }
      setLoading(false)
      startPolling(response.data.id)
    } catch (err) {
      setLoading(false)
      setError(err.response?.data?.message || 'Tarama başlatılamadı')
    }
  }

  const handleRetry = () => {
    setScanning(false)
    setScanId(null)
    setProgress(0)
    setScanError('')
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Yeni Tarama</h1>

      <div className="card space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Proje Adı *</label>
          <input
            className="input-field"
            value={projectName}
            onChange={(e) => setProjectName(e.target.value)}
            maxLength={100}
            disabled={scanning || loading}
            placeholder="Örn: my-java-app"
          />
        </div>

        <div className="flex border-b border-gray-200">
          {['git', 'zip'].map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              disabled={scanning || loading}
              className={`px-6 py-3 font-medium text-sm transition-colors ${
                tab === t
                  ? 'border-b-2 border-mint-koyu text-mint-koyu'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {t === 'git' ? 'GitHub URL' : 'ZIP Yükle'}
            </button>
          ))}
        </div>

        {tab === 'git' ? (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">GitHub URL</label>
            <input
              className="input-field"
              value={githubUrl}
              onChange={(e) => setGithubUrl(e.target.value)}
              disabled={scanning || loading}
              placeholder="https://github.com/user/repo"
            />
          </div>
        ) : (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">ZIP Dosyası (max 20 MB)</label>
            <input
              type="file"
              accept=".zip"
              className="input-field"
              onChange={(e) => setZipFile(e.target.files[0])}
              disabled={scanning || loading}
            />
            {zipFile && (
              <p className="text-sm text-gray-500 mt-1">
                {zipFile.name} ({(zipFile.size / 1024 / 1024).toFixed(2)} MB)
              </p>
            )}
          </div>
        )}

        {error && <div className="bg-red-50 text-red-700 px-4 py-2 rounded-lg text-sm">{error}</div>}

        {scanning && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600 flex items-center gap-2">
              <Spinner size="sm" /> Tarama çalışıyor... (Scan #{scanId})
            </p>
            <ProgressBar progress={progress} />
          </div>
        )}

        {scanError && (
          <div className="space-y-3">
            <div className="bg-red-50 text-red-700 px-4 py-2 rounded-lg text-sm">{scanError}</div>
            <button onClick={handleRetry} className="btn-secondary">Tekrar Dene</button>
          </div>
        )}

        {!scanning && !scanError && (
          <button
            onClick={handleStart}
            className="btn-primary w-full flex items-center justify-center gap-2"
            disabled={loading}
          >
            {loading ? <><Spinner size="sm" /> Başlatılıyor...</> : 'Start Scan'}
          </button>
        )}
      </div>
    </div>
  )
}
