import { BoltIcon } from '@heroicons/react/24/outline'

export const SLOT_TYPE_FILTERS = [
  { value: 'ALL', label: 'All' },
  { value: 'SMALL', label: 'Small' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LARGE', label: 'Large' },
  { value: 'OVERSIZED', label: 'Oversized' },
  { value: 'EV', label: 'EV' },
]

export default function VehicleTypeFilter({ value, onChange }) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {SLOT_TYPE_FILTERS.map((item) => {
        const selected = value === item.value
        return (
          <button
            key={item.value}
            type="button"
            onClick={() => onChange(item.value)}
            className={`inline-flex min-h-10 shrink-0 items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition ${
              selected
                ? 'border-gray-950 bg-gray-950 text-white shadow-sm'
                : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:text-gray-950'
            }`}
          >
            {item.value === 'EV' && <BoltIcon className="h-4 w-4" />}
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
