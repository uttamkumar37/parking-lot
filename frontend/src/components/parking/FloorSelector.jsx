export default function FloorSelector({ floors, selectedFloorId, onChange }) {
  if (!floors?.length) return null

  const selectedFloor = floors.find((floor) => floor.floorId === selectedFloorId) ?? floors[0]

  return (
    <div className="space-y-3">
      <div className="hidden gap-2 overflow-x-auto sm:flex">
        {floors.map((floor) => {
          const selected = floor.floorId === selectedFloor.floorId
          return (
            <button
              key={floor.floorId}
              type="button"
              onClick={() => onChange(floor.floorId)}
              className={`shrink-0 rounded-lg border px-4 py-2 text-sm font-semibold transition ${
                selected
                  ? 'border-primary-600 bg-primary-600 text-white shadow-sm'
                  : 'border-gray-200 bg-white text-gray-600 hover:border-primary-300 hover:text-primary-700'
              }`}
            >
              {floor.floorName || `Floor ${floor.floorNumber}`}
            </button>
          )
        })}
      </div>

      <select
        className="input sm:hidden"
        value={selectedFloor.floorId}
        onChange={(event) => onChange(event.target.value)}
      >
        {floors.map((floor) => (
          <option key={floor.floorId} value={floor.floorId}>
            {floor.floorName || `Floor ${floor.floorNumber}`}
          </option>
        ))}
      </select>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <FloorStat label="Total" value={selectedFloor.totalSlots} />
        <FloorStat label="Available" value={selectedFloor.availableSlots} tone="text-emerald-700" />
        <FloorStat label="Occupied" value={selectedFloor.occupiedSlots} tone="text-gray-700" />
        <FloorStat label="Reserved / Disabled" value={(selectedFloor.reservedSlots ?? 0) + (selectedFloor.disabledSlots ?? 0)} tone="text-amber-700" />
      </div>
    </div>
  )
}

function FloorStat({ label, value, tone = 'text-gray-900' }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white px-4 py-3">
      <p className={`text-xl font-bold ${tone}`}>{value ?? 0}</p>
      <p className="mt-0.5 text-xs font-medium uppercase tracking-normal text-gray-400">{label}</p>
    </div>
  )
}
