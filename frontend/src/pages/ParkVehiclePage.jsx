import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import {
  ArrowPathIcon,
  BuildingOffice2Icon,
  SparklesIcon,
} from '@heroicons/react/24/outline'
import { getLots, getLotAvailability, getLotSlotMap, parkVehicle } from '../api/endpoints'
import ConfirmParkingModal from '../components/parking/ConfirmParkingModal'
import FloorSelector from '../components/parking/FloorSelector'
import LotSelector from '../components/parking/LotSelector'
import ParkingSlotMap from '../components/parking/ParkingSlotMap'
import SlotDetailPanel from '../components/parking/SlotDetailPanel'
import SlotLegend from '../components/parking/SlotLegend'
import VehicleTypeFilter from '../components/parking/VehicleTypeFilter'

const COMPATIBLE_VEHICLE_TYPES = {
  SMALL: ['MOTORCYCLE'],
  MEDIUM: ['CAR', 'SUV', 'ELECTRIC_CAR', 'MOTORCYCLE'],
  LARGE: ['TRUCK', 'CAR', 'SUV'],
  OVERSIZED: ['BUS', 'TRUCK'],
  EV: ['ELECTRIC_CAR'],
}

const DEFAULT_VEHICLE_TYPE = {
  SMALL: 'MOTORCYCLE',
  MEDIUM: 'CAR',
  LARGE: 'TRUCK',
  OVERSIZED: 'BUS',
  EV: 'ELECTRIC_CAR',
}

