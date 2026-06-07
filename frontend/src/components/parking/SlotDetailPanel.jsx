import { BoltIcon, CheckCircleIcon, XMarkIcon } from '@heroicons/react/24/outline'

export default function SlotDetailPanel({ slot, floor, lot, onConfirm, onClear }) {
  const hasSelection = Boolean(slot)

  return (
    <div className={`${hasSelection ? 'fixed inset-x-0 bottom-0 z-30 max-h-[78vh] overflow-auto rounded-t-2xl border-t border-gray-200' : 'hidden'} bg-white p-5 shadow-2xl lg:sticky lg:top-6 lg:block lg:max-h-none lg:rounded-xl lg:border lg:border-gray-200 lg:shadow-sm`}>
      <div className="mb-4 flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-normal text-primary-600">Selected slot</p>
          <h2 className="mt-1 text-xl font-bold text-gray-950">
            {slot ? slot.code : 'No slot selected'}
          </h2>
        </div>
        {hasSelection && (
          <button type="button" onClick={onClear} className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700 lg:hidden">
            <XMarkIcon className="h-5 w-5" />
          </button>
        )}
      </div>

      {!slot ? (
        <div className="rounded-lg border border-dashed border-gray-300 bg-gray-50 p-5 text-sm text-gray-500">
          Pick an available rectangle from the map to see slot details and confirm parking.
        </div>
      ) : (
        <div className="space-y-5">
          <div className="rounded-lg bg-gray-950 p-4 text-white">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs text-gray-300">Slot number</p>
                <p className="mt-1 text-2xl font-bold">{slot.slotNumber}</p>
              </div>
              {slot.evSupported && (
                <span className="inline-flex items-center gap-1 rounded-full bg-violet-500 px-3 py-1 text-xs font-bold text-white">
                  <BoltIcon className="h-4 w-4" />
                  EV
                </span>
              )}
            </div>
          </div>

          <dl className="grid grid-cols-2 gap-3 text-sm">
            <Detail label="Lot" value={lot?.name ?? lot?.lotName ?? 'Selected lot'} />
            <Detail label="Floor" value={floor?.floorName || `Floor ${floor?.floorNumber ?? ''}`} />
            <Detail label="Section" value={slot.code?.slice(0, 1)} />
            <Detail label="Vehicle type" value={formatType(slot.type)} />
            <Detail label="Hourly price" value={`$${Number(slot.hourlyRate ?? 0).toFixed(2)}`} />
            <Detail label="Status" value={formatType(slot.status)} />
          </dl>

          <button type="button" onClick={onConfirm} className="btn-primary w-full py-3">
            <CheckCircleIcon className="mr-2 h-5 w-5" />
            Park Here
          </button>
        </div>
      )}
    </div>
  )
}

function Detail({ label, value }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2">
      <dt className="text-xs font-medium text-gray-400">{label}</dt>
      <dd className="mt-1 font-semibold text-gray-900">{value}</dd>
    </div>
  )
}

function formatType(value) {
  return String(value ?? '')
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (match) => match.toUpperCase())
}
