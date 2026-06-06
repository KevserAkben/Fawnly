export default function SeverityBadge({ severity }) {
  const cls = {
    HIGH: 'badge-high',
    MEDIUM: 'badge-medium',
    LOW: 'badge-low',
  }[severity] || 'badge-low'

  return <span className={cls}>{severity}</span>
}
