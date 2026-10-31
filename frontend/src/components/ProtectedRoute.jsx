import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function ProtectedRoute({ children, requireProfile = true }) {
  const { isAuthenticated, hasProfile, initializing } = useAuth()

  if (initializing) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 text-white">
        <div className="spinner mb-4" style={{ width: 36, height: 36, borderWidth: 3 }} />
        <p className="text-slate-400 text-sm animate-pulse">Loading...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (requireProfile && !hasProfile) {
    return <Navigate to="/complete-profile" replace />
  }

  return children
}
