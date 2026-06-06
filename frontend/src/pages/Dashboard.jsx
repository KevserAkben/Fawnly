import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDashboard } from '../api/settings'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getDashboard()
      .then((res) => setData(res.data))
      .catch(() => setError('Dashboard yüklenemedi'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error) {
    return <div className="text-red-600">{error}</div>
  }

  const stats = [
    { label: 'Toplam Tarama', value: data.totalScans, color: 'bg-mint' },
    { label: 'Tamamlanan', value: data.completedScans, color: 'bg-green-100' },
    { label: 'Başarısız', value: data.failedScans, color: 'bg-red-100' },
    { label: 'Toplam Bulgu', value: data.totalFindings, color: 'bg-toz-pembe' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-2">
        Hoş geldin, {data.username}!
      </h1>
      <p className="text-gray-600 mb-8">Güvenlik tarama özetiniz</p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {stats.map((s) => (
          <div key={s.label} className={`card ${s.color}`}>
            <p className="text-sm text-gray-600">{s.label}</p>
            <p className="text-3xl font-bold text-gray-800 mt-1">{s.value}</p>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold">Son Taramalar</h2>
          <Link to="/scan" className="btn-primary text-sm">Yeni Tarama</Link>
        </div>

        {data.recentScans?.length === 0 ? (
          <p className="text-gray-500 text-center py-8">Henüz tarama yok. İlk taramanızı başlatın!</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-600">
                  <th className="pb-3 pr-4">Proje</th>
                  <th className="pb-3 pr-4">Durum</th>
                  <th className="pb-3 pr-4">Bulgu</th>
                  <th className="pb-3">İşlem</th>
                </tr>
              </thead>
              <tbody>
                {data.recentScans.map((scan) => (
                  <tr key={scan.id} className="border-b border-gray-100">
                    <td className="py-3 pr-4 font-medium">{scan.projectName}</td>
                    <td className="py-3 pr-4"><StatusBadge status={scan.status} /></td>
                    <td className="py-3 pr-4">{scan.findingCount}</td>
                    <td className="py-3">
                      {scan.status === 'done' && (
                        <Link to={`/results/${scan.id}`} className="text-mint-koyu hover:underline">
                          Görüntüle
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
