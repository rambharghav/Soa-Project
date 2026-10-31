import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import api from '../api/client.js'
import { formatINR, formatDateTime, getErrorMessage } from '../utils/helpers.js'
import Layout from '../components/Layout.jsx'
import {
  History,
  ArrowUpRight,
  ArrowDownLeft,
  Search,
  Filter,
  IndianRupee,
} from 'lucide-react'

export default function TransactionHistory() {
  const { userId } = useAuth()
  const toast = useToast()
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all') // all, sent, received, success, failed

  useEffect(() => {
    const fetchTransactions = async () => {
      try {
        const res = await api.get(`/api/transactions/user/${userId}`)
        setTransactions(res.data || [])
      } catch (err) {
        toast.error(getErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }
    if (userId) fetchTransactions()
  }, [userId])

  const filtered = transactions.filter((tx) => {
    if (filter === 'sent' && tx.senderUserId !== userId) return false
    if (filter === 'received' && tx.receiverUserId !== userId) return false
    if (filter === 'success' && tx.status !== 'COMPLETED') return false
    if (filter === 'failed' && tx.status !== 'FAILED') return false

    if (search) {
      const s = search.toLowerCase()
      const matchId = String(tx.id).includes(s)
      const matchSender = String(tx.senderUserId).includes(s)
      const matchReceiver = String(tx.receiverUserId).includes(s)
      const matchAmount = String(tx.amount).includes(s)
      return matchId || matchSender || matchReceiver || matchAmount
    }
    return true
  })

  const filterButtons = [
    { key: 'all', label: 'All' },
    { key: 'sent', label: 'Sent' },
    { key: 'received', label: 'Received' },
    { key: 'success', label: 'Success' },
    { key: 'failed', label: 'Failed' },
  ]

  if (loading) {
    return (
      <Layout>
        <div className="loading-screen">
          <div className="spinner" style={{ width: 40, height: 40 }} />
          <p className="text-slate-400">Loading transactions...</p>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="space-y-6 animate-fade-in">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white flex items-center gap-3">
            <History size={28} style={{ color: '#22d3ee' }} />
            Transaction History
          </h1>
          <p className="text-slate-400 mt-1">{transactions.length} total transactions</p>
        </div>

        {/* Filters & Search */}
        <div className="glass-card p-4">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Search */}
            <div className="relative flex-1">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                id="tx-search"
                type="text"
                className="pv-input"
                style={{ paddingLeft: '40px' }}
                placeholder="Search by ID, user ID, or amount..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            {/* Filter buttons */}
            <div className="flex items-center gap-2 flex-wrap">
              <Filter size={16} className="text-slate-500" />
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

        {/* Transaction List */}
        {filtered.length === 0 ? (
          <div className="glass-card p-12 text-center">
            <IndianRupee size={56} className="mx-auto mb-4" style={{ color: '#3d4f63' }} />
            <p className="text-lg text-slate-400 font-medium">No transactions found</p>
            <p className="text-sm text-slate-500 mt-1">
              {transactions.length === 0
                ? 'Start by sending money to another user'
                : 'Try adjusting your filters'}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filtered.map((tx) => {
              const isSender = tx.senderUserId === userId
              const statusClass =
                tx.status === 'COMPLETED'
                  ? 'success'
                  : tx.status === 'FAILED'
                  ? 'failed'
                  : 'pending'

              return (
                <div
                  key={tx.id}
                  className="glass-card p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="flex items-center gap-4">
                    <div
                      className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
                      style={{
                        background: isSender
                          ? 'rgba(244,63,94,0.12)'
                          : 'rgba(16,185,129,0.12)',
                      }}
                    >
                      {isSender ? (
                        <ArrowUpRight size={24} style={{ color: '#fb7185' }} />
                      ) : (
                        <ArrowDownLeft size={24} style={{ color: '#34d399' }} />
                      )}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-white">
                        {isSender
                          ? `Sent to User #${tx.receiverUserId}`
                          : `Received from User #${tx.senderUserId}`}
                      </p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Transaction #{tx.id} • {formatDateTime(tx.createdAt)}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 sm:text-right">
                    <div className="flex-1">
                      <p
                        className="text-lg font-bold"
                        style={{ color: isSender ? '#fb7185' : '#34d399' }}
                      >
                        {isSender ? '−' : '+'}{formatINR(tx.amount)}
                      </p>
                    </div>
                    <span className={`badge badge-${statusClass}`}>
                      {tx.status}
                    </span>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </Layout>
  )
}
