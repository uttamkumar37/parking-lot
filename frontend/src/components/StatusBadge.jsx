export default function StatusBadge({ status }) {
  const map = {
    ACTIVE:          'badge-green',
    COMPLETED:       'badge-blue',
    PENDING_PAYMENT: 'badge-yellow',
    CANCELLED:       'badge-red',
    PENDING:         'badge-yellow',
    FAILED:          'badge-red',
    REFUNDED:        'badge-blue',
  }
  return <span className={map[status] ?? 'badge bg-gray-100 text-gray-700'}>{status}</span>
}
