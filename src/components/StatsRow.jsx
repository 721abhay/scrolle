import { motion } from 'framer-motion'

function pad(n) {
  return String(n).padStart(2, '0')
}

function formatTime(secs) {
  const h = Math.floor(secs / 3600)
  const m = Math.floor((secs % 3600) / 60)
  const s = secs % 60
  if (h > 0) return `${pad(h)}:${pad(m)}:${pad(s)}`
  return `${pad(m)}:${pad(s)}`
}

function getScrollScore(total, elapsed) {
  return total * 10 + Math.floor(elapsed / 60) * 5
}

function getAvgPerMin(total, elapsed) {
  if (elapsed < 10) return '—'
  return ((total / elapsed) * 60).toFixed(1)
}

export default function StatsRow({ elapsed, total }) {
  const score = getScrollScore(total, elapsed)
  const avg = getAvgPerMin(total, elapsed)

  const stats = [
    { label: 'Session', value: formatTime(elapsed), icon: '⏱️' },
    { label: 'Score',   value: score,               icon: '🏆' },
    { label: 'Avg/min', value: avg,                 icon: '⚡' },
  ]

  return (
    <div className="flex gap-3">
      {stats.map((s, i) => (
        <motion.div
          key={s.label}
          className="glass rounded-2xl flex-1 py-3 px-2 flex flex-col items-center gap-1"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: i * 0.1 }}
        >
          <span className="text-base">{s.icon}</span>
          <span className="text-white font-bold text-sm tabular-nums">{s.value}</span>
          <span className="text-white/40 text-xs font-medium">{s.label}</span>
        </motion.div>
      ))}
    </div>
  )
}

