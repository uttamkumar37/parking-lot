import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getMyBookings } from '../api/endpoints'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'
import { QueueListIcon, ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/24/outline'

export default function BookingsPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback((p) => {
    setLoading(true)
    getMyBookings(p).then(({ data: res }) => {
      setData(res.data)
      setLoading(false)
    })
  }, [])

  useEffect(() => { load(page) }, [load, page])

  const bookings = data?.content ?? []
  const totalPages = data?.totalPages ?? 1

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">My Bookings</h1>

      {loading ? <Spinner size="lg" /> : bookings.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <QueueListIcon className="h-14 w-14 mx-auto mb-3 opacity-30" />
          <p>No bookings yet. <Link to="/park" className="text-primary-600 hover:underline">Park a vehicle</Link> to get started.</p>
        </div>
      ) : (
        <>
          <div className="overflow-x-auto card p-0">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {['Booking', 'Vehicle', 'Slot', 'Entry', 'Duration', 'Status', 'Amount', ''].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-100">
                {bookings.map((b) => (
                  <tr key={b.bookingId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{b.bookingId.slice(0, 8)}</td>
                    <td className="px-4 py-3">
                      <div className="font-semibold">{b.licensePlate}</div>
                      <div className="text-xs text-gray-400">{b.vehicleType}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div>{b.slotNumber}</div>
                      <div className="text-xs text-gray-400">{b.floorName} · {b.lotName}</div>
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-600">
                      {b.entryTime ? new Date(b.entryTime).toLocaleString() : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs">
                      {b.durationMinutes != null ? `${b.durationMinutes} min` : '—'}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={b.status} /></td>
                    <td className="px-4 py-3 text-xs font-medium">
                      {b.bill?.totalAmount != null ? `$${b.bill.totalAmount}` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <Link to={`/bookings/${b.bookingId}`}
                        className="text-primary-600 hover:underline text-xs font-medium">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-500">Page {page + 1} of {totalPages}</p>
            <div className="flex gap-2">
              <button onClick={() => setPage((p) => p - 1)} disabled={page === 0} className="btn-secondary p-2">
                <ChevronLeftIcon className="h-4 w-4" />
              </button>
              <button onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1} className="btn-secondary p-2">
                <ChevronRightIcon className="h-4 w-4" />
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
