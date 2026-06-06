import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getScanHistory, deleteScan } from '../api/scans'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'

export default function History() {
  const [scans, setScans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [deleting, setDeleting] = useState(null)

  const load = () => {
    setLoading(true)
    getScanHistory()
      .then((res) => setScans(res.data))
      .catch(() => setError('Geçmiş yüklenemedi'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleDelete = async (id) => {
    if (!confirm('Bu taramayı silmek istediğinize emin misiniz?')) return
    setDeleting(id)
    try {
      await deleteScan(id)
      setScans((prev) => prev.filter((s) => s.id !== id))
    } catch {
      alert('Silme başarısız')
    } finally {
      setDeleting(null)
    }
  }

  const formatDate = (d) => d ? new Date(d).toLocaleString('tr-TR') : '-'

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spinner size="lg" /></div>
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Tarama Geçmişi</h1>

      {error && <div className="text-red-600 mb-4">{error}</div>}

      <div className="card overflow-x-auto">
        {scans.length === 0 ? (
          <p className="text-gray-500 text-center py-8">Henüz tarama yok</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-gray-600">
                <th className="pb-3 pr-4">Proje Adı</th>
                <th className="pb-3 pr-4">Tarih</th>
                <th className="pb-3 pr-4">Bulgu Sayısı</th>
                <th className="pb-3 pr-4">Durum</th>
                <th className="pb-3">İşlemler</th>
              </tr>
            </thead>
            <tbody>
              {scans.map((scan) => (
                <tr key={scan.id} className="border-b border-gray-100">
                  <td className="py-3 pr-4 font-medium">{scan.projectName}</td>
                  <td className="py-3 pr-4">{formatDate(scan.createdAt)}</td>
                  <td className="py-3 pr-4">{scan.findingCount}</td>
                  <td className="py-3 pr-4"><StatusBadge status={scan.status} /></td>
                  <td className="py-3 flex gap-2">
                    {scan.status === 'done' && (
                      <Link to={`/results/${scan.id}`}
                            className="btn-primary text-xs px-3 py-1.5">
                        View
                      </Link>
                    )}
                    <button
                      onClick={() => handleDelete(scan.id)}
                      disabled={deleting === scan.id}
                      className="btn-secondary text-xs px-3 py-1.5"
                    >
                      {deleting === scan.id ? '...' : 'Delete'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
