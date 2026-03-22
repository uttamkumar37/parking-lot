import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import {
  HomeIcon, QueueListIcon, TruckIcon, Cog6ToothIcon,
  ArrowRightOnRectangleIcon, UserCircleIcon, ShieldCheckIcon,
} from '@heroicons/react/24/outline'

const navItems = [
  { to: '/dashboard', label: 'Dashboard',    Icon: HomeIcon },
  { to: '/park',      label: 'Park Vehicle', Icon: TruckIcon },
  { to: '/bookings',  label: 'My Bookings',  Icon: QueueListIcon },
]

export default function Layout() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex bg-gray-50">
      {/* Sidebar */}
      <aside className="w-64 bg-gradient-to-b from-primary-900 to-primary-800 text-white flex flex-col shadow-xl shrink-0">
        <div className="p-6 border-b border-primary-800">
          <div className="flex items-center gap-3">
            <ShieldCheckIcon className="h-8 w-8 text-primary-300" />
            <span className="text-xl font-bold tracking-tight">ParkSmart</span>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1">
          {navItems.map(({ to, label, Icon }) => (
            <NavLink
              key={to} to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-700 text-white'
                    : 'text-primary-200 hover:bg-primary-800 hover:text-white'
                }`
              }
            >
              <Icon className="h-5 w-5" />
              {label}
            </NavLink>
          ))}

          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-700 text-white'
                    : 'text-primary-200 hover:bg-primary-800 hover:text-white'
                }`
              }
            >
              <Cog6ToothIcon className="h-5 w-5" />
              Admin
            </NavLink>
          )}
        </nav>

        <div className="p-4 border-t border-primary-800">
          <div className="flex items-center gap-3 mb-3">
            <UserCircleIcon className="h-8 w-8 text-primary-300" />
            <div className="min-w-0">
              <p className="text-sm font-medium truncate">{user?.name}</p>
              <p className="text-xs text-primary-400 truncate">{user?.email}</p>
            </div>
          </div>
          <button onClick={handleLogout} className="flex items-center gap-2 text-primary-300 hover:text-white text-sm w-full">
            <ArrowRightOnRectangleIcon className="h-4 w-4" />
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto">
        <div className="max-w-7xl mx-auto p-6">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
