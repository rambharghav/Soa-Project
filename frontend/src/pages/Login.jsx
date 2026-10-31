import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import { Wallet, Eye, EyeOff, LogIn } from 'lucide-react'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const { login, loading } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!username.trim() || !password.trim()) {
      toast.error('Please fill in all fields')
      return
    }

    const result = await login(username.trim(), password)

    if (result.success) {
      toast.success('Login successful!')
      if (result.hasProfile) {
        navigate('/dashboard')
      } else {
        navigate('/complete-profile')
      }
    } else {
      toast.error(result.message)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4"
      style={{
        background: 'radial-gradient(ellipse at top, rgba(6,182,212,0.08) 0%, transparent 50%), #030712',
      }}
    >
      <div className="w-full max-w-md animate-fade-in">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 rounded-2xl mx-auto mb-4 flex items-center justify-center animate-pulse-glow"
            style={{ background: 'linear-gradient(135deg, #06b6d4, #0891b2)' }}
          >
            <Wallet size={32} className="text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Pay<span style={{ color: '#22d3ee' }}>Vault</span>
          </h1>
          <p className="text-slate-400 mt-2 text-sm">Sign in to your account</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="glass-card p-8 space-y-6">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Username
            </label>
            <input
              id="login-username"
              type="text"
              className="pv-input"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              disabled={loading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Password
            </label>
            <div className="relative">
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                className="pv-input"
                style={{ paddingRight: '48px' }}
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                disabled={loading}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors"
                style={{ background: 'none', border: 'none', cursor: 'pointer' }}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            id="login-submit"
            type="submit"
            className="btn-primary w-full"
            disabled={loading}
          >
            {loading ? (
              <>
                <div className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                Signing in...
              </>
            ) : (
              <>
                <LogIn size={18} />
                Sign In
              </>
            )}
          </button>

          <p className="text-center text-sm text-slate-400">
            Don't have an account?{' '}
            <Link
              to="/signup"
              className="font-medium no-underline"
              style={{ color: '#22d3ee' }}
            >
              Create one
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
