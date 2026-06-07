import { useEffect, useMemo, useState } from 'react'
import { XMarkIcon } from '@heroicons/react/24/outline'

const VEHICLE_TYPE_LABELS = {
  MOTORCYCLE: 'Motorcycle',
  CAR: 'Car',
  SUV: 'SUV',
  TRUCK: 'Truck',
  ELECTRIC_CAR: 'Electric car',
  BUS: 'Bus',
}

export default function ConfirmParkingModal({ open, slot, lot, allowedVehicleTypes, defaultVehicleType, loading, onClose, onConfirm }) {
  const [form, setForm] = useState({
    licensePlate: '',
    vehicleType: defaultVehicleType ?? 'CAR',
    brand: '',
    model: '',
    color: '',
  })

  useEffect(() => {
    if (!open) return
    setForm({
      licensePlate: '',
      vehicleType: defaultVehicleType ?? allowedVehicleTypes?.[0] ?? 'CAR',
      brand: '',
      model: '',
      color: '',
    })
  }, [allowedVehicleTypes, defaultVehicleType, open])

  const vehicleOptions = useMemo(
    () => allowedVehicleTypes?.length ? allowedVehicleTypes : ['CAR'],
    [allowedVehicleTypes]
  )

  if (!open || !slot) return null

  const set = (key, value) => setForm((current) => ({ ...current, [key]: value }))

  const submit = (event) => {
    event.preventDefault()
    onConfirm(form)
  }

  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-gray-950/40 px-3 py-4 backdrop-blur-sm sm:items-center">
      <form onSubmit={submit} className="w-full max-w-lg rounded-xl bg-white shadow-2xl">
        <div className="flex items-start justify-between border-b border-gray-200 px-5 py-4">
          <div>
            <h2 className="text-lg font-bold text-gray-950">Confirm parking</h2>
            <p className="mt-1 text-sm text-gray-500">
              {lot?.name ?? 'Selected lot'} · Slot {slot.code} · {slot.slotNumber}
            </p>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700">
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-4 px-5 py-5">
          <div>
            <label className="mb-1 block text-sm font-semibold text-gray-700">License plate</label>
            <input
              className="input uppercase"
              placeholder="ABC123"
              value={form.licensePlate}
              onChange={(event) => set('licensePlate', event.target.value.toUpperCase())}
              maxLength={10}
              required
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-semibold text-gray-700">Vehicle type</label>
            <select
              className="input"
              value={form.vehicleType}
              onChange={(event) => set('vehicleType', event.target.value)}
            >
              {vehicleOptions.map((type) => (
                <option key={type} value={type}>{VEHICLE_TYPE_LABELS[type] ?? type}</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-gray-600">Brand</label>
              <input className="input" placeholder="Toyota" value={form.brand} onChange={(event) => set('brand', event.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gray-600">Model</label>
              <input className="input" placeholder="Camry" value={form.model} onChange={(event) => set('model', event.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-gray-600">Color</label>
              <input className="input" placeholder="White" value={form.color} onChange={(event) => set('color', event.target.value)} />
            </div>
          </div>
        </div>

        <div className="flex flex-col-reverse gap-2 border-t border-gray-200 px-5 py-4 sm:flex-row sm:justify-end">
          <button type="button" onClick={onClose} className="btn-secondary" disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={loading || !form.licensePlate || !slot}>
            {loading ? 'Confirming...' : 'Confirm Slot'}
          </button>
        </div>
      </form>
    </div>
  )
}
