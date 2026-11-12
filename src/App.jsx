import { useState, useEffect, useRef } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import PlatformTabs from './components/PlatformTabs'
import CounterDisplay from './components/CounterDisplay'
import AddButton from './components/AddButton'
import StatsRow from './components/StatsRow'
import ResetButton from './components/ResetButton'
import MilestoneToast from './components/MilestoneToast'
import ParticleBurst from './components/ParticleBurst'
import TopPill from './components/TopPill'

const PLATFORMS = [
  { id: 'reels',  label: 'Reels',  emoji: '🎬', color: '#E1306C' },
  { id: 'shorts', label: 'Shorts', emoji: '🔴', color: '#FF0000' },
  { id: 'tiktok', label: 'TikTok', emoji: '🎵', color: '#69C9D0' },
]

const MILESTONES = [
  { count: 10,  msg: "10 reels! You're just warming up 👀",      emoji: '👀' },
  { count: 25,  msg: "25 reels!! Ok you're hooked 😅",           emoji: '😅' },
  { count: 50,  msg: "50 REELS!! Skill issue confirmed 😂",       emoji: '😂' },
  { count: 100, msg: "100 REELS?! Put the phone down 💀",         emoji: '💀' },
  { count: 200, msg: "200... there is literally no hope 🫡",      emoji: '🫡' },
  { count: 500, msg: "500 REELS. You are the main character 🏆", emoji: '🏆' },
]

function getBgGradient(total) {
  if (total < 10)  return 'linear-gradient(160deg, #0f0c29 0%, #302b63 50%, #24243e 100%)'
  if (total < 25)  return 'linear-gradient(160deg, #0f2027 0%, #203a43 50%, #2c5364 100%)'
  if (total < 50)  return 'linear-gradient(160deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)'
  if (total < 100) return 'linear-gradient(160deg, #200122 0%, #6f0000 50%, #200122 100%)'
  if (total < 200) return 'linear-gradient(160deg, #3d0000 0%, #c94040 50%, #3d0000 100%)'
  return 'linear-gradient(160deg, #ff416c 0%, #ff4b2b 50%, #ff416c 100%)'
}

export default function App() {
  const [counts, setCounts]                       = useState({ reels: 0, shorts: 0, tiktok: 0 })
  const [active, setActive]                       = useState('reels')
  const [elapsed, setElapsed]                     = useState(0)
  const [showBurst, setShowBurst]                 = useState(false)
  const [burstKey, setBurstKey]                   = useState(0)
  const [milestone, setMilestone]                 = useState(null)
  const [triggeredMilestones, setTriggeredMilestones] = useState(new Set())
  const [pillPulse, setPillPulse]                 = useState(false)
  const startTimeRef = useRef(Date.now())

  const total = counts.reels + counts.shorts + counts.tiktok

  useEffect(() => {
    const id = setInterval(() => {
      setElapsed(Math.floor((Date.now() - startTimeRef.current) / 1000))
    }, 1000)
    return () => clearInterval(id)
  }, [])

  const handleAdd = () => {
    const newCounts = { ...counts, [active]: counts[active] + 1 }
    setCounts(newCounts)

    // pill pulse
    setPillPulse(true)
    setTimeout(() => setPillPulse(false), 400)

    // burst
    setShowBurst(true)
    setBurstKey(k => k + 1)
    setTimeout(() => setShowBurst(false), 700)

    // milestones
    const newTotal = newCounts.reels + newCounts.shorts + newCounts.tiktok
    for (const m of MILESTONES) {
      if (newTotal === m.count && !triggeredMilestones.has(m.count)) {
        setMilestone(m)
        setTriggeredMilestones(prev => new Set([...prev, m.count]))
        break
      }
    }
  }

  const handleReset = () => {
    setCounts({ reels: 0, shorts: 0, tiktok: 0 })
    setElapsed(0)
    startTimeRef.current = Date.now()
    setTriggeredMilestones(new Set())
  }

  const activePlatform = PLATFORMS.find(p => p.id === active)
  const bgGradient     = getBgGradient(total)

  return (
    <motion.div
      className="relative h-screen w-screen flex flex-col items-center overflow-hidden select-none"
      animate={{ background: bgGradient }}
      transition={{ duration: 1.5, ease: 'easeInOut' }}
      style={{ background: bgGradient }}
    >
      {/* Background orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <motion.div
          className="absolute w-96 h-96 rounded-full blur-3xl opacity-20"
          style={{ background: activePlatform.color, top: '-10%', left: '-10%' }}
          animate={{ x: [0, 30, 0], y: [0, 20, 0] }}
          transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
        />
        <motion.div
          className="absolute w-96 h-96 rounded-full blur-3xl opacity-15"
          style={{ background: activePlatform.color, bottom: '-10%', right: '-10%' }}
          animate={{ x: [0, -30, 0], y: [0, -20, 0] }}
          transition={{ duration: 10, repeat: Infinity, ease: 'easeInOut' }}
        />
      </div>

      {/* ── TOP PILL ── */}
      <TopPill
        total={total}
        platform={activePlatform}
        pulse={pillPulse}
        elapsed={elapsed}
      />

      {/* ── DOWNLOAD NATIVE ANDROID APP BUTTON ── */}
      <motion.a
        href="/scrolle.apk"
        download="scrolle.apk"
        whileTap={{ scale: 0.94 }}
        whileHover={{ scale: 1.02 }}
        className="relative z-20 mt-1 mb-2 px-5 py-2.5 rounded-full flex items-center justify-center gap-2 text-white font-bold text-xs tracking-wide shadow-xl border border-white/20 transition-all no-underline"
        style={{
          background: 'linear-gradient(90deg, #E1306C, #833AB4)',
          boxShadow: '0 4px 18px rgba(225, 48, 108, 0.45)'
        }}
      >
        <span className="text-sm">📥</span>
        <span>Download Android App (.APK)</span>
      </motion.a>

      {/* App name */}
      <div className="relative z-10 w-full px-6 pt-3 pb-1 text-center">
        <motion.h1
          className="text-white font-black text-xl tracking-tight"
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          scrolle <span className="text-base">📱</span>
        </motion.h1>
      </div>

      {/* Platform Tabs */}
      <div className="relative z-10 w-full px-4 mt-1">
        <PlatformTabs
          platforms={PLATFORMS}
          active={active}
          counts={counts}
          onSelect={setActive}
        />
      </div>

      {/* Counter */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center w-full">
        <CounterDisplay count={counts[active]} total={total} platform={activePlatform} />
        <div className="relative">
          {showBurst && <ParticleBurst key={burstKey} color={activePlatform.color} />}
        </div>
      </div>

      {/* Add Button */}
      <div className="relative z-10 mb-4">
        <AddButton onAdd={handleAdd} platform={activePlatform} />
      </div>

      {/* Stats */}
      <div className="relative z-10 w-full px-4 mb-3">
        <StatsRow elapsed={elapsed} total={total} />
      </div>

      {/* Reset */}
      <div className="relative z-10 mb-6">
        <ResetButton onReset={handleReset} />
      </div>

      {/* Milestone Toast */}
      <AnimatePresence>
        {milestone && (
          <MilestoneToast milestone={milestone} onDismiss={() => setMilestone(null)} />
        )}
      </AnimatePresence>
    </motion.div>
  )
}
