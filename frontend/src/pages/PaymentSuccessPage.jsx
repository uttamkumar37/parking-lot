import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CheckCircleIcon } from '@heroicons/react/24/outline'

export default function PaymentSuccessPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const sessionId = params.get('session_id')

  useEffect(() => {
    const t = setTimeout(() => navigate('/bookings'), 5000)
    return () => clearTimeout(t)
  }, [navigate])

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-4">
      <CheckCircleIcon className="h-20 w-20 text-success-500" />
      <h1 className="text-2xl font-bold text-gray-900">Payment Successful!</h1>
      <p className="text-gray-500 text-center max-w-sm">
        Your parking fee has been paid. Redirecting to your bookings in 5 seconds…
      </p>
      {sessionId && (
        <p className="text-xs text-gray-400 font-mono">Session: {sessionId.slice(0, 16)}…</p>
      )}
      <button onClick={() => navigate('/bookings')} className="btn-primary mt-4">
        View Bookings
      </button>
    </div>
  )
}
