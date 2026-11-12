import { motion, AnimatePresence } from 'framer-motion'

export default function TopPill({ total, platform, pulse, elapsed }) {
  const mins = Math.floor(elapsed / 60)
  const secs = elapsed % 60
  const timeStr = mins > 0
    ? `${mins}m ${String(secs).padStart(2, '0')}s`
    : `${secs}s`

  return (
    <div className="relative z-20 w-full flex justify-center pt-4 pb-1">
      <motion.div
        className="flex items-center gap-0 overflow-hidden"
        style={{
          background: 'rgba(0,0,0,0.75)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          borderRadius: 999,
          border: '1px solid rgba(255,255,255,0.15)',
          boxShadow: pulse
            ? `0 0 0 3px ${platform.color}60, 0 4px 20px rgba(0,0,0,0.4)`
            : '0 4px 20px rgba(0,0,0,0.4)',
          transition: 'box-shadow 0.2s ease',
        }}
        animate={pulse ? { scale: [1, 1.08, 1] } : { scale: 1 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
      >
        {/* Left — icon + "live" dot */}
        <div className="flex items-center gap-2 pl-3 pr-2 py-2">
          {/* Pulsing live dot */}
          <span className="relative flex h-2 w-2">
            <motion.span
              className="absolute inline-flex h-full w-full rounded-full opacity-75"
              style={{ background: platform.color }}
              animate={{ scale: [1, 2, 1], opacity: [0.7, 0, 0.7] }}
              transition={{ duration: 1.5, repeat: Infinity }}
            />
            <span
              className="relative inline-flex rounded-full h-2 w-2"
              style={{ background: platform.color }}
            />
          </span>
          <span className="text-sm">{platform.emoji}</span>
        </div>

        {/* Divider */}
        <div style={{ width: 1, height: 20, background: 'rgba(255,255,255,0.15)' }} />

        {/* Center — big count */}
        <div className="px-3 py-2 flex items-center gap-1.5">
          <AnimatePresence mode="popLayout">
            <motion.span
              key={total}
              initial={{ y: -16, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 16, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 500, damping: 30 }}
              className="text-white font-black tabular-nums"
              style={{ fontSize: '1.1rem', lineHeight: 1 }}
            >
              {total}
            </motion.span>
          </AnimatePresence>
          <span className="text-white/50 text-xs font-medium">
            {total === 1 ? 'reel' : 'reels'}
          </span>
        </div>

        {/* Divider */}
        <div style={{ width: 1, height: 20, background: 'rgba(255,255,255,0.15)' }} />

        {/* Right — session time */}
        <div className="flex items-center gap-1 pr-3 pl-2 py-2">
          <span className="text-white/40 text-xs">⏱</span>
          <span className="text-white/60 text-xs font-semibold tabular-nums">{timeStr}</span>
        </div>
      </motion.div>
    </div>
  )
}

