import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getLots, parkVehicle } from '../api/endpoints'
import toast from 'react-hot-toast'
import { TruckIcon } from '@heroicons/react/24/outline'

const VEHICLE_TYPES = ['MOTORCYCLE', 'CAR', 'SUV', 'TRUCK', 'ELECTRIC_CAR', 'BUS']

export default function ParkVehiclePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [lots, setLots] = useState([])
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({
    lotId: location.state?.lotId ?? '',
    licensePlate: '',
    vehicleType: 'CAR',
    brand: '',
    model: '',
    color: '',
  })

  useEffect(() => {
    getLots().then(({ data }) => setLots(data.data ?? []))
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await parkVehicle(form)
      toast.success(`Vehicle parked! Slot: ${data.data.slotNumber}`)
      navigate(`/bookings/${data.data.bookingId}`)
    } catch (err) {
      toast.error(err.response?.data?.message ?? 'Parking failed')
    } finally {
      setLoading(false)
    }
  }

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Park a Vehicle</h1>
        <p className="text-gray-500 text-sm mt-1">Fill in the details to allocate a parking slot</p>
      </div>

      <form onSubmit={handleSubmit} className="card space-y-5">
        {/* Lot */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Parking Lot</label>
          <select className="input" value={form.lotId} onChange={(e) => set('lotId', e.target.value)} required>
            <option value="">Select a lot…</option>
            {lots.filter((l) => l.active).map((l) => (
              <option key={l.id} value={l.id}>{l.name} — {l.address}</option>
            ))}
          </select>
        </div>

        {/* License plate */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">License Plate</label>
          <input
            className="input uppercase"
            placeholder="ABC-1234"
            value={form.licensePlate}
            onChange={(e) => set('licensePlate', e.target.value.toUpperCase())}
            required
          />
        </div>

        {/* Vehicle type */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Vehicle Type</label>
          <select className="input" value={form.vehicleType} onChange={(e) => set('vehicleType', e.target.value)}>
            {VEHICLE_TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </div>

        {/* Brand / Model / Color */}
        <div className="grid grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Brand</label>
            <input className="input text-sm" placeholder="Toyota" value={form.brand} onChange={(e) => set('brand', e.target.value)} />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Model</label>
            <input className="input text-sm" placeholder="Camry" value={form.model} onChange={(e) => set('model', e.target.value)} />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Color</label>
            <input className="input text-sm" placeholder="White" value={form.color} onChange={(e) => set('color', e.target.value)} />
          </div>
        </div>

        <button type="submit" disabled={loading} className="btn-primary w-full py-3">
          <TruckIcon className="h-5 w-5 mr-2" />
          {loading ? 'Allocating slot…' : 'Park Vehicle'}
        </button>
      </form>
    </div>
  )
}
