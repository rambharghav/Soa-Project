import { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import api from '../api/client.js'
import { formatINR, formatDateTime, getErrorMessage } from '../utils/helpers.js'
import Layout from '../components/Layout.jsx'
import {
  BookOpen,
  ArrowUpRight,
  ArrowDownLeft,
  Search,
  Filter,
  Download,
  RefreshCw,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  Copy,
  Check,
  CheckCircle2,
  Receipt,
  Scale,
  TrendingDown,
  TrendingUp,
  Wallet,
  ArrowLeft,
} from 'lucide-react'

export default function Ledger() {
  const { userId, username, hasProfile } = useAuth()
  const toast = useToast()
  const [entries, setEntries] = useState([])
  const [wallet, setWallet] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [serviceUnavailable, setServiceUnavailable] = useState(false)
  const [unavailableMessage, setUnavailableMessage] = useState('')
  const [authDenied, setAuthDenied] = useState(false)
  const [deniedMessage, setDeniedMessage] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all') // all, credits, debits, posted, reversed
  const [copiedId, setCopiedId] = useState(null)

  const inFlightRef = useRef(false)
  const lastToastMsgRef = useRef(null)

  const showToastOnce = (msg, isError = true) => {
    if (!msg) return
    if (lastToastMsgRef.current === msg) return
    lastToastMsgRef.current = msg
    if (isError) {
      toast.error(msg)
    } else {
      toast.success(msg)
    }
    setTimeout(() => {
      lastToastMsgRef.current = null
    }, 4000)
  }

  const fetchLedgerData = async (isManualRefresh = false) => {
    if (!userId) {
      setLoading(false)
      return
    }

    if (inFlightRef.current && !isManualRefresh) {
      return
    }

    inFlightRef.current = true
    if (isManualRefresh) setRefreshing(true)
    else setLoading(true)

    setServiceUnavailable(false)
    setUnavailableMessage('')
    setAuthDenied(false)
    setDeniedMessage('')

    try {
      const [ledgerRes, walletRes] = await Promise.all([
        api.get(`/api/wallets/user/${userId}/ledger`),
        api.get(`/api/wallets/user/${userId}`).catch(() => null),
      ])

      setEntries(Array.isArray(ledgerRes.data) ? ledgerRes.data : [])
      if (walletRes?.data) {
        setWallet(walletRes.data)
      }
      if (isManualRefresh) {
        showToastOnce('Ledger reconciled & updated', false)
      }
    } catch (err) {
      const status = err.response?.status
      const msg = getErrorMessage(err)

      if (status === 403) {
        setAuthDenied(true)
        setDeniedMessage(msg || `Access denied: You are not authorized to view the financial ledger for user id ${userId}`)
        showToastOnce(msg || `Access denied: You are not authorized to view user ${userId}'s ledger`)
      } else if (status === 503 || msg.toLowerCase().includes('unavailable') || msg.toLowerCase().includes('circuit')) {
        setServiceUnavailable(true)
        setUnavailableMessage(msg || 'Wallet Service is temporarily unavailable. Circuit breaker engaged.')
      } else {
        showToastOnce(msg)
      }
    } finally {
      inFlightRef.current = false
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    if (userId) {
      fetchLedgerData()
    } else {
      setLoading(false)
    }
  }, [userId])

  const copyToClipboard = (text, id) => {
    navigator.clipboard?.writeText(text)
    setCopiedId(id)
    setTimeout(() => setCopiedId(null), 2000)
    toast.success(`Copied reference: ${text}`)
  }

  const exportCSV = () => {
    if (entries.length === 0) {
      toast.error('No ledger entries to export')
      return
    }

    const headers = ['Entry ID', 'Transaction Ref', 'Timestamp', 'Description', 'Entry Type', 'Debit (INR)', 'Credit (INR)', 'Running Balance (INR)', 'Status']
    const rows = entries.map((e) => {
      const isDebit = e.entryType === 'DEBIT'
      return [
        e.id,
        `"${e.transactionRef}"`,
        `"${e.createdAt}"`,
        `"${(e.description || '').replace(/"/g, '""')}"`,
        e.entryType,
        isDebit ? e.amount : '',
        !isDebit ? e.amount : '',
        e.balanceAfter,
        e.status,
      ].join(',')
    })

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows].join('\n')
    const encodedUri = encodeURI(csvContent)
    const link = document.createElement('a')
    link.setAttribute('href', encodedUri)
    link.setAttribute('download', `PayVault_Ledger_User_${userId}_${new Date().toISOString().slice(0, 10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    toast.success('Ledger statement exported as CSV')
  }

  // Summary Metrics calculation
  const totalCredits = entries
    .filter((e) => e.entryType === 'CREDIT' && e.status === 'POSTED')
    .reduce((sum, e) => sum + Number(e.amount || 0), 0)

  const totalDebits = entries
    .filter((e) => e.entryType === 'DEBIT' && e.status === 'POSTED')
    .reduce((sum, e) => sum + Number(e.amount || 0), 0)

  const netMovement = totalCredits - totalDebits
  const currentRunningBalance = entries.length > 0 ? entries[0].balanceAfter : wallet?.balance || 0

  // Filtered entries
  const filteredEntries = entries.filter((e) => {
    if (filter === 'credits' && e.entryType !== 'CREDIT') return false
    if (filter === 'debits' && e.entryType !== 'DEBIT') return false
    if (filter === 'posted' && e.status !== 'POSTED') return false
    if (filter === 'reversed' && e.status !== 'REVERSED') return false

    if (search.trim()) {
      const term = search.toLowerCase()
      const refMatch = (e.transactionRef || '').toLowerCase().includes(term)
      const descMatch = (e.description || '').toLowerCase().includes(term)
      const amountMatch = String(e.amount).includes(term)
      const balMatch = String(e.balanceAfter).includes(term)
      return refMatch || descMatch || amountMatch || balMatch
    }
    return true
  })

  const filterButtons = [
    { key: 'all', label: 'All Entries' },
    { key: 'credits', label: 'Credits Only (+)' },
    { key: 'debits', label: 'Debits Only (−)' },
    { key: 'posted', label: 'Posted' },
    { key: 'reversed', label: 'Reversed' },
  ]

  if (loading) {
    return (
      <Layout>
        <div className="loading-screen">
          <div className="spinner" style={{ width: 42, height: 42, borderWidth: 3 }} />
          <p className="text-slate-400 font-medium animate-pulse">Loading immutable financial ledger...</p>
        </div>
      </Layout>
    )
  }

  if (!hasProfile || !userId) {
    return (
      <Layout>
        <div className="max-w-xl mx-auto my-12 glass-card p-8 text-center space-y-4">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
            <AlertTriangle size={28} />
          </div>
          <h2 className="text-xl font-bold text-white">Profile Required</h2>
          <p className="text-sm text-slate-400">
            You must complete your profile onboarding before accessing your financial ledger.
          </p>
          <div className="pt-2">
            <Link to="/complete-profile" className="btn-primary no-underline text-sm py-2 px-6">
              Complete Profile
            </Link>
          </div>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="space-y-6 animate-fade-in">
        {/* Header section */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <div
                className="w-10 h-10 rounded-xl flex items-center justify-center shadow-lg"
                style={{ background: 'linear-gradient(135deg, #06b6d4, #0891b2)' }}
              >
                <BookOpen size={22} className="text-white" />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight flex items-center gap-2">
                  Financial Ledger
                  <span className="badge badge-success text-[10px] py-0.5 px-2">Audited</span>
                </h1>
                <p className="text-slate-400 text-sm mt-0.5">
                  Chronological double-entry records with guaranteed running balances & tamper-proof audit trails.
                </p>
              </div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2.5">
            <button
              onClick={() => fetchLedgerData(true)}
              disabled={refreshing || authDenied}
              className="btn-secondary text-xs sm:text-sm py-2 px-3.5 flex items-center gap-2"
              title="Refresh Ledger"
            >
              <RefreshCw size={15} className={refreshing ? 'animate-spin' : ''} />
              <span>{refreshing ? 'Syncing...' : 'Reconcile'}</span>
            </button>
            <button
              onClick={exportCSV}
              disabled={entries.length === 0}
              className="btn-primary text-xs sm:text-sm py-2 px-3.5 flex items-center gap-2"
              title="Download Statement"
            >
              <Download size={15} />
              <span>Export CSV</span>
            </button>
          </div>
        </div>

        {/* Authorization Denied Banner */}
        {authDenied && (
          <div
            className="p-5 rounded-2xl border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 animate-slide-up"
            style={{
              background: 'linear-gradient(135deg, rgba(244,63,94,0.15), rgba(159,18,57,0.15))',
              borderColor: 'rgba(244,63,94,0.35)',
            }}
          >
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-rose-500/20 border border-rose-500/30 flex items-center justify-center flex-shrink-0 text-rose-400 mt-0.5">
                <ShieldAlert size={22} />
              </div>
              <div>
                <p className="text-sm font-semibold text-rose-200">
                  Ledger Access Restricted
                </p>
                <p className="text-xs text-rose-300/80 mt-1">
                  {deniedMessage}
                </p>
                <p className="text-xs text-slate-400 mt-1.5">
                  Authenticated session: <span className="text-white font-mono font-medium">@{username}</span>. Each user may only access their verified personal financial ledger.
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Link
                to="/dashboard"
                className="btn-secondary text-xs py-2 px-3.5 flex items-center gap-1.5 no-underline"
              >
                <ArrowLeft size={13} />
                Dashboard
              </Link>
            </div>
          </div>
        )}

        {/* Resilience / Circuit Breaker Banner */}
        {serviceUnavailable && !authDenied && (
          <div
            className="p-4 rounded-xl border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-slide-up"
            style={{
              background: 'rgba(244,63,94,0.1)',
              borderColor: 'rgba(244,63,94,0.3)',
            }}
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-rose-500/20 flex items-center justify-center flex-shrink-0 text-rose-400">
                <AlertTriangle size={20} />
              </div>
              <div>
                <p className="text-sm font-semibold text-rose-200">
                  Wallet Service Temporarily Unavailable
                </p>
                <p className="text-xs text-rose-300/80 mt-0.5">
                  {unavailableMessage} Circuit breaker engaged to protect data integrity.
                </p>
              </div>
            </div>
            <button
              onClick={() => fetchLedgerData(true)}
              className="px-3.5 py-1.5 rounded-lg text-xs font-semibold text-white bg-rose-600 hover:bg-rose-500 transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              <RefreshCw size={13} />
              Retry Connection
            </button>
          </div>
        )}

        {/* KPI / Metric Cards (shown when not denied) */}
        {!authDenied && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Current Running Balance */}
            <div className="glass-card p-5 relative overflow-hidden">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Wallet size={14} style={{ color: '#22d3ee' }} />
                  Reconciled Balance
                </span>
                <span className="badge badge-success text-[10px]">Verified</span>
              </div>
              <p className="text-2xl sm:text-3xl font-bold text-white tracking-tight mt-1">
                {formatINR(currentRunningBalance)}
              </p>
              <p className="text-[11px] text-slate-500 mt-2 flex items-center gap-1">
                <ShieldCheck size={12} className="text-emerald-400" /> Matches current wallet state
              </p>
            </div>

            {/* Total Inflow / Credits */}
            <div className="glass-card p-5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <TrendingUp size={14} className="text-emerald-400" />
                  Total Credits
                </span>
                <span className="text-xs font-semibold text-emerald-400">Inflow</span>
              </div>
              <p className="text-2xl sm:text-3xl font-bold text-emerald-400 tracking-tight mt-1">
                +{formatINR(totalCredits)}
              </p>
              <p className="text-[11px] text-slate-500 mt-2">
                {entries.filter((e) => e.entryType === 'CREDIT').length} credit postings
              </p>
            </div>

            {/* Total Outflow / Debits */}
            <div className="glass-card p-5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <TrendingDown size={14} className="text-rose-400" />
                  Total Debits
                </span>
                <span className="text-xs font-semibold text-rose-400">Outflow</span>
              </div>
              <p className="text-2xl sm:text-3xl font-bold text-rose-400 tracking-tight mt-1">
                −{formatINR(totalDebits)}
              </p>
              <p className="text-[11px] text-slate-500 mt-2">
                {entries.filter((e) => e.entryType === 'DEBIT').length} debit postings
              </p>
            </div>

            {/* Net Movement */}
            <div className="glass-card p-5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Scale size={14} style={{ color: '#22d3ee' }} />
                  Net Movement
                </span>
                <span className="text-xs text-slate-400">Accounting</span>
              </div>
              <p
                className="text-2xl sm:text-3xl font-bold tracking-tight mt-1"
                style={{ color: netMovement >= 0 ? '#34d399' : '#fb7185' }}
              >
                {netMovement >= 0 ? '+' : ''}{formatINR(netMovement)}
              </p>
              <p className="text-[11px] text-slate-500 mt-2">
                Credits vs Debits balance
              </p>
            </div>
          </div>
        )}

        {/* Filter and Search Bar (shown when not denied) */}
        {!authDenied && (
          <div className="glass-card p-4">
            <div className="flex flex-col lg:flex-row gap-4 items-stretch lg:items-center justify-between">
              {/* Search */}
              <div className="relative flex-1">
                <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  id="ledger-search"
                  type="text"
                  className="pv-input text-sm"
                  style={{ paddingLeft: '40px' }}
                  placeholder="Search reference (TX-..., INIT-...), description, or amount..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>

              {/* Quick Filter Buttons */}
              <div className="flex items-center gap-1.5 flex-wrap">
                <Filter size={15} className="text-slate-500 mr-1" />
                {filterButtons.map((f) => (
                  <button
                    key={f.key}
                    onClick={() => setFilter(f.key)}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer"
                    style={{
                      background: filter === f.key ? 'rgba(6,182,212,0.15)' : 'transparent',
                      color: filter === f.key ? '#22d3ee' : '#94a3b8',
                      border: `1px solid ${filter === f.key ? 'rgba(6,182,212,0.3)' : 'rgba(100,116,139,0.2)'}`,
                    }}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Ledger Entries Table */}
        {!authDenied && (
          <div className="glass-card overflow-hidden">
            <div className="p-4 sm:p-5 border-b border-slate-700/40 flex items-center justify-between">
              <div>
                <h2 className="text-base font-semibold text-white flex items-center gap-2">
                  <Receipt size={18} style={{ color: '#22d3ee' }} />
                  Ledger Journal Entries
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  Showing {filteredEntries.length} of {entries.length} posted records
                </p>
              </div>
              <span className="text-xs text-slate-400 hidden sm:inline">
                Account: <span className="font-mono text-slate-200">#WA-{wallet?.id || userId}</span> (User #{userId} • @{username})
              </span>
            </div>

            {filteredEntries.length === 0 ? (
              <div className="p-12 text-center">
                <BookOpen size={48} className="mx-auto mb-3" style={{ color: '#3d4f63' }} />
                <p className="text-base text-slate-300 font-medium">
                  {entries.length === 0
                    ? 'No Ledger Entries Yet'
                    : 'No matching entries found'}
                </p>
                <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                  {entries.length === 0
                    ? 'Your financial ledger has no posted records yet. Once money is credited, debited, or transferred, real-time records with running balances will appear here.'
                    : 'Try clearing your search query or adjusting your active filters.'}
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead>
                    <tr className="border-b border-slate-700/50 text-[11px] uppercase tracking-wider text-slate-400 bg-slate-900/40">
                      <th className="py-3.5 px-4 font-semibold">Reference</th>
                      <th className="py-3.5 px-4 font-semibold">Date & Time</th>
                      <th className="py-3.5 px-4 font-semibold">Description</th>
                      <th className="py-3.5 px-4 font-semibold text-right">Debit (−)</th>
                      <th className="py-3.5 px-4 font-semibold text-right">Credit (+)</th>
                      <th className="py-3.5 px-4 font-semibold text-center">Status</th>
                      <th className="py-3.5 px-4 font-semibold text-right">Running Balance</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {filteredEntries.map((e) => {
                      const isDebit = e.entryType === 'DEBIT'
                      const isZero = Number(e.amount) === 0
                      const isReversed = e.status === 'REVERSED' || (e.transactionRef || '').startsWith('REV-')

                      return (
                        <tr
                          key={e.id}
                          className="hover:bg-slate-800/30 transition-colors group"
                        >
                          {/* Reference Column */}
                          <td className="py-4 px-4 whitespace-nowrap">
                            <div className="flex items-center gap-1.5">
                              <span className="font-mono text-xs text-cyan-300 bg-cyan-950/40 border border-cyan-800/40 px-2 py-0.5 rounded">
                                {e.transactionRef}
                              </span>
                              <button
                                onClick={() => copyToClipboard(e.transactionRef, e.id)}
                                className="text-slate-500 hover:text-cyan-400 transition-colors p-1 rounded"
                                title="Copy Reference"
                              >
                                {copiedId === e.id ? <Check size={13} className="text-emerald-400" /> : <Copy size={13} />}
                              </button>
                            </div>
                          </td>

                          {/* Timestamp Column */}
                          <td className="py-4 px-4 whitespace-nowrap text-xs text-slate-400">
                            {formatDateTime(e.createdAt)}
                          </td>

                          {/* Description Column */}
                          <td className="py-4 px-4">
                            <div className="flex items-center gap-2">
                              <div
                                className="w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0"
                                style={{
                                  background: isDebit
                                    ? 'rgba(244,63,94,0.12)'
                                    : isZero
                                    ? 'rgba(100,116,139,0.15)'
                                    : 'rgba(16,185,129,0.12)',
                                }}
                              >
                                {isDebit ? (
                                  <ArrowUpRight size={15} style={{ color: '#fb7185' }} />
                                ) : isZero ? (
                                  <CheckCircle2 size={15} className="text-slate-400" />
                                ) : (
                                  <ArrowDownLeft size={15} style={{ color: '#34d399' }} />
                                )}
                              </div>
                              <div>
                                <p className="text-sm font-medium text-slate-200">
                                  {e.description}
                                </p>
                                <span className="text-[10px] text-slate-500 font-mono">
                                  Entry #{e.id}
                                </span>
                              </div>
                            </div>
                          </td>

                          {/* Debit Column */}
                          <td className="py-4 px-4 whitespace-nowrap text-right font-medium">
                            {isDebit ? (
                              <span className="text-rose-400 font-semibold">
                                −{formatINR(e.amount)}
                              </span>
                            ) : (
                              <span className="text-slate-600">—</span>
                            )}
                          </td>

                          {/* Credit Column */}
                          <td className="py-4 px-4 whitespace-nowrap text-right font-medium">
                            {!isDebit && !isZero ? (
                              <span className="text-emerald-400 font-semibold">
                                +{formatINR(e.amount)}
                              </span>
                            ) : !isDebit && isZero ? (
                              <span className="text-slate-400 text-xs font-mono">0.00</span>
                            ) : (
                              <span className="text-slate-600">—</span>
                            )}
                          </td>

                          {/* Status Column */}
                          <td className="py-4 px-4 whitespace-nowrap text-center">
                            <span
                              className={`badge ${
                                e.status === 'POSTED'
                                  ? 'badge-success'
                                  : e.status === 'REVERSED' || isReversed
                                  ? 'badge-pending'
                                  : 'badge-failed'
                              }`}
                            >
                              {e.status}
                            </span>
                          </td>

                          {/* Running Balance Column */}
                          <td className="py-4 px-4 whitespace-nowrap text-right font-bold text-slate-100 font-mono text-sm">
                            {formatINR(e.balanceAfter)}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Auditability & Integrity Footer Note */}
        <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/30 flex items-start gap-3">
          <ShieldCheck size={18} className="text-cyan-400 mt-0.5 flex-shrink-0" />
          <div className="text-xs text-slate-400 space-y-1">
            <p className="font-semibold text-slate-300">
              PayVault Immutable Financial Ledger Guarantee
            </p>
            <p>
              Posted transactions and ledger entries are strictly immutable and auditable. Entries cannot be altered or silently deleted. Reversals are recorded as offsetting credit journal entries linked to the original transaction reference. Debit/credit updates and balance mutations are transactionally atomic with duplicate retry protection.
            </p>
          </div>
        </div>
      </div>
    </Layout>
  )
}
