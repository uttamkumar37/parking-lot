import { useEffect, useState } from 'react'
import { getDashboard, adminGetBookings } from '../api/endpoints'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'
import {
  UsersIcon, CurrencyDollarIcon, ArchiveBoxIcon,
  BuildingOffice2Icon,
} from '@heroicons/react/24/outline'

function StatCard({ label, value, Icon, color = 'text-primary-600' }) {
  return (
    <div className="card flex items-center gap-4">
      <div className={`p-3 rounded-xl bg-gray-100 ${color}`}>
        <Icon className="h-6 w-6" />
      </div>
      <div>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
        <p className="text-sm text-gray-500">{label}</p>
      </div>
    </div>
  )
}

export default function AdminDashPage() {
  const [dash, setDash] = useState(null)
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getDashboard(), adminGetBookings(0)])
      .then(([{ data: d }, { data: b }]) => {
        setDash(d.data)
        setBookings(b.data?.content ?? [])
        setLoading(false)
      })
  }, [])

  if (loading) return <Spinner size="lg" />

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Active Bookings" value={dash.activeBookings}  Icon={ArchiveBoxIcon} color="text-green-600" />
        <StatCard label="Bookings Today"  value={dash.totalBookingsToday} Icon={UsersIcon} color="text-blue-600" />
        <StatCard label="Revenue Today"   value={`$${dash.revenueToday ?? 0}`} Icon={CurrencyDollarIcon} color="text-emerald-600" />
        <StatCard label="Active Lots"     value={dash.totalActiveLots}  Icon={BuildingOffice2Icon} color="text-primary-600" />
      </div>

      {/* Occupancy */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-800">Occupancy</h2>
          <span className="text-2xl font-bold text-orange-500">{dash.occupancyRate}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-3">
          <div
            className="bg-orange-400 h-3 rounded-full transition-all"
            style={{ width: `${Math.min(dash.occupancyRate, 100)}%` }}
          />
        </div>
        <p className="text-xs text-gray-400 mt-2">{dash.availableSlots} of {dash.totalSlots} slots available</p>
      </div>

      {/* Slot availability by type */}
      <div className="card">
        <h2 className="font-semibold text-gray-800 mb-4">Available by Slot Type</h2>
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          {Object.entries(dash.slotAvailabilityByType ?? {}).map(([type, count]) => (
            <div key={type} className="text-center p-3 bg-gray-50 rounded-lg">
              <p className="text-2xl font-bold text-gray-900">{count}</p>
              <p className="text-xs text-gray-500 mt-1">{type}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Recent bookings */}
      <div className="card p-0">
        <div className="px-6 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-800">Recent Bookings</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-100 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['ID', 'Vehicle', 'Slot', 'Entry', 'Status'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {bookings.map((b) => (
                <tr key={b.bookingId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-xs font-mono text-gray-500">{b.bookingId?.slice(0, 8)}</td>
                  <td className="px-4 py-3">{b.licensePlate}</td>
                  <td className="px-4 py-3">{b.slotNumber} · {b.floorName}</td>
                  <td className="px-4 py-3 text-xs text-gray-600">
                    {b.entryTime ? new Date(b.entryTime).toLocaleString() : '—'}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={b.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
