import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getBooking, exitVehicle, createCheckout } from '../api/endpoints'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'
import toast from 'react-hot-toast'
import { ArrowLeftIcon, CreditCardIcon, ArrowRightOnRectangleIcon } from '@heroicons/react/24/outline'

export default function BookingDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [booking, setBooking] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    getBooking(id).then(({ data }) => {
      setBooking(data.data)
      setLoading(false)
    })
  }

  useEffect(load, [id])

  const handleExit = async () => {
    if (!confirm('Confirm vehicle exit? A bill will be generated.')) return
    try {
      await exitVehicle(id)
      toast.success('Exit recorded — bill generated!')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Exit failed')
    }
  }

  const handlePay = async () => {
    try {
      const { data } = await createCheckout(id)
      window.location.href = data.data  // redirect to Stripe
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Payment initiation failed')
    }
  }

  if (loading) return <Spinner size="lg" />
  if (!booking) return null

  const b = booking

  return (
    <div className="max-w-xl mx-auto space-y-5">
      <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm">
        <ArrowLeftIcon className="h-4 w-4" /> Back
      </button>

      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">Booking Details</h1>
            <p className="text-xs text-gray-400 font-mono mt-0.5">{b.bookingId}</p>
          </div>
          <StatusBadge status={b.status} />
        </div>

        <dl className="space-y-3 text-sm">
          {[
            ['Vehicle',   `${b.licensePlate} (${b.vehicleType})`],
            ['Slot',      `${b.slotNumber} · ${b.floorName} · ${b.lotName}`],
            ['Entry',     b.entryTime ? new Date(b.entryTime).toLocaleString() : '—'],
            ['Exit',      b.exitTime  ? new Date(b.exitTime).toLocaleString()  : 'Still parked'],
            ['Duration',  b.durationMinutes != null ? `${b.durationMinutes} minutes` : '—'],
          ].map(([label, value]) => (
            <div key={label} className="flex justify-between">
              <dt className="text-gray-500">{label}</dt>
              <dd className="font-medium text-gray-900 text-right">{value}</dd>
            </div>
          ))}
        </dl>

        {b.bill && (
          <div className="mt-5 pt-5 border-t border-gray-100 space-y-2 text-sm">
            <h3 className="font-semibold text-gray-800 mb-3">Bill</h3>
            {[
              ['Base Amount',   `$${b.bill.baseAmount}`],
              ['Tax (10%)',     `$${b.bill.taxAmount}`],
              ['Strategy',      b.bill.pricingStrategy],
              ['Payment Status', b.bill.paymentStatus],
            ].map(([label, value]) => (
              <div key={label} className="flex justify-between">
                <span className="text-gray-500">{label}</span>
                <span className="font-medium">{value}</span>
              </div>
            ))}
            <div className="flex justify-between text-base font-bold pt-2 border-t border-gray-100">
              <span>Total</span>
              <span className="text-primary-700">${b.bill.totalAmount}</span>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="mt-6 flex gap-3">
          {b.status === 'ACTIVE' && (
            <button onClick={handleExit} className="btn-danger flex-1">
              <ArrowRightOnRectangleIcon className="h-4 w-4 mr-2" />
              Exit & Generate Bill
            </button>
          )}
          {b.status === 'PENDING_PAYMENT' && b.bill?.paymentStatus !== 'COMPLETED' && (
            <button onClick={handlePay} className="btn-primary flex-1">
              <CreditCardIcon className="h-4 w-4 mr-2" />
              Pay Now ${b.bill?.totalAmount}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
