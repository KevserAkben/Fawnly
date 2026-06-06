import { getPasswordStrength } from '../utils/validation'

export default function PasswordStrength({ password }) {
  const strength = getPasswordStrength(password)
  if (!password) return null

  return (
    <div className="mt-2">
      <div className="flex gap-1 mb-1">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-1.5 flex-1 rounded-full transition-colors"
            style={{
              backgroundColor: i <= strength.level ? strength.color : '#e5e7eb',
            }}
          />
        ))}
      </div>
      <span className="text-xs text-gray-600">{strength.label}</span>
    </div>
  )
}
