import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import LoginPage       from './pages/LoginPage'
import RegisterPage    from './pages/RegisterPage'
import DashboardPage   from './pages/DashboardPage'
import ParkVehiclePage from './pages/ParkVehiclePage'
import BookingsPage    from './pages/BookingsPage'
import BookingDetailPage from './pages/BookingDetailPage'
import AdminDashPage   from './pages/AdminDashPage'
import PaymentSuccessPage from './pages/PaymentSuccessPage'
import Layout          from './components/Layout'

function PrivateRoute({ children, adminOnly = false }) {
  const { isAuthenticated, user } = useAuthStore()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (adminOnly && user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<Layout />}>
        <Route path="/dashboard" element={
          <PrivateRoute><DashboardPage /></PrivateRoute>
        } />
        <Route path="/park" element={
          <PrivateRoute><ParkVehiclePage /></PrivateRoute>
        } />
        <Route path="/bookings" element={
          <PrivateRoute><BookingsPage /></PrivateRoute>
        } />
        <Route path="/bookings/:id" element={
          <PrivateRoute><BookingDetailPage /></PrivateRoute>
        } />
        <Route path="/payment/success" element={
          <PrivateRoute><PaymentSuccessPage /></PrivateRoute>
        } />
        <Route path="/admin" element={
          <PrivateRoute adminOnly><AdminDashPage /></PrivateRoute>
        } />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
