import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import api from '../api/client.js'
import { formatINR, getErrorMessage } from '../utils/helpers.js'
import Layout from '../components/Layout.jsx'
import {
  Send,
  Wallet,
  AlertTriangle,
  CheckCircle,
  User,
  Users,
  Check,
  Sparkles,
  RefreshCw,
  ShieldCheck,
  ArrowUpRight,
} from 'lucide-react'

export default function SendMoney() {
  const { userId, username } = useAuth()
  const toast = useToast()

  const [wallet, setWallet] = useState(null)
  const [currentUser, setCurrentUser] = useState(null)
  const [recipients, setRecipients] = useState([])
  const [selectedRecipientId, setSelectedRecipientId] = useState('')
  const [amount, setAmount] = useState('')

  const [loading, setLoading] = useState(false)
  const [fetchingData, setFetchingData] = useState(true)
  const [showConfirm, setShowConfirm] = useState(false)
  const [result, setResult] = useState(null)

  // Fetch wallet, all users, and current user profile
  const loadTransferData = useCallback(async () => {
    if (!userId) return
    setFetchingData(true)
    try {
      const [walletRes, usersRes, currentUserRes] = await Promise.all([
        api.get(`/api/wallets/user/${userId}`),
        api.get('/api/users'),
        api.get(`/api/users/${userId}`).catch(() => null),
      ])

      setWallet(walletRes.data)

      const allUsers = Array.isArray(usersRes.data) ? usersRes.data : []
      // Exclude the currently logged-in user from the recipient list
      const availableRecipients = allUsers.filter(
        (u) => Number(u.id) !== Number(userId)
      )
      setRecipients(availableRecipients)

      // Set current user profile
      if (currentUserRes?.data) {
        setCurrentUser(currentUserRes.data)
      } else {
        const found = allUsers.find((u) => Number(u.id) === Number(userId))
        if (found) setCurrentUser(found)
      }

      // Automatically select the first recipient if none selected yet
      if (availableRecipients.length > 0) {
        setSelectedRecipientId((prev) =>
          prev && availableRecipients.some((r) => String(r.id) === prev)
            ? prev
            : String(availableRecipients[0].id)
        )
      }
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setFetchingData(false)
    }
  }, [userId, toast])

  useEffect(() => {
    loadTransferData()
  }, [loadTransferData])

  const selectedRecipient = recipients.find(
    (r) => String(r.id) === String(selectedRecipientId)
  )

  const validate = () => {
    const rid = Number(selectedRecipientId)
    if (!rid || rid <= 0) {
      toast.error('Please select a recipient to send money')
      return false
    }
    if (rid === Number(userId)) {
      toast.error('You cannot send money to yourself')
      return false
    }
    const amt = parseFloat(amount)
    if (!amt || isNaN(amt) || amt < 0.01) {
      toast.error('Amount must be at least ₹0.01')
      return false
    }
    if (wallet && amt > parseFloat(wallet.balance)) {
      toast.info('Warning: Amount exceeds your displayed balance')
    }
    return true
  }

  const handleReview = (e) => {
    e.preventDefault()
    if (!validate()) return
    setShowConfirm(true)
  }

  const handleTransfer = async () => {
    setLoading(true)
    setResult(null)
    try {
      const payload = {
        senderUserId: Number(userId),
        receiverUserId: Number(selectedRecipientId),
        amount: parseFloat(amount),
      }

      const res = await api.post('/api/transactions/transfer', payload)
      setResult({
        success: true,
        data: res.data,
        recipient: selectedRecipient,
      })
      toast.success('Transfer successful!')

      // Refresh wallet balance
      const wRes = await api.get(`/api/wallets/user/${userId}`)
      setWallet(wRes.data)
    } catch (err) {
      const errMsg = getErrorMessage(err)
      setResult({ success: false, message: errMsg })
      toast.error(errMsg)
    } finally {
      setLoading(false)
      setShowConfirm(false)
    }
  }

  const handleReset = () => {
    setAmount('')
    setResult(null)
    setShowConfirm(false)
  }

  const quickAmounts = [500, 1000, 2000, 5000]

  if (fetchingData) {
    return (
      <Layout>
        <div className="loading-screen">
          <div className="spinner" style={{ width: 40, height: 40 }} />
          <p className="text-slate-400 font-medium">Loading transfer details...</p>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="w-full space-y-8 animate-fade-in">
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-800/80">
          <div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white tracking-tight flex items-center gap-3">
              <span
                className="w-11 h-11 sm:w-12 sm:h-12 rounded-2xl flex items-center justify-center"
                style={{ background: 'rgba(6,182,212,0.12)' }}
              >
                <Send size={24} style={{ color: '#22d3ee' }} />
              </span>
              Send Money
            </h1>
            <p className="text-slate-400 text-sm sm:text-base mt-1.5">
              Direct wallet-to-wallet transfer across registered PayVault accounts
            </p>
          </div>
          <button
            onClick={loadTransferData}
            className="btn-secondary self-start sm:self-center text-xs sm:text-sm py-2.5 px-4 rounded-xl flex items-center gap-2"
            title="Refresh balance and users"
          >
            <RefreshCw size={15} />
            Refresh
          </button>
        </div>

        {/* Transfer Result Banner */}
        {result && (
          <div
            className="glass-card p-6 sm:p-8 text-center animate-slide-up max-w-xl mx-auto"
            style={{
              borderColor: result.success
                ? 'rgba(16,185,129,0.35)'
                : 'rgba(244,63,94,0.35)',
            }}
          >
            {result.success ? (
              <>
                <div
                  className="w-16 h-16 rounded-2xl mx-auto mb-4 flex items-center justify-center"
                  style={{ background: 'rgba(16,185,129,0.15)' }}
                >
                  <CheckCircle size={36} style={{ color: '#34d399' }} />
                </div>
                <h3 className="text-2xl font-bold text-white mb-2">
                  Transfer Successful!
                </h3>
                <p className="text-slate-300 text-sm sm:text-base mt-1">
                  <span className="font-bold text-white">
                    {formatINR(result.data.amount)}
                  </span>{' '}
                  sent to{' '}
                  <span className="font-semibold text-cyan-400">
                    {result.recipient?.name || `User #${result.data.receiverUserId}`}
                  </span>{' '}
                  (User ID: #{result.data.receiverUserId})
                </p>
                <div
                  className="inline-block mt-4 px-4 py-2 rounded-xl text-xs font-mono text-slate-400"
                  style={{ background: 'rgba(10,15,30,0.7)', border: '1px solid rgba(100,116,139,0.2)' }}
                >
                  Transaction Reference #{result.data.id} • Status: COMPLETED
                </div>
                <div className="mt-6">
                  <button onClick={handleReset} className="btn-primary px-8 py-3 text-sm font-bold">
                    Send Another Payment
                  </button>
                </div>
              </>
            ) : (
              <>
                <div
                  className="w-16 h-16 rounded-2xl mx-auto mb-4 flex items-center justify-center"
                  style={{ background: 'rgba(244,63,94,0.15)' }}
                >
                  <AlertTriangle size={36} style={{ color: '#fb7185' }} />
                </div>
                <h3 className="text-2xl font-bold text-white mb-2">
                  Transfer Failed
                </h3>
                <p className="text-sm mt-1" style={{ color: '#fb7185' }}>
                  {result.message}
                </p>
                <div className="mt-6">
                  <button onClick={handleReset} className="btn-secondary px-8 py-3 text-sm font-bold">
                    Try Again
                  </button>
                </div>
              </>
            )}
          </div>
        )}

        {/* Main Content Layout (Wide Two-Column Grid on Desktop) */}
        {!result && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 xl:gap-10 items-start">
            {/* Left Column (~42%): Available Balance & Sender Account */}
            <div className="lg:col-span-5 space-y-6">
              {/* Available Balance Card (Prominent & Spacious) */}
              <div
                className="p-7 sm:p-8 rounded-2xl relative overflow-hidden"
                style={{
                  background: 'linear-gradient(135deg, rgba(13,21,39,0.95), rgba(17,24,39,0.95))',
                  border: '1px solid rgba(6,182,212,0.3)',
                  boxShadow: '0 8px 32px rgba(6,182,212,0.09)',
                }}
              >
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2 text-cyan-400 text-xs sm:text-sm font-semibold uppercase tracking-wider">
                    <Wallet size={18} />
                    Available Balance
                  </div>
                  <span className="badge badge-success text-xs py-1 px-3">
                    {wallet?.currency || 'INR'} • ACTIVE
                  </span>
                </div>

                <div className="mt-2">
                  <div className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight">
                    {formatINR(wallet?.balance || 0)}
                  </div>
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-slate-800 text-xs sm:text-sm text-slate-400">
                    <span>Wallet ID: #{wallet?.id || '—'}</span>
                    <span className="text-emerald-400 font-medium flex items-center gap-1.5">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                      Ready for transfer
                    </span>
                  </div>
                </div>
              </div>

              {/* From / Sender Section (Clean & Wide) */}
              <div
                className="p-6 sm:p-7 rounded-2xl space-y-4"
                style={{
                  background: 'rgba(17,24,39,0.6)',
                  border: '1px solid rgba(100,116,139,0.18)',
                }}
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs sm:text-sm uppercase tracking-wider font-semibold text-slate-300 flex items-center gap-1.5">
                    <User size={15} style={{ color: '#22d3ee' }} />
                    From (Sender)
                  </span>
                  <span
                    className="px-3 py-1 rounded-full text-xs font-semibold flex items-center gap-1.5"
                    style={{
                      background: 'rgba(6,182,212,0.12)',
                      color: '#22d3ee',
                      border: '1px solid rgba(6,182,212,0.25)',
                    }}
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                    Logged-in account
                  </span>
                </div>

                <div
                  className="p-4 sm:p-4.5 rounded-xl flex items-center justify-between"
                  style={{
                    background: 'rgba(10,15,30,0.65)',
                    border: '1px solid rgba(100,116,139,0.15)',
                  }}
                >
                  <div className="flex items-center gap-3.5">
                    <div
                      className="w-12 h-12 rounded-xl flex items-center justify-center font-bold text-lg text-white flex-shrink-0"
                      style={{
                        background: 'linear-gradient(135deg, #06b6d4, #0891b2)',
                      }}
                    >
                      {currentUser?.name
                        ? currentUser.name.charAt(0).toUpperCase()
                        : (username || 'U').charAt(0).toUpperCase()}
                    </div>
                    <div className="overflow-hidden">
                      <p className="text-base font-semibold text-white truncate">
                        {currentUser?.name || username || `User #${userId}`}
                      </p>
                      <p className="text-xs sm:text-sm text-slate-400 truncate">
                        {currentUser?.email || 'Registered PayVault User'}
                      </p>
                    </div>
                  </div>

                  <div className="text-right pl-2 flex-shrink-0">
                    <span
                      className="inline-block px-3 py-1.5 rounded-lg text-xs font-bold font-mono"
                      style={{
                        background: 'rgba(6,182,212,0.15)',
                        color: '#22d3ee',
                        border: '1px solid rgba(6,182,212,0.25)',
                      }}
                    >
                      User ID: {userId}
                    </span>
                  </div>
                </div>
              </div>

              {/* Security & Verification Note */}
              <div
                className="p-4 sm:p-4.5 rounded-xl flex items-center gap-3 text-xs sm:text-sm text-slate-400 hidden sm:flex"
                style={{
                  background: 'rgba(10,15,30,0.4)',
                  border: '1px solid rgba(100,116,139,0.12)',
                }}
              >
                <ShieldCheck size={18} className="text-cyan-400 flex-shrink-0" />
                <span>
                  Transfers are secured with JWT authentication and atomic balance validation.
                </span>
              </div>
            </div>

            {/* Right Column (~58%): Transfer Form */}
            <div className="lg:col-span-7">
              <form onSubmit={handleReview} className="space-y-6">
                {/* To / Recipient Section */}
                <div
                  className="p-6 sm:p-8 rounded-2xl space-y-5"
                  style={{
                    background: 'rgba(17,24,39,0.7)',
                    border: '1px solid rgba(100,116,139,0.18)',
                  }}
                >
                  <div className="flex items-center justify-between">
                    <label
                      htmlFor="send-receiver-id"
                      className="text-xs sm:text-sm uppercase tracking-wider font-semibold text-slate-300 flex items-center gap-1.5 cursor-pointer"
                    >
                      <Users size={16} style={{ color: '#22d3ee' }} />
                      To (Recipient)
                    </label>
                    <span className="text-xs sm:text-sm text-slate-400">
                      {recipients.length} user{recipients.length === 1 ? '' : 's'} available
                    </span>
                  </div>

                  {recipients.length === 0 ? (
                    <div
                      className="p-8 text-center rounded-xl"
                      style={{ background: 'rgba(10,15,30,0.5)' }}
                    >
                      <Users size={36} className="mx-auto mb-2 text-slate-500" />
                      <p className="text-slate-300 font-medium text-sm sm:text-base">
                        No other users found
                      </p>
                      <p className="text-xs sm:text-sm text-slate-500 mt-1">
                        Other registered PayVault accounts will appear here automatically.
                      </p>
                    </div>
                  ) : (
                    <>
                      {/* Recipient Dropdown Selector */}
                      <div>
                        <select
                          id="send-receiver-id"
                          name="receiverUserId"
                          className="pv-input cursor-pointer text-sm sm:text-base py-3"
                          value={selectedRecipientId}
                          onChange={(e) => setSelectedRecipientId(e.target.value)}
                          disabled={loading}
                        >
                          {recipients.map((r) => (
                            <option
                              key={r.id}
                              value={r.id}
                              style={{ background: '#0a0f1e', color: '#fff' }}
                            >
                              {r.name} — {r.email} (User ID: {r.id})
                            </option>
                          ))}
                        </select>
                      </div>

                      {/* Recipient Cards (Clean 2-Column Responsive Grid) */}
                      <div className="space-y-2.5">
                        <div className="flex items-center justify-between text-xs sm:text-sm text-slate-400 font-medium">
                          <span>Quick Select Recipient:</span>
                          <span className="text-xs text-slate-500">Click card to select</span>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                          {recipients.map((r) => {
                            const isSelected =
                              String(r.id) === String(selectedRecipientId)
                            return (
                              <div
                                key={r.id}
                                onClick={() => setSelectedRecipientId(String(r.id))}
                                className="p-4 rounded-xl cursor-pointer transition-all flex items-center justify-between"
                                style={{
                                  background: isSelected
                                    ? 'rgba(6,182,212,0.1)'
                                    : 'rgba(10,15,30,0.55)',
                                  border: isSelected
                                    ? '1.5px solid #06b6d4'
                                    : '1px solid rgba(100,116,139,0.2)',
                                  boxShadow: isSelected
                                    ? '0 0 20px rgba(6,182,212,0.14)'
                                    : 'none',
                                }}
                              >
                                <div className="flex items-center gap-3.5 overflow-hidden">
                                  <div
                                    className="w-11 h-11 rounded-xl flex items-center justify-center font-bold text-sm sm:text-base text-white flex-shrink-0"
                                    style={{
                                      background: isSelected
                                        ? 'linear-gradient(135deg, #06b6d4, #0891b2)'
                                        : 'rgba(30,42,58,0.85)',
                                    }}
                                  >
                                    {r.name ? r.name.charAt(0).toUpperCase() : 'U'}
                                  </div>
                                  <div className="overflow-hidden">
                                    <p className="text-sm sm:text-base font-semibold text-white truncate">
                                      {r.name}
                                    </p>
                                    <p className="text-xs sm:text-sm text-slate-400 truncate">
                                      {r.email}
                                    </p>
                                  </div>
                                </div>

                                <div className="flex items-center gap-2.5 pl-2 flex-shrink-0">
                                  <span
                                    className="px-2.5 py-1 rounded-lg text-xs font-bold font-mono"
                                    style={{
                                      background: isSelected
                                        ? 'rgba(6,182,212,0.22)'
                                        : 'rgba(100,116,139,0.12)',
                                      color: isSelected ? '#22d3ee' : '#94a3b8',
                                      border: isSelected
                                        ? '1px solid rgba(6,182,212,0.3)'
                                        : '1px solid transparent',
                                    }}
                                  >
                                    User ID: {r.id}
                                  </span>
                                  {isSelected ? (
                                    <div
                                      className="w-5 h-5 rounded-full flex items-center justify-center"
                                      style={{ background: '#06b6d4' }}
                                    >
                                      <Check size={12} className="text-white" />
                                    </div>
                                  ) : (
                                    <div
                                      className="w-5 h-5 rounded-full border border-slate-700 flex items-center justify-center"
                                    />
                                  )}
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      </div>

                      {/* Active Selected Recipient Summary Pill */}
                      {selectedRecipient && (
                        <div
                          className="p-3.5 rounded-xl flex items-center justify-between text-xs sm:text-sm"
                          style={{
                            background: 'rgba(6,182,212,0.06)',
                            border: '1px dashed rgba(6,182,212,0.35)',
                          }}
                        >
                          <div className="flex items-center gap-2.5 overflow-hidden">
                            <Sparkles size={16} className="text-cyan-400 flex-shrink-0" />
                            <span className="text-slate-400">Selected:</span>
                            <span className="text-white font-semibold truncate">
                              {selectedRecipient.name}
                            </span>
                            <span className="text-slate-400 text-xs hidden sm:inline truncate">
                              ({selectedRecipient.email})
                            </span>
                          </div>
                          <span className="text-cyan-400 font-bold font-mono flex-shrink-0 pl-2">
                            User ID: {selectedRecipient.id}
                          </span>
                        </div>
                      )}
                    </>
                  )}
                </div>

                {/* Amount Section */}
                <div
                  className="p-6 sm:p-8 rounded-2xl space-y-5"
                  style={{
                    background: 'rgba(17,24,39,0.7)',
                    border: '1px solid rgba(100,116,139,0.18)',
                  }}
                >
                  <div className="flex items-center justify-between">
                    <label
                      htmlFor="send-amount"
                      className="text-xs sm:text-sm uppercase tracking-wider font-semibold text-slate-300"
                    >
                      Amount to Transfer
                    </label>
                    {wallet && (
                      <span className="text-xs sm:text-sm text-slate-400">
                        Available:{' '}
                        <span className="text-slate-200 font-medium">
                          {formatINR(wallet.balance)}
                        </span>
                      </span>
                    )}
                  </div>

                  <div className="relative">
                    <div className="absolute left-4 top-1/2 -translate-y-1/2 text-cyan-400 font-bold text-2xl select-none">
                      ₹
                    </div>
                    <input
                      id="send-amount"
                      type="number"
                      className="pv-input"
                      style={{
                        paddingLeft: '44px',
                        paddingRight: '16px',
                        paddingTop: '16px',
                        paddingBottom: '16px',
                        fontSize: '1.65rem',
                        fontWeight: '700',
                        letterSpacing: '-0.02em',
                      }}
                      placeholder="0.00"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      disabled={loading}
                      min="0.01"
                      step="0.01"
                    />
                  </div>

                  {/* Quick Amount Buttons */}
                  <div className="flex flex-wrap items-center gap-2.5 pt-1">
                    <span className="text-xs sm:text-sm text-slate-400 mr-1 font-medium">
                      Quick:
                    </span>
                    {quickAmounts.map((q) => (
                      <button
                        key={q}
                        type="button"
                        onClick={() => setAmount(String(q))}
                        className="px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer"
                        style={{
                          background:
                            amount === String(q)
                              ? 'rgba(6,182,212,0.22)'
                              : 'rgba(10,15,30,0.6)',
                          border:
                            amount === String(q)
                              ? '1px solid #06b6d4'
                              : '1px solid rgba(100,116,139,0.2)',
                          color: amount === String(q) ? '#22d3ee' : '#cbd5e1',
                        }}
                      >
                        ₹{q.toLocaleString('en-IN')}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Send Money Button (Prominent & Full-Width) */}
                <button
                  id="send-review"
                  type="submit"
                  className="btn-primary w-full py-4 sm:py-4.5 text-base sm:text-lg font-bold rounded-xl shadow-xl"
                  disabled={loading || recipients.length === 0}
                  style={{
                    letterSpacing: '0.01em',
                  }}
                >
                  <Send size={20} />
                  Send Money
                </button>
              </form>
            </div>
          </div>
        )}

        {/* Confirmation Modal */}
        {showConfirm && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center px-4"
            style={{
              background: 'rgba(3,7,18,0.88)',
              backdropFilter: 'blur(12px)',
            }}
          >
            <div
              className="glass-card p-6 sm:p-8 w-full max-w-lg animate-slide-up text-center"
              style={{ borderColor: 'rgba(6,182,212,0.4)', background: '#0b1120' }}
            >
              <div
                className="w-14 h-14 rounded-2xl mx-auto mb-4 flex items-center justify-center"
                style={{ background: 'rgba(6,182,212,0.15)' }}
              >
                <ArrowUpRight size={28} style={{ color: '#22d3ee' }} />
              </div>
              <h3 className="text-2xl font-bold text-white mb-1.5">
                Confirm Transfer
              </h3>
              <p className="text-xs sm:text-sm text-slate-400 mb-6">
                Please verify the transaction details before transferring funds.
              </p>

              <div
                className="space-y-3.5 mb-6 text-left p-4 sm:p-5 rounded-xl"
                style={{
                  background: 'rgba(15,23,42,0.7)',
                  border: '1px solid rgba(100,116,139,0.2)',
                }}
              >
                <div className="flex justify-between items-start text-sm sm:text-base pb-3 border-b border-slate-800">
                  <span className="text-slate-400">From</span>
                  <div className="text-right">
                    <p className="text-white font-semibold">
                      {currentUser?.name || username || `User #${userId}`}
                    </p>
                    <span className="text-xs sm:text-sm text-cyan-400 font-mono">
                      User ID: {userId}
                    </span>
                  </div>
                </div>

                <div className="flex justify-between items-start text-sm sm:text-base pb-3 border-b border-slate-800">
                  <span className="text-slate-400">To</span>
                  <div className="text-right">
                    <p className="text-white font-semibold">
                      {selectedRecipient?.name}
                    </p>
                    <p className="text-xs sm:text-sm text-slate-400">
                      {selectedRecipient?.email}
                    </p>
                    <span className="text-xs sm:text-sm text-cyan-400 font-semibold font-mono">
                      User ID: {selectedRecipient?.id}
                    </span>
                  </div>
                </div>

                <div className="flex justify-between items-center text-sm sm:text-base pt-1">
                  <span className="text-slate-400 font-medium">
                    Transfer Amount
                  </span>
                  <span className="text-2xl sm:text-3xl font-extrabold text-emerald-400">
                    {formatINR(parseFloat(amount))}
                  </span>
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowConfirm(false)}
                  className="btn-secondary flex-1 py-3.5 text-sm font-semibold"
                  disabled={loading}
                >
                  Cancel
                </button>
                <button
                  id="send-confirm"
                  type="button"
                  onClick={handleTransfer}
                  className="btn-primary flex-1 py-3.5 text-sm font-bold"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <div
                        className="spinner"
                        style={{ width: 18, height: 18, borderWidth: 2 }}
                      />
                      Processing...
                    </>
                  ) : (
                    <>
                      <Send size={16} />
                      Confirm & Send
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}


