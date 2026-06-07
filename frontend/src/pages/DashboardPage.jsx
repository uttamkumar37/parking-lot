import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getLots, getLotAvailability } from '../api/endpoints'
import Spinner from '../components/Spinner'
import { BuildingOffice2Icon, BoltIcon, TruckIcon } from '@heroicons/react/24/outline'

const SLOT_COLORS = {
  SMALL:     'bg-sky-100 text-sky-800 border-sky-200',
  MEDIUM:    'bg-green-100 text-green-800 border-green-200',
  LARGE:     'bg-orange-100 text-orange-800 border-orange-200',
  EV:        'bg-purple-100 text-purple-800 border-purple-200',
  OVERSIZED: 'bg-rose-100 text-rose-800 border-rose-200',
}

export default function DashboardPage() {
  const [lots, setLots] = useState([])
  const [selectedLot, setSelectedLot] = useState(null)
  const [availability, setAvailability] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    getLots().then(({ data }) => {
      setLots(data.data ?? [])
      setLoading(false)
    })
  }, [])

  useEffect(() => {
    if (!selectedLot) return
    setAvailability(null)
    getLotAvailability(selectedLot.id).then(({ data }) => setAvailability(data.data))
  }, [selectedLot])

  if (loading) return <Spinner size="lg" />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Parking Dashboard</h1>
        <p className="text-gray-500 text-sm mt-1">Real-time slot availability across all locations</p>
      </div>

      {/* Lot grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {lots.map((lot) => (
          <button
            key={lot.id}
            onClick={() => setSelectedLot(lot)}
            className={`card text-left hover:shadow-md transition-shadow ${
              selectedLot?.id === lot.id ? 'ring-2 ring-primary-500' : ''
            }`}
          >
            <div className="flex items-start gap-3">
              <BuildingOffice2Icon className="h-8 w-8 text-primary-500 shrink-0 mt-0.5" />
              <div>
                <h3 className="font-semibold text-gray-900">{lot.name}</h3>
                <p className="text-sm text-gray-500">{lot.address}</p>
                {lot.city && <p className="text-xs text-gray-400">{lot.city}</p>}
              </div>
            </div>
            <div className="mt-3 flex items-center gap-2">
              <span className="text-xs text-gray-500">{lot.totalFloors} floor{lot.totalFloors !== 1 ? 's' : ''}</span>
              <span className={`badge ${lot.active ? 'badge-green' : 'badge-red'}`}>
                {lot.active ? 'Open' : 'Closed'}
              </span>
            </div>
          </button>
        ))}
      </div>

      {/* Availability panel */}
      {selectedLot && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-semibold">{selectedLot.name} — Availability</h2>
              <p className="text-sm text-gray-500">{selectedLot.address}</p>
            </div>
            <button
              className="btn-primary"
              onClick={() => navigate('/park', { state: { lotId: selectedLot.id, lotName: selectedLot.name } })}
            >
              <TruckIcon className="h-4 w-4 mr-2" />
              Choose Parking Slot
            </button>
          </div>

          {!availability ? (
            <Spinner />
          ) : (
            <>
              {/* Summary stats */}
              <div className="grid grid-cols-3 gap-4 mb-6">
                <div className="text-center p-4 bg-gray-50 rounded-lg">
                  <p className="text-3xl font-bold text-primary-600">{availability.totalAvailable}</p>
                  <p className="text-xs text-gray-500 mt-1">Available</p>
                </div>
                <div className="text-center p-4 bg-gray-50 rounded-lg">
                  <p className="text-3xl font-bold text-gray-700">{availability.totalCapacity}</p>
                  <p className="text-xs text-gray-500 mt-1">Total Capacity</p>
                </div>
                <div className="text-center p-4 bg-gray-50 rounded-lg">
                  <p className="text-3xl font-bold text-orange-500">{availability.occupancyPercent}%</p>
                  <p className="text-xs text-gray-500 mt-1">Occupancy</p>
                </div>
              </div>

              {/* Slot type breakdown */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
                {Object.entries(availability.availableByType ?? {}).map(([type, count]) => (
                  <div
                    key={type}
                    className={`border rounded-lg p-3 text-center ${SLOT_COLORS[type] ?? 'bg-gray-50 text-gray-700 border-gray-200'}`}
                  >
                    {type === 'EV' && <BoltIcon className="h-4 w-4 mx-auto mb-1" />}
                    <p className="text-2xl font-bold">{count}</p>
                    <p className="text-xs font-medium mt-0.5">{type}</p>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      {lots.length === 0 && !loading && (
        <div className="text-center py-12 text-gray-400">
          <BuildingOffice2Icon className="h-16 w-16 mx-auto mb-3 opacity-30" />
          <p className="text-lg">No parking lots available</p>
        </div>
      )}
    </div>
  )
}