export default function ParkVehiclePage() {
  const location = useLocation()
  const navigate = useNavigate()

  const [lots, setLots] = useState([])
  const [availabilityByLot, setAvailabilityByLot] = useState({})
  const [selectedLotId, setSelectedLotId] = useState(location.state?.lotId ?? '')
  const [lotsLoading, setLotsLoading] = useState(true)
  const [lotsError, setLotsError] = useState('')

  const [slotMap, setSlotMap] = useState(null)
  const [slotMapLoading, setSlotMapLoading] = useState(false)
  const [slotMapError, setSlotMapError] = useState('')
  const [selectedFloorId, setSelectedFloorId] = useState('')
  const [filterType, setFilterType] = useState('ALL')
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [bookingLoading, setBookingLoading] = useState(false)

  const selectedLot = useMemo(
    () => lots.find((lot) => lot.id === selectedLotId),
    [lots, selectedLotId]
  )

  const selectedFloor = useMemo(
    () => slotMap?.floors?.find((floor) => floor.floorId === selectedFloorId) ?? slotMap?.floors?.[0],
    [selectedFloorId, slotMap]
  )

  const allowedVehicleTypes = useMemo(
    () => selectedSlot ? COMPATIBLE_VEHICLE_TYPES[selectedSlot.type] ?? ['CAR'] : ['CAR'],
    [selectedSlot]
  )

  useEffect(() => {
    loadLots()
  }, [])

  useEffect(() => {
    if (!selectedLotId) return
    loadSlotMap(selectedLotId)
  }, [selectedLotId])

  async function loadLots() {
    setLotsLoading(true)
    setLotsError('')
    try {
      const { data } = await getLots()
      const activeLots = data.data ?? []
      setLots(activeLots)

      const initialLotId = selectedLotId || activeLots[0]?.id || ''
      setSelectedLotId(initialLotId)

      const availabilityPairs = await Promise.all(
        activeLots.map(async (lot) => {
          try {
            const response = await getLotAvailability(lot.id)
            return [lot.id, response.data.data]
          } catch {
            return [lot.id, null]
          }
        })
      )
      setAvailabilityByLot(Object.fromEntries(availabilityPairs))
    } catch (error) {
      setLotsError(error.response?.data?.message ?? 'Unable to load parking lots')
    } finally {
      setLotsLoading(false)
    }
  }

  async function loadSlotMap(lotId) {
    setSlotMapLoading(true)
    setSlotMapError('')
    setSlotMap(null)
    setSelectedSlot(null)
    try {
      const { data } = await getLotSlotMap(lotId)
      const map = data.data
      setSlotMap(map)
      setSelectedFloorId(map?.floors?.[0]?.floorId ?? '')
    } catch (error) {
      setSlotMapError(error.response?.data?.message ?? 'Unable to load slot map')
    } finally {
      setSlotMapLoading(false)
    }
  }

  const handleLotSelect = (lotId) => {
    setSelectedLotId(lotId)
    setSelectedFloorId('')
    setSelectedSlot(null)
    setFilterType('ALL')
  }

  const handleFloorChange = (floorId) => {
    setSelectedFloorId(floorId)
    setSelectedSlot(null)
  }

  const handleFilterChange = (type) => {
    setFilterType(type)
    setSelectedSlot(null)
  }

  const handleConfirm = async (vehicleForm) => {
    if (!selectedLotId || !selectedSlot) return

    setBookingLoading(true)
    try {
      const { data } = await parkVehicle({
        lotId: selectedLotId,
        slotId: selectedSlot.id,
        licensePlate: vehicleForm.licensePlate.trim().toUpperCase(),
        vehicleType: vehicleForm.vehicleType,
        brand: vehicleForm.brand,
        model: vehicleForm.model,
        color: vehicleForm.color,
      })
      toast.success(`Vehicle parked at ${data.data.slotNumber}`)
      navigate(`/bookings/${data.data.bookingId}`)
    } catch (error) {
      toast.error(error.response?.data?.message ?? 'Parking failed')
      setConfirmOpen(false)
      if (selectedLotId) loadSlotMap(selectedLotId)
    } finally {
      setBookingLoading(false)
    }
  }

  return (
    <div className={`space-y-6 ${selectedSlot ? 'pb-36 lg:pb-0' : ''}`}>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-primary-600">Parking</p>
          <h1 className="mt-1 text-2xl font-bold text-gray-950">Choose Parking Slot</h1>
          <p className="mt-1 text-sm text-gray-500">Pick a lot, floor, and available slot before confirming your vehicle.</p>
        </div>
        {selectedLot && (
          <div className="inline-flex items-center gap-2 rounded-full border border-primary-100 bg-primary-50 px-3 py-2 text-sm font-semibold text-primary-700">
            <SparklesIcon className="h-4 w-4" />
            {availabilityByLot[selectedLot.id]?.totalAvailable ?? 0} slots available
          </div>
        )}
      </div>

      {lotsError ? (
        <RetryPanel message={lotsError} onRetry={loadLots} />
      ) : (
        <LotSelector
          lots={lots}
          selectedLotId={selectedLotId}
          availabilityByLot={availabilityByLot}
          loading={lotsLoading}
          onSelect={handleLotSelect}
        />
      )}

      {!lotsLoading && lots.length === 0 && !lotsError && (
        <div className="rounded-xl border border-dashed border-gray-300 bg-white p-10 text-center text-gray-500">
          <BuildingOffice2Icon className="mx-auto h-12 w-12 text-gray-300" />
          <p className="mt-3 font-semibold text-gray-700">No open parking lots</p>
          <p className="mt-1 text-sm">Check back later or contact parking support.</p>
        </div>
      )}

      {selectedLotId && (
        <section className="space-y-5">
          {slotMapLoading ? (
            <SlotMapSkeleton />
          ) : slotMapError ? (
            <RetryPanel message={slotMapError} onRetry={() => loadSlotMap(selectedLotId)} />
          ) : slotMap ? (
            <>
              <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
                <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-gray-950">{slotMap.lotName}</h2>
                    <p className="mt-1 text-sm text-gray-500">{slotMap.address}{slotMap.city ? `, ${slotMap.city}` : ''}</p>
                  </div>
                  <SlotLegend />
                </div>
                <div className="space-y-5">
                  <FloorSelector
                    floors={slotMap.floors}
                    selectedFloorId={selectedFloor?.floorId}
                    onChange={handleFloorChange}
                  />
                  <VehicleTypeFilter value={filterType} onChange={handleFilterChange} />
                </div>
              </div>

              <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
                <ParkingSlotMap
                  floor={selectedFloor}
                  filterType={filterType}
                  selectedSlotId={selectedSlot?.id}
                  onSelect={setSelectedSlot}
                />
                <SlotDetailPanel
                  slot={selectedSlot}
                  floor={selectedFloor}
                  lot={selectedLot}
                  onConfirm={() => setConfirmOpen(true)}
                  onClear={() => setSelectedSlot(null)}
                />
              </div>
            </>
          ) : null}
        </section>
      )}

      <ConfirmParkingModal
        open={confirmOpen}
        slot={selectedSlot}
        lot={selectedLot}
        allowedVehicleTypes={allowedVehicleTypes}
        defaultVehicleType={selectedSlot ? DEFAULT_VEHICLE_TYPE[selectedSlot.type] : 'CAR'}
        loading={bookingLoading}
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleConfirm}
      />
    </div>
  )
}

function RetryPanel({ message, onRetry }) {
  return (
    <div className="rounded-xl border border-red-100 bg-red-50 p-5">
      <p className="font-semibold text-red-800">{message}</p>
      <button type="button" onClick={onRetry} className="btn-secondary mt-4">
        <ArrowPathIcon className="mr-2 h-4 w-4" />
        Retry
      </button>
    </div>
  )
}

function SlotMapSkeleton() {
  return (
    <div className="space-y-4">
      <div className="rounded-xl border border-gray-200 bg-white p-4">
        <div className="h-5 w-48 animate-pulse rounded bg-gray-200" />
        <div className="mt-3 h-4 w-72 animate-pulse rounded bg-gray-100" />
        <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-4">
          {[1, 2, 3, 4].map((item) => (
            <div key={item} className="h-16 animate-pulse rounded-lg bg-gray-100" />
          ))}
        </div>
      </div>
      <div className="h-[420px] animate-pulse rounded-xl bg-gray-100" />
    </div>
  )
}
