import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import api from '../api/client.js'
import { validateEmail, validatePhone, formatDate, getErrorMessage } from '../utils/helpers.js'
import Layout from '../components/Layout.jsx'
import { User, Save, Mail, Phone, Calendar } from 'lucide-react'

export default function Profile() {
  const { userId } = useAuth()
  const toast = useToast()
  const [profile, setProfile] = useState(null)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState(false)

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await api.get(`/api/users/${userId}`)
        setProfile(res.data)
        setName(res.data.name || '')
        setEmail(res.data.email || '')
        setPhone(res.data.phone || '')
      } catch (err) {
        toast.error(getErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }
    if (userId) fetchProfile()
  }, [userId])

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

  const handleSave = async (e) => {
    e.preventDefault()
    if (!validate()) return

    setSaving(true)
    try {
      const res = await api.put(`/api/users/${userId}`, {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
      })
      setProfile(res.data)
      setEditing(false)
      toast.success('Profile updated successfully!')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleCancel = () => {
    setName(profile?.name || '')
    setEmail(profile?.email || '')
    setPhone(profile?.phone || '')
    setEditing(false)
  }

  if (loading) {
    return (
      <Layout>
        <div className="loading-screen">
          <div className="spinner" style={{ width: 40, height: 40 }} />
          <p className="text-slate-400">Loading profile...</p>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white flex items-center gap-3">
            <User size={28} style={{ color: '#22d3ee' }} />
            My Profile
          </h1>
          <p className="text-slate-400 mt-1">Manage your personal information</p>
        </div>

        {/* Avatar Card */}
        <div className="glass-card p-6 flex items-center gap-5">
          <div
            className="w-16 h-16 rounded-2xl flex items-center justify-center flex-shrink-0"
            style={{ background: 'linear-gradient(135deg, #06b6d4, #0891b2)' }}
          >
            <span className="text-2xl font-bold text-white">
              {profile?.name?.charAt(0)?.toUpperCase() || '?'}
            </span>
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">{profile?.name}</h2>
            <p className="text-sm text-slate-400">User ID: #{userId}</p>
            <div className="flex items-center gap-1 mt-1 text-xs text-slate-500">
              <Calendar size={12} />
              Member since {formatDate(profile?.createdAt)}
            </div>
          </div>
        </div>

        {/* Profile Form */}
        <form onSubmit={handleSave} className="glass-card p-6 space-y-5">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-lg font-semibold text-white">Personal Information</h3>
            {!editing && (
              <button
                type="button"
                onClick={() => setEditing(true)}
                className="btn-secondary text-sm"
                style={{ padding: '8px 16px' }}
              >
                Edit
              </button>
            )}
          </div>

          <div>
            <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
              <User size={14} />
              Full Name
            </label>
            <input
              id="edit-name"
              type="text"
              className="pv-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={!editing || saving}
            />
          </div>

          <div>
            <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
              <Mail size={14} />
              Email Address
            </label>
            <input
              id="edit-email"
              type="email"
              className="pv-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={!editing || saving}
            />
          </div>

          <div>
            <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
              <Phone size={14} />
              Phone Number
            </label>
            <input
              id="edit-phone"
              type="tel"
              className="pv-input"
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
              disabled={!editing || saving}
            />
            {editing && (
              <p className="text-xs text-slate-500 mt-1">10-digit Indian number starting with 6-9</p>
            )}
          </div>

          {editing && (
            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={handleCancel}
                className="btn-secondary flex-1"
                disabled={saving}
              >
                Cancel
              </button>
              <button
                id="edit-save"
                type="submit"
                className="btn-primary flex-1"
                disabled={saving}
              >
                {saving ? (
                  <>
                    <div className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save size={16} />
                    Save Changes
                  </>
                )}
              </button>
            </div>
          )}
        </form>
      </div>
    </Layout>
  )
}
