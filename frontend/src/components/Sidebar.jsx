import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import { clearTokens } from '../utils/auth'
import DeerIcon from './DeerIcon'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: '📊' },
  { to: '/scan', label: 'Scan', icon: '🔍' },
  { to: '/history', label: 'Results', icon: '📋' },
  { to: '/settings', label: 'Settings', icon: '⚙️' },
]

export default function Sidebar() {
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await logout()
    } catch {
      // proceed with local cleanup
    }
    clearTokens()
    navigate('/login')
  }

  return (
    <aside className="fixed left-0 top-0 h-full w-64 bg-mint flex flex-col shadow-lg z-40
                       md:w-64 max-md:w-56">
      <div className="p-6 flex items-center gap-3 border-b border-mint-koyu/30">
        <DeerIcon className="w-10 h-10" />
        <span className="text-xl font-bold text-gray-800">Fawnly</span>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-colors ${
                isActive
                  ? 'bg-mint-koyu text-white'
                  : 'text-gray-700 hover:bg-pembe-koyu/40'
              }`
            }
          >
            <span>{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-mint-koyu/30">
        <button
          onClick={handleLogout}
          className="w-full btn-secondary text-center"
        >
          Logout
        </button>
      </div>
    </aside>
  )
}
