import { ArrowDownIcon, BoltIcon, NoSymbolIcon } from '@heroicons/react/24/outline'

export default function ParkingSlotMap({ floor, filterType, selectedSlotId, onSelect }) {
  if (!floor) {
    return (
      <div className="rounded-xl border border-dashed border-gray-300 bg-white p-10 text-center text-gray-500">
        Select a parking lot to view available floors.
      </div>
    )
  }

  const sections = floor.sections
    .map((section) => ({
      ...section,
      slots: section.slots.filter((slot) => filterType === 'ALL' || slot.type === filterType),
    }))
    .filter((section) => section.slots.length > 0)

  const hasSlots = sections.some((section) => section.slots.length > 0)

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm md:p-5">
      <div className="mb-5 flex items-center justify-center">
        <div className="inline-flex items-center gap-2 rounded-full bg-gray-950 px-4 py-2 text-xs font-bold text-white">
          ENTRY
          <ArrowDownIcon className="h-4 w-4" />
        </div>
      </div>

      {!hasSlots ? (
        <div className="flex min-h-72 flex-col items-center justify-center rounded-lg border border-dashed border-gray-300 bg-gray-50 px-6 text-center">
          <NoSymbolIcon className="h-10 w-10 text-gray-300" />
          <p className="mt-3 text-sm font-semibold text-gray-700">No slots match this filter</p>
          <p className="mt-1 text-sm text-gray-500">Choose another floor or slot type.</p>
        </div>
      ) : (
        <div className="overflow-x-auto pb-3">
          <div className="min-w-[720px] space-y-8">
            {sections.map((section) => (
              <section key={section.sectionName} className="space-y-3">
                <div className="flex items-center gap-3">
                  <h3 className="text-sm font-bold text-gray-900">Section {section.sectionName}</h3>
                  <div className="h-px flex-1 bg-gray-200" />
                </div>

                <div className="space-y-2">
                  {chunk(section.slots, 6).map((row, rowIndex) => (
                    <div key={`${section.sectionName}-${rowIndex}`} className="grid grid-cols-[repeat(3,84px)_80px_repeat(3,84px)] items-center justify-center gap-2">
                      {row.slice(0, 3).map((slot) => (
                        <SlotButton
                          key={slot.id}
                          slot={slot}
                          selected={selectedSlotId === slot.id}
                          onSelect={onSelect}
                        />
                      ))}
                      {Array.from({ length: 3 - row.slice(0, 3).length }).map((_, index) => (
                        <span key={`left-empty-${index}`} />
                      ))}
                      <div className="flex h-12 items-center justify-center rounded border border-dashed border-gray-300 bg-gray-50 text-[11px] font-semibold text-gray-400">
                        Drive Lane
                      </div>
                      {row.slice(3, 6).map((slot) => (
                        <SlotButton
                          key={slot.id}
                          slot={slot}
                          selected={selectedSlotId === slot.id}
                          onSelect={onSelect}
                        />
                      ))}
                      {Array.from({ length: 3 - row.slice(3, 6).length }).map((_, index) => (
                        <span key={`right-empty-${index}`} />
                      ))}
                    </div>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function SlotButton({ slot, selected, onSelect }) {
  const available = slot.status === 'AVAILABLE'
  const disabled = !available

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onSelect(slot)}
      className={`relative flex h-12 w-[84px] items-center justify-center rounded-lg border text-sm font-bold transition ${slotClass(slot, selected)}`}
      aria-label={`${slot.code} ${slot.type} ${slot.status}`}
      title={`${slot.code} · ${slot.slotNumber} · ${slot.type} · ${slot.status}`}
    >
      <span>{slot.code}</span>
      {slot.evSupported && (
        <BoltIcon className={`absolute right-1.5 top-1.5 h-3.5 w-3.5 ${selected ? 'text-white' : 'text-violet-700'}`} />
      )}
    </button>
  )
}

function slotClass(slot, selected) {
  if (selected) return 'border-primary-700 bg-primary-600 text-white shadow-md ring-2 ring-primary-200'
  if (slot.status === 'AVAILABLE' && slot.evSupported) return 'border-violet-300 bg-violet-50 text-violet-900 hover:border-violet-500 hover:shadow-sm'
  if (slot.status === 'AVAILABLE') return 'border-emerald-300 bg-white text-gray-900 hover:border-emerald-500 hover:bg-emerald-50 hover:shadow-sm'
  if (slot.status === 'OCCUPIED') return 'cursor-not-allowed border-gray-300 bg-gray-200 text-gray-400'
  if (slot.status === 'RESERVED') return 'cursor-not-allowed border-amber-300 bg-amber-100 text-amber-700'
  return 'cursor-not-allowed border-red-200 bg-red-50 text-red-400'
}

function chunk(items, size) {
  const rows = []
  for (let index = 0; index < items.length; index += size) {
    rows.push(items.slice(index, index + size))
  }
  return rows
}
