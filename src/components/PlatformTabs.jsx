import { motion } from 'framer-motion'

export default function PlatformTabs({ platforms, active, counts, onSelect }) {
  return (
    <div className="glass rounded-2xl p-1.5 flex gap-1">
      {platforms.map((platform) => {
        const isActive = platform.id === active
        return (
          <motion.button
            key={platform.id}
            onClick={() => onSelect(platform.id)}
            className="relative flex-1 rounded-xl py-2.5 px-2 flex flex-col items-center gap-1 border-0 outline-none cursor-pointer transition-colors"
            style={{ background: 'transparent' }}
            whileTap={{ scale: 0.95 }}
          >
            {/* Active background */}
            {isActive && (
              <motion.div
                layoutId="activeTab"
                className="absolute inset-0 rounded-xl"
                style={{
                  background: `${platform.color}25`,
                  border: `1px solid ${platform.color}60`,
                }}
                transition={{ type: 'spring', stiffness: 500, damping: 30 }}
              />
            )}

            <span className="relative z-10 text-lg">{platform.emoji}</span>
            <span
              className="relative z-10 text-xs font-semibold"
              style={{ color: isActive ? platform.color : 'rgba(255,255,255,0.4)' }}
            >
              {platform.label}
            </span>

            {/* Count badge */}
            {counts[platform.id] > 0 && (
              <motion.span
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                className="absolute -top-1 -right-1 z-20 min-w-5 h-5 rounded-full flex items-center justify-center text-white font-black text-xs px-1"
                style={{ background: platform.color, fontSize: '10px' }}
              >
                {counts[platform.id]}
              </motion.span>
            )}
          </motion.button>
        )
      })}
    </div>
  )
}

