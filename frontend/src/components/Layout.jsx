import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

export default function Layout() {
  return (
    <div className="min-h-screen bg-toz-pembe/30">
      <Sidebar />
      <main className="ml-64 max-md:ml-56 p-6 md:p-8 min-h-screen">
        <Outlet />
      </main>
    </div>
  )
}
