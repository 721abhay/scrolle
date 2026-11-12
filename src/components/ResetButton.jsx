import { useState } from 'react'
import { motion, useAnimate } from 'framer-motion'

export default function ResetButton({ onReset }) {
  const [confirm, setConfirm] = useState(false)
  const [scope, animate] = useAnimate()

  const handleClick = async () => {
    if (!confirm) {
      // Shake animation
      await animate(scope.current, {
        x: [-8, 8, -8, 8, -4, 4, 0],
      }, { duration: 0.4 })
      setConfirm(true)
      setTimeout(() => setConfirm(false), 3000)
    } else {
      onReset()
      setConfirm(false)
    }
  }

  return (
    <motion.button
      ref={scope}
      onClick={handleClick}
      whileTap={{ scale: 0.9 }}
      className="glass rounded-full px-6 py-2 flex items-center gap-2 border-0 outline-none cursor-pointer"
      animate={{
        borderColor: confirm ? 'rgba(239,68,68,0.6)' : 'rgba(255,255,255,0.1)',
        background: confirm ? 'rgba(239,68,68,0.15)' : 'rgba(255,255,255,0.05)',
      }}
    >
      <span className="text-sm">{confirm ? '⚠️' : '🔄'}</span>
      <span
        className="text-xs font-semibold"
        style={{ color: confirm ? '#ef4444' : 'rgba(255,255,255,0.4)' }}
      >
        {confirm ? 'Tap again to reset!' : 'Reset session'}
      </span>
    </motion.button>
  )
}

