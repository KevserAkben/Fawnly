export default function ProgressBar({ progress = 0 }) {
  return (
    <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
      <div
        className="h-full rounded-full transition-all duration-500 ease-out"
        style={{
          width: `${progress}%`,
          background: 'linear-gradient(90deg, var(--mint), var(--mint-koyu))',
        }}
      />
    </div>
  )
}
