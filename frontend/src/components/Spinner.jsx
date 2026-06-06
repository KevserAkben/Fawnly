export default function Spinner({ size = 'md' }) {
  const sizeClass = size === 'sm' ? 'w-5 h-5' : size === 'lg' ? 'w-10 h-10' : 'w-7 h-7'
  return (
    <div
      className={`${sizeClass} border-3 border-mint border-t-mint-koyu rounded-full animate-spin`}
      style={{ borderWidth: '3px' }}
    />
  )
}
