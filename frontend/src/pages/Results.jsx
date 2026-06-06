import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getFindings, updateTriage, updateNote } from '../api/scans'
import SeverityBadge from '../components/SeverityBadge'
import Spinner from '../components/Spinner'

const TRIAGE_OPTIONS = ['True Positive', 'False Positive', 'Not Exploitable', 'Needs Review']

export default function Results() {
  const { scanId } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [severityFilter, setSeverityFilter] = useState('ALL')
  const [triageFilter, setTriageFilter] = useState('ALL')
  const [notes, setNotes] = useState({})
  const [savingNote, setSavingNote] = useState(null)

  useEffect(() => {
    getFindings(scanId)
      .then((res) => {
        setData(res.data)
        const initialNotes = {}
        res.data.findings.forEach((f) => { initialNotes[f.id] = f.note || '' })
        setNotes(initialNotes)
      })
      .catch(() => setError('Sonuçlar yüklenemedi'))
      .finally(() => setLoading(false))
  }, [scanId])

  const handleTriageChange = async (findingId, triageStatus) => {
    try {
      await updateTriage(scanId, findingId, triageStatus)
      setData((prev) => ({
        ...prev,
        findings: prev.findings.map((f) =>
          f.id === findingId ? { ...f, triageStatus } : f
        ),
      }))
    } catch {
      alert('Triage güncellenemedi')
    }
  }

  const handleSaveNote = async (findingId) => {
    setSavingNote(findingId)
    try {
      await updateNote(scanId, findingId, notes[findingId])
    } catch {
      alert('Not kaydedilemedi')
    } finally {
      setSavingNote(null)
    }
  }

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner size="lg" /></div>
  }

  if (error) return <div className="text-red-600">{error}</div>

  const filtered = data.findings.filter((f) => {
    if (severityFilter !== 'ALL' && f.severity !== severityFilter) return false
    if (triageFilter !== 'ALL' && f.triageStatus !== triageFilter) return false
    return true
  })

  const formatDate = (d) => d ? new Date(d).toLocaleString('tr-TR') : '-'

  return (
    <div>
      <Link to="/history" className="text-mint-koyu hover:underline text-sm mb-4 inline-block">
        ← Back to scan history
      </Link>

      <h1 className="text-2xl font-bold text-gray-800 mb-6">
        {data.projectName} — Sonuçlar
      </h1>

      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mb-6">
        <div className="card text-center">
          <p className="text-sm text-gray-600">Toplam</p>
          <p className="text-2xl font-bold">{data.total}</p>
        </div>
        <div className="card text-center" style={{ borderTop: '3px solid #ef4444' }}>
          <p className="text-sm text-gray-600">HIGH</p>
          <p className="text-2xl font-bold text-red-500">{data.high}</p>
        </div>
        <div className="card text-center" style={{ borderTop: '3px solid #f97316' }}>
          <p className="text-sm text-gray-600">MEDIUM</p>
          <p className="text-2xl font-bold text-orange-500">{data.medium}</p>
        </div>
        <div className="card text-center" style={{ borderTop: '3px solid #3b82f6' }}>
          <p className="text-sm text-gray-600">LOW</p>
          <p className="text-2xl font-bold text-blue-500">{data.low}</p>
        </div>
        <div className="card text-center col-span-2 sm:col-span-1">
          <p className="text-sm text-gray-600">Tarama Tarihi</p>
          <p className="text-sm font-medium mt-1">{formatDate(data.scanDate)}</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-3 mb-4">
        <select className="input-field w-auto" value={severityFilter}
                onChange={(e) => setSeverityFilter(e.target.value)}>
          <option value="ALL">Tüm Severity</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
        </select>
        <select className="input-field w-auto" value={triageFilter}
                onChange={(e) => setTriageFilter(e.target.value)}>
          <option value="ALL">Tüm Triage</option>
          {TRIAGE_OPTIONS.map((o) => <option key={o} value={o}>{o}</option>)}
        </select>
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-gray-600">
              <th className="pb-3 pr-3">Severity</th>
              <th className="pb-3 pr-3">Rule ID</th>
              <th className="pb-3 pr-3">OWASP</th>
              <th className="pb-3 pr-3">CWE</th>
              <th className="pb-3 pr-3">Dosya:Satır</th>
              <th className="pb-3 pr-3">Açıklama</th>
              <th className="pb-3 pr-3">Triage</th>
              <th className="pb-3">Not</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={8} className="py-8 text-center text-gray-500">Bulgu yok</td></tr>
            ) : filtered.map((f) => (
              <tr key={f.id} className="border-b border-gray-100 align-top">
                <td className="py-3 pr-3"><SeverityBadge severity={f.severity} /></td>
                <td className="py-3 pr-3 font-mono text-xs">{f.ruleId}</td>
                <td className="py-3 pr-3">{f.owaspCode || '-'}</td>
                <td className="py-3 pr-3">{f.cwe || '-'}</td>
                <td className="py-3 pr-3 font-mono text-xs whitespace-nowrap">
                  {f.filePath}:{f.lineNo}
                </td>
                <td className="py-3 pr-3 max-w-xs">{f.message}</td>
                <td className="py-3 pr-3">
                  <select
                    className="input-field text-xs py-1"
                    value={f.triageStatus}
                    onChange={(e) => handleTriageChange(f.id, e.target.value)}
                  >
                    {TRIAGE_OPTIONS.map((o) => <option key={o} value={o}>{o}</option>)}
                  </select>
                </td>
                <td className="py-3">
                  <textarea
                    className="input-field text-xs min-w-[150px]"
                    rows={2}
                    value={notes[f.id] || ''}
                    onChange={(e) => setNotes({ ...notes, [f.id]: e.target.value })}
                  />
                  <button
                    onClick={() => handleSaveNote(f.id)}
                    className="text-xs text-mint-koyu hover:underline mt-1"
                    disabled={savingNote === f.id}
                  >
                    {savingNote === f.id ? 'Kaydediliyor...' : 'Save Note'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
