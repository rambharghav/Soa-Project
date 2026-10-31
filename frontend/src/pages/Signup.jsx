import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import { Wallet, Eye, EyeOff, UserPlus } from 'lucide-react'

export default function Signup() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const { signup, loading } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const validate = () => {
    if (!username.trim()) {
      toast.error('Username is required')
      return false
    }
    if (username.trim().length < 3 || username.trim().length > 50) {
      toast.error('Username must be between 3 and 50 characters')
      return false
    }
    if (!password) {
      toast.error('Password is required')
      return false
    }
    if (password.length < 6) {
      toast.error('Password must be at least 6 characters')
      return false
    }
    if (password !== confirmPassword) {
      toast.error('Passwords do not match')
      return false
    }
    return true
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return

    const result = await signup(username.trim(), password)

    if (result.success) {
      toast.success('Account created! Please sign in.')
      navigate('/login')
    } else {
      toast.error(result.message)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4"
      style={{
        background: 'radial-gradient(ellipse at top, rgba(16,185,129,0.06) 0%, transparent 50%), #030712',
      }}
    >
      <div className="w-full max-w-md animate-fade-in">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 rounded-2xl mx-auto mb-4 flex items-center justify-center"
            style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}
          >
            <Wallet size={32} className="text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Create Account
          </h1>
          <p className="text-slate-400 mt-2 text-sm">Join PayVault today</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="glass-card p-8 space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Username
            </label>
            <input
              id="signup-username"
              type="text"
              className="pv-input"
              placeholder="Choose a username (3-50 chars)"
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
                id="signup-password"
                type={showPassword ? 'text' : 'password'}
                className="pv-input"
                style={{ paddingRight: '48px' }}
                placeholder="Min 6 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
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

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Confirm Password
            </label>
            <input
              id="signup-confirm-password"
              type="password"
              className="pv-input"
              placeholder="Re-enter password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
              disabled={loading}
            />
          </div>

          <button
            id="signup-submit"
            type="submit"
            className="btn-primary w-full"
            style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}
            disabled={loading}
          >
            {loading ? (
              <>
                <div className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                Creating account...
              </>
            ) : (
              <>
                <UserPlus size={18} />
                Create Account
              </>
            )}
          </button>

          <p className="text-center text-sm text-slate-400">
            Already have an account?{' '}
            <Link
              to="/login"
              className="font-medium no-underline"
              style={{ color: '#22d3ee' }}
            >
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
