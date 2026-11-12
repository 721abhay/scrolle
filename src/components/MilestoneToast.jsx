import { motion } from 'framer-motion'
import { useEffect } from 'react'

export default function MilestoneToast({ milestone, onDismiss }) {
  useEffect(() => {
    const t = setTimeout(onDismiss, 4000)
    return () => clearTimeout(t)
  }, [onDismiss])

  return (
    <motion.div
      className="fixed inset-0 z-50 flex items-center justify-center p-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onDismiss}
      style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(8px)' }}
    >
      <motion.div
        className="glass rounded-3xl p-8 max-w-sm w-full text-center shadow-2xl"
        initial={{ scale: 0.5, y: 100, opacity: 0 }}
        animate={{ scale: 1, y: 0, opacity: 1 }}
        exit={{ scale: 0.5, y: 100, opacity: 0 }}
        transition={{ type: 'spring', stiffness: 400, damping: 25 }}
        onClick={e => e.stopPropagation()}
      >
        {/* Big emoji */}
        <motion.div
          className="text-7xl mb-4"
          animate={{ rotate: [0, -10, 10, -10, 10, 0], scale: [1, 1.2, 1] }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          {milestone.emoji}
        </motion.div>

        {/* Count */}
        <motion.div
          className="text-white font-black text-5xl mb-3"
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: 'spring', delay: 0.1 }}
        >
          {milestone.count}
        </motion.div>

        {/* Message */}
        <p className="text-white/80 font-semibold text-lg leading-tight mb-6">
          {milestone.msg}
        </p>

        {/* Dismiss */}
        <motion.button
          className="glass rounded-full px-6 py-2.5 text-white/60 text-sm font-medium border-0 outline-none cursor-pointer"
          whileTap={{ scale: 0.9 }}
          onClick={onDismiss}
        >
          ok ok ill keep going 📱
        </motion.button>

        {/* Progress bar */}
        <motion.div
          className="mt-4 h-1 rounded-full overflow-hidden"
          style={{ background: 'rgba(255,255,255,0.1)' }}
        >
          <motion.div
            className="h-full rounded-full"
            style={{ background: 'rgba(255,255,255,0.4)' }}
            initial={{ width: '100%' }}
            animate={{ width: '0%' }}
            transition={{ duration: 4, ease: 'linear' }}
          />
        </motion.div>
      </motion.div>
    </motion.div>
  )
}

