import { BuildingOffice2Icon, CheckCircleIcon, MapPinIcon } from '@heroicons/react/24/outline'

export default function LotSelector({ lots, selectedLotId, availabilityByLot, loading, onSelect }) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        {[1, 2, 3].map((item) => (
          <div key={item} className="h-36 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="h-4 w-2/3 animate-pulse rounded bg-gray-200" />
            <div className="mt-4 h-3 w-full animate-pulse rounded bg-gray-100" />
            <div className="mt-2 h-3 w-3/4 animate-pulse rounded bg-gray-100" />
            <div className="mt-6 h-6 w-24 animate-pulse rounded-full bg-gray-100" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
      {lots.map((lot) => {
        const selected = selectedLotId === lot.id
        const availability = availabilityByLot[lot.id]

        return (
          <button
            key={lot.id}
            type="button"
            onClick={() => onSelect(lot.id)}
            className={`rounded-xl border bg-white p-4 text-left shadow-sm transition ${
              selected
                ? 'border-primary-500 ring-2 ring-primary-100'
                : 'border-gray-200 hover:border-primary-300 hover:shadow-md'
            }`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <div className={`rounded-lg p-2 ${selected ? 'bg-primary-50 text-primary-700' : 'bg-gray-50 text-gray-500'}`}>
                  <BuildingOffice2Icon className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-gray-950">{lot.name}</h3>
                  <p className="mt-1 flex items-start gap-1 text-sm text-gray-500">
                    <MapPinIcon className="mt-0.5 h-4 w-4 shrink-0" />
                    <span>{lot.address}</span>
                  </p>
                  {lot.city && <p className="mt-1 text-xs text-gray-400">{lot.city}</p>}
                </div>
              </div>
              {selected && <CheckCircleIcon className="h-5 w-5 shrink-0 text-primary-600" />}
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
              <span className={`badge ${lot.active ? 'badge-green' : 'badge-red'}`}>
                {lot.active ? 'Open' : 'Closed'}
              </span>
              <span className="rounded-full bg-gray-100 px-2.5 py-1 font-medium text-gray-600">
                {lot.totalFloors} floor{lot.totalFloors === 1 ? '' : 's'}
              </span>
              <span className="rounded-full bg-primary-50 px-2.5 py-1 font-semibold text-primary-700">
                {availability ? `${availability.totalAvailable} available` : 'Checking slots'}
              </span>
            </div>
          </button>
        )
      })}
    </div>
  )
}
