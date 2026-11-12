import { useState } from 'react'
import { motion } from 'framer-motion'

export default function AddButton({ onAdd, platform }) {
  const [pressing, setPressing] = useState(false)

  const handleClick = () => {
    setPressing(true)
    onAdd()
    setTimeout(() => setPressing(false), 200)
  }

  return (
    <div className="relative flex items-center justify-center">
      {/* Outer glow rings */}
      <motion.div
        className="absolute rounded-full"
        style={{
          width: 160,
          height: 160,
          border: `2px solid ${platform.color}40`,
        }}
        animate={{ scale: [1, 1.2, 1], opacity: [0.5, 0, 0.5] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
      />
      <motion.div
        className="absolute rounded-full"
        style={{
          width: 190,
          height: 190,
          border: `1px solid ${platform.color}25`,
        }}
        animate={{ scale: [1, 1.3, 1], opacity: [0.3, 0, 0.3] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut', delay: 0.3 }}
      />

      {/* Main button */}
      <motion.button
        onClick={handleClick}
        whileTap={{ scale: 0.88 }}
        whileHover={{ scale: 1.05 }}
        className="relative z-10 w-32 h-32 rounded-full flex items-center justify-center cursor-pointer border-0 outline-none"
        style={{
          background: `radial-gradient(circle at 35% 35%, ${platform.color}ff, ${platform.color}99)`,
          boxShadow: `0 0 40px ${platform.color}60, 0 20px 60px rgba(0,0,0,0.5)`,
        }}
        animate={{
          boxShadow: pressing
            ? `0 0 15px ${platform.color}40, 0 5px 20px rgba(0,0,0,0.5)`
            : `0 0 40px ${platform.color}60, 0 20px 60px rgba(0,0,0,0.5)`,
        }}
      >
        {/* Inner shine */}
        <div
          className="absolute inset-0 rounded-full opacity-30"
          style={{
            background: 'radial-gradient(circle at 30% 30%, rgba(255,255,255,0.8), transparent 60%)',
          }}
        />

        {/* Icon */}
        <motion.div
          className="relative z-10 flex flex-col items-center gap-1"
          animate={{ scale: pressing ? 0.9 : 1 }}
          transition={{ duration: 0.1 }}
        >
          <span style={{ fontSize: '2rem' }}>{platform.emoji}</span>
          <span className="text-white font-black text-xs tracking-wider">+1</span>
        </motion.div>
      </motion.button>
    </div>
  )
}

