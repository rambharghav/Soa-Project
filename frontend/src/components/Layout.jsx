import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import {
  LayoutDashboard,
  Send,
  History,
  BookOpen,
  User,
  LogOut,
  Wallet,
} from 'lucide-react'

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/send', label: 'Send Money', icon: Send },
  { path: '/transactions', label: 'History', icon: History },
  { path: '/ledger', label: 'Ledger', icon: BookOpen },
  { path: '/profile', label: 'Profile', icon: User },
]

export default function Layout({ children }) {
  const { username, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Top Navbar */}
      <nav className="sticky top-0 z-50 border-b border-slate-500/15"
        style={{
          background: 'linear-gradient(135deg, rgba(10,15,30,0.95), rgba(17,24,39,0.95))',
          backdropFilter: 'blur(20px)',
        }}
      >
        <div className="w-full mx-auto px-4 sm:px-6 lg:px-12 xl:px-16 2xl:px-24">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <Link to="/dashboard" className="flex items-center gap-3 no-underline">
              <div className="w-9 h-9 rounded-xl flex items-center justify-center"
                style={{ background: 'linear-gradient(135deg, #06b6d4, #0891b2)' }}
              >
                <Wallet size={20} className="text-white" />
              </div>
              <span className="text-lg font-bold tracking-tight text-white hidden sm:block">
                Pay<span style={{ color: '#22d3ee' }}>Vault</span>
              </span>
            </Link>

            {/* Desktop Nav (Centered) */}
            <div className="hidden md:flex flex-1 items-center justify-center gap-2 mx-4 lg:mx-8">
              {navItems.map(({ path, label, icon: Icon }) => {
                const isActive = location.pathname === path
                return (
                  <Link
                    key={path}
                    to={path}
                    className="flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all no-underline"
                    style={{
                      color: isActive ? '#22d3ee' : '#94a3b8',
                      background: isActive ? 'rgba(6,182,212,0.08)' : 'transparent',
                      border: isActive ? '1px solid rgba(6,182,212,0.25)' : '1px solid transparent',
                      fontWeight: isActive ? 600 : 500,
                    }}
                  >
                    <Icon size={17} />
                    {label}
                  </Link>
                )
              })}
            </div>

            {/* User Section */}
            <div className="flex items-center gap-3">
              <span className="text-sm font-medium text-slate-300 hidden sm:block">
                {username}
              </span>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-3 py-1.5 rounded-xl text-sm font-medium transition-all cursor-pointer hover:bg-rose-500/10"
                style={{
                  color: '#fb7185',
                  background: 'transparent',
                  border: '1px solid rgba(244,63,94,0.2)',
                }}
                title="Logout"
              >
                <LogOut size={16} />
                <span className="hidden sm:inline">Logout</span>
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Mobile Bottom Nav */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-50 border-t border-slate-800"
        style={{
          background: 'rgba(10,15,30,0.95)',
          backdropFilter: 'blur(20px)',
        }}
      >
        <div className="flex justify-around py-2.5">
          {navItems.map(({ path, label, icon: Icon }) => {
            const isActive = location.pathname === path
            return (
              <Link
                key={path}
                to={path}
                className="flex flex-col items-center gap-1 py-1 px-3 no-underline transition-colors"
                style={{
                  color: isActive ? '#22d3ee' : '#64748b',
                  fontWeight: isActive ? 600 : 500,
                }}
              >
                <Icon size={20} />
                <span className="text-xs">{label}</span>
              </Link>
            )
          })}
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1 w-full mx-auto px-4 sm:px-6 lg:px-12 xl:px-16 2xl:px-24 py-8 pb-24 md:pb-8">
        {children}
      </main>
    </div>
  )
}
