import { BoltIcon } from '@heroicons/react/24/outline'

const LEGEND_ITEMS = [
  { label: 'Available', className: 'border-emerald-300 bg-white' },
  { label: 'Selected', className: 'border-primary-600 bg-primary-600' },
  { label: 'Occupied', className: 'border-gray-300 bg-gray-200' },
  { label: 'EV', className: 'border-violet-300 bg-violet-50', ev: true },
  { label: 'Disabled / Reserved', className: 'border-amber-300 bg-amber-100' },
]

export default function SlotLegend() {
  return (
    <div className="flex flex-wrap gap-x-4 gap-y-2 text-xs text-gray-500">
      {LEGEND_ITEMS.map((item) => (
        <div key={item.label} className="flex items-center gap-2">
          <span className={`inline-flex h-4 w-6 items-center justify-center rounded border ${item.className}`}>
            {item.ev && <BoltIcon className="h-3 w-3 text-violet-700" />}
          </span>
          <span>{item.label}</span>
        </div>
      ))}
    </div>
  )
}
