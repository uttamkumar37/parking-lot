import api from './axios'

// ── Auth ──────────────────────────────────────────────────────────────────────
export const register = (data) => api.post('/auth/register', data)
export const login    = (data) => api.post('/auth/login', data)

// ── Parking Lots ──────────────────────────────────────────────────────────────
export const getLots         = ()        => api.get('/parking/lots')
export const getLotAvailability = (lotId) => api.get(`/parking/lots/${lotId}/availability`)

// ── Bookings ──────────────────────────────────────────────────────────────────
export const parkVehicle     = (data)      => api.post('/bookings/park', data)
export const exitVehicle     = (bookingId) => api.post(`/bookings/${bookingId}/exit`)
export const getMyBookings   = (page = 0)  => api.get(`/bookings/my?page=${page}&size=10`)
export const getBooking      = (id)        => api.get(`/bookings/${id}`)

// ── Payments ──────────────────────────────────────────────────────────────────
export const createCheckout  = (bookingId) => api.post(`/payments/${bookingId}/checkout`)
export const getPaymentStatus = (bookingId) => api.get(`/payments/${bookingId}/status`)

// ── Vehicles ──────────────────────────────────────────────────────────────────
export const getMyVehicles   = () => api.get('/vehicles')

// ── Admin ─────────────────────────────────────────────────────────────────────
export const getDashboard    = () => api.get('/admin/dashboard')
export const adminGetBookings = (page = 0) => api.get(`/admin/bookings?page=${page}&size=20`)
export const getRevenue      = (from, to)  => api.get(`/admin/revenue?from=${from}&to=${to}`)
export const createLot       = (data)      => api.post('/admin/lots', data)
export const toggleLot       = (lotId)     => api.patch(`/admin/lots/${lotId}/toggle`)
export const getUsers        = (page = 0)  => api.get(`/admin/users?page=${page}&size=20`)
