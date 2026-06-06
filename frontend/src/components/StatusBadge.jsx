export default function StatusBadge({ status }) {
  const styles = {
    queued: 'bg-gray-200 text-gray-700',
    running: 'bg-blue-100 text-blue-700',
    done: 'bg-green-100 text-green-700',
    failed: 'bg-red-100 text-red-700',
  }

  return (
    <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status] || styles.queued}`}>
      {status}
    </span>
  )
}
