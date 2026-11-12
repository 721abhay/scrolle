import { AnimatePresence, motion } from 'framer-motion'

function DigitColumn({ digit }) {
  return (
    <div className="overflow-hidden h-28 md:h-40 flex items-center justify-center">
      <AnimatePresence mode="popLayout">
        <motion.span
          key={digit}
          initial={{ y: 80, opacity: 0, scale: 0.8 }}
          animate={{ y: 0, opacity: 1, scale: 1 }}
          exit={{ y: -80, opacity: 0, scale: 0.8 }}
          transition={{ type: 'spring', stiffness: 400, damping: 25 }}
          className="block tabular-nums"
        >
          {digit}
        </motion.span>
      </AnimatePresence>
    </div>
  )
}

export default function CounterDisplay({ count, total, platform }) {
  const digits = String(count).padStart(1, '0').split('')

  return (
    <div className="flex flex-col items-center gap-2">
      {/* Platform label */}
      <motion.div
        key={platform.id}
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        className="glass rounded-full px-4 py-1.5 flex items-center gap-2 mb-2"
      >
        <span className="text-lg">{platform.emoji}</span>
        <span className="text-white/70 text-sm font-semibold">{platform.label}</span>
      </motion.div>

      {/* Big number */}
      <div className="flex items-center justify-center gap-1">
        {digits.map((d, i) => (
          <div
            key={i}
            className="text-white font-black"
            style={{ fontSize: digits.length > 2 ? '5rem' : '8rem', lineHeight: 1 }}
          >
            <DigitColumn digit={d} />
          </div>
        ))}
      </div>

      {/* Label */}
      <motion.p
        className="text-white/40 text-sm font-medium tracking-widest uppercase mt-1"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
      >
        {platform.label} watched
      </motion.p>

      {/* Total badge */}
      {total > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass rounded-full px-3 py-1 mt-1"
        >
          <span className="text-white/50 text-xs">Total across all: </span>
          <span className="text-white font-bold text-xs">{total}</span>
        </motion.div>
      )}
    </div>
  )
}

