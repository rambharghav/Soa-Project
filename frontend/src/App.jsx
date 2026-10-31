import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Login from './pages/Login.jsx'
import Signup from './pages/Signup.jsx'
import CompleteProfile from './pages/CompleteProfile.jsx'
import Dashboard from './pages/Dashboard.jsx'
import SendMoney from './pages/SendMoney.jsx'
import TransactionHistory from './pages/TransactionHistory.jsx'
import Ledger from './pages/Ledger.jsx'
import Profile from './pages/Profile.jsx'

function App() {
  const { isAuthenticated, hasProfile, initializing } = useAuth()

  if (initializing) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 text-white">
        <div className="spinner mb-4" style={{ width: 36, height: 36, borderWidth: 3 }} />
        <p className="text-slate-400 text-sm animate-pulse">Initializing PayVault...</p>
      </div>
    )
  }

  return (
    <Routes>
      {/* Public Routes */}
      <Route
        path="/login"
        element={
          isAuthenticated
            ? <Navigate to={hasProfile ? '/dashboard' : '/complete-profile'} replace />
            : <Login />
        }
      />
      <Route
        path="/signup"
        element={
          isAuthenticated
            ? <Navigate to={hasProfile ? '/dashboard' : '/complete-profile'} replace />
            : <Signup />
        }
      />

      {/* Onboarding - requires auth but not profile */}
      <Route
        path="/complete-profile"
        element={
          !isAuthenticated
            ? <Navigate to="/login" replace />
            : hasProfile
              ? <Navigate to="/dashboard" replace />
              : <CompleteProfile />
        }
      />

      {/* Protected Routes - require auth + profile */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/send"
        element={
          <ProtectedRoute>
            <SendMoney />
          </ProtectedRoute>
        }
      />
      <Route
        path="/transactions"
        element={
          <ProtectedRoute>
            <TransactionHistory />
          </ProtectedRoute>
        }
      />
      <Route
        path="/ledger"
        element={
          <ProtectedRoute>
            <Ledger />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />

      {/* Catch-all */}
      <Route path="*" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
    </Routes>
  )
}

export default App
