import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useToast } from '../context/ToastContext.jsx'
import api from '../api/client.js'
import { formatINR, formatDateTime, getErrorMessage } from '../utils/helpers.js'
import Layout from '../components/Layout.jsx'
import {
  Wallet,
  TrendingUp,
  Send,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  Clock,
  IndianRupee,
  BookOpen,
} from 'lucide-react'

export default function Dashboard() {
  const { userId, username } = useAuth()
  const toast = useToast()
  const [profile, setProfile] = useState(null)
  const [wallet, setWallet] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)

  const fetchData = async () => {
    setLoading(true)
    try {
      const [userRes, walletRes, txRes] = await Promise.all([
        api.get(`/api/users/${userId}`),
        api.get(`/api/wallets/user/${userId}`),
        api.get(`/api/transactions/user/${userId}`),
      ])
      setProfile(userRes.data)
      setWallet(walletRes.data)
      setTransactions(txRes.data || [])
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (userId) fetchData()
  }, [userId])

  const recentTx = transactions.slice(0, 5)

  if (loading) {
    return (
      <Layout>
        <div className="loading-screen">
          <div className="spinner" style={{ width: 40, height: 40 }} />
          <p className="text-slate-400">Loading dashboard...</p>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="space-y-8 animate-fade-in">
        {/* Welcome Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white">
            Welcome back, <span style={{ color: '#22d3ee' }}>{profile?.name || username}</span>
          </h1>
          <p className="text-slate-400 mt-1">Here's your financial overview</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {/* Balance Card */}
          <div className="glass-card p-6 animate-pulse-glow">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2 text-slate-400 text-sm font-medium">
                <Wallet size={16} />
                Wallet Balance
              </div>
              <span className="badge badge-success">{wallet?.currency || 'INR'}</span>
            </div>
            <p className="text-3xl font-bold text-white tracking-tight">
              {formatINR(wallet?.balance || 0)}
            </p>
            <p className="text-xs text-slate-500 mt-2">
              Wallet ID: #{wallet?.id}
            </p>
          </div>

          {/* Transactions Count */}
          <div className="glass-card p-6">
            <div className="flex items-center gap-2 text-slate-400 text-sm font-medium mb-4">
              <TrendingUp size={16} />
              Total Transactions
            </div>
            <p className="text-3xl font-bold text-white">{transactions.length}</p>
            <p className="text-xs text-slate-500 mt-2">All time</p>
          </div>

          {/* Quick Send */}
          <div className="glass-card p-6 flex flex-col justify-between sm:col-span-2 lg:col-span-1">
            <div className="flex items-center gap-2 text-slate-400 text-sm font-medium mb-4">
              <Send size={16} />
              Quick Actions
            </div>
            <div className="flex gap-2">
              <Link
                to="/send"
                className="btn-primary flex-1 text-center no-underline text-xs sm:text-sm py-2.5"
              >
                <Send size={15} />
                Send
              </Link>
              <Link
                to="/ledger"
                className="btn-secondary flex-1 text-center no-underline text-xs sm:text-sm py-2.5"
              >
                <BookOpen size={15} />
                Ledger
              </Link>
              <button
                onClick={fetchData}
                className="btn-secondary text-sm px-3"
                title="Refresh"
              >
                <RefreshCw size={15} />
              </button>
            </div>
          </div>
        </div>

        {/* User Info Card */}
        <div className="glass-card p-6">
          <h2 className="text-lg font-semibold text-white mb-4">Account Details</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <p className="text-xs text-slate-500 uppercase tracking-wider mb-1">User ID</p>
              <p className="text-slate-200 font-medium">#{userId}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500 uppercase tracking-wider mb-1">Email</p>
              <p className="text-slate-200 font-medium">{profile?.email || '—'}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500 uppercase tracking-wider mb-1">Phone</p>
              <p className="text-slate-200 font-medium">{profile?.phone || '—'}</p>
            </div>
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="glass-card p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-semibold text-white flex items-center gap-2">
              <Clock size={18} style={{ color: '#22d3ee' }} />
              Recent Transactions
            </h2>
            {transactions.length > 5 && (
              <Link
                to="/transactions"
                className="text-sm font-medium no-underline"
                style={{ color: '#22d3ee' }}
              >
                View All →
              </Link>
            )}
          </div>

          {recentTx.length === 0 ? (
            <div className="text-center py-12">
              <IndianRupee size={48} className="mx-auto mb-3" style={{ color: '#3d4f63' }} />
              <p className="text-slate-400">No transactions yet</p>
              <p className="text-xs text-slate-500 mt-1">Your transactions will appear here</p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentTx.map((tx) => {
                const isSender = tx.senderUserId === userId
                return (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between p-4 rounded-xl transition-colors"
                    style={{ background: 'rgba(10,15,30,0.5)' }}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className="w-10 h-10 rounded-xl flex items-center justify-center"
                        style={{
                          background: isSender
                            ? 'rgba(244,63,94,0.12)'
                            : 'rgba(16,185,129,0.12)',
                        }}
                      >
                        {isSender ? (
                          <ArrowUpRight size={20} style={{ color: '#fb7185' }} />
                        ) : (
                          <ArrowDownLeft size={20} style={{ color: '#34d399' }} />
                        )}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-slate-200">
                          {isSender
                            ? `Sent to User #${tx.receiverUserId}`
                            : `Received from User #${tx.senderUserId}`}
                        </p>
                        <p className="text-xs text-slate-500">
                          {formatDateTime(tx.createdAt)}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p
                        className="text-sm font-semibold"
                        style={{ color: isSender ? '#fb7185' : '#34d399' }}
                      >
                        {isSender ? '−' : '+'}{formatINR(tx.amount)}
                      </p>
                      <span className={`badge badge-${tx.status === 'COMPLETED' ? 'success' : tx.status === 'FAILED' ? 'failed' : 'pending'}`}>
                        {tx.status}
                      </span>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </Layout>
  )
}
