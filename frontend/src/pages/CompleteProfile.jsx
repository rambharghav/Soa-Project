import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import { validateEmail, validatePhone } from '../utils/helpers.js'
import { UserCheck, Sparkles } from 'lucide-react'

export default function CompleteProfile() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const { completeProfile, loading, username } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const validate = () => {
    if (!name.trim() || name.trim().length < 2) {
      toast.error('Name must be at least 2 characters')
      return false
    }
    if (name.trim().length > 100) {
      toast.error('Name must be at most 100 characters')
      return false
    }
    if (!email.trim() || !validateEmail(email.trim())) {
      toast.error('Enter a valid email address')
      return false
    }
    if (!phone.trim() || !validatePhone(phone.trim())) {
      toast.error('Enter a valid 10-digit Indian phone number (starting with 6-9)')
      return false
    }
    return true
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return

    const result = await completeProfile(name.trim(), email.trim(), phone.trim())

    if (result.success) {
      toast.success('Profile created & wallet activated!')
      navigate('/dashboard')
    } else {
      toast.error(result.message)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4"
      style={{
        background: 'radial-gradient(ellipse at top, rgba(251,191,36,0.06) 0%, transparent 50%), #030712',
      }}
    >
      <div className="w-full max-w-lg animate-fade-in">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 rounded-2xl mx-auto mb-4 flex items-center justify-center"
            style={{ background: 'linear-gradient(135deg, #f59e0b, #d97706)' }}
          >
            <Sparkles size={32} className="text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Complete Your Profile
          </h1>
          <p className="text-slate-400 mt-2 text-sm">
            Welcome, <span style={{ color: '#fbbf24' }}>{username}</span>! Set up your profile to start using PayVault.
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="glass-card p-8 space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Full Name
            </label>
            <input
              id="profile-name"
              type="text"
              className="pv-input"
              placeholder="Enter your full name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={loading}
            />
            <p className="text-xs text-slate-500 mt-1">2-100 characters</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Email Address
            </label>
            <input
              id="profile-email"
              type="email"
              className="pv-input"
              placeholder="your@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Phone Number
            </label>
            <input
              id="profile-phone"
              type="tel"
              className="pv-input"
              placeholder="10-digit Indian mobile number"
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
              disabled={loading}
            />
            <p className="text-xs text-slate-500 mt-1">Must start with 6, 7, 8, or 9</p>
          </div>

          <button
            id="profile-submit"
            type="submit"
            className="btn-primary w-full"
            style={{ background: 'linear-gradient(135deg, #f59e0b, #d97706)' }}
            disabled={loading}
          >
            {loading ? (
              <>
                <div className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                Setting up...
              </>
            ) : (
              <>
                <UserCheck size={18} />
                Create Profile & Wallet
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  )
}
