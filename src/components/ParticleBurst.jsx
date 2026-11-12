import { useEffect, useRef } from 'react'

const PARTICLE_COUNT = 20

function randomBetween(a, b) {
  return a + Math.random() * (b - a)
}

export default function ParticleBurst({ color }) {
  const containerRef = useRef(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const particles = []
    for (let i = 0; i < PARTICLE_COUNT; i++) {
      const el = document.createElement('div')
      const angle = (i / PARTICLE_COUNT) * Math.PI * 2
      const distance = randomBetween(60, 130)
      const size = randomBetween(4, 10)
      const tx = Math.cos(angle) * distance
      const ty = Math.sin(angle) * distance

      el.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        border-radius: 50%;
        background: ${color};
        top: 50%;
        left: 50%;
        margin-top: -${size / 2}px;
        margin-left: -${size / 2}px;
        pointer-events: none;
        box-shadow: 0 0 6px ${color};
      `

      // Animate with Web Animations API
      el.animate([
        { transform: 'translate(0, 0) scale(1)', opacity: 1 },
        { transform: `translate(${tx}px, ${ty}px) scale(0)`, opacity: 0 },
      ], {
        duration: randomBetween(400, 700),
        easing: 'cubic-bezier(0, .9, .57, 1)',
        fill: 'forwards',
      })

      container.appendChild(el)
      particles.push(el)
    }

    // Also spawn some stars/sparkles
    for (let i = 0; i < 8; i++) {
      const el = document.createElement('div')
      const angle = randomBetween(0, Math.PI * 2)
      const distance = randomBetween(30, 80)
      const size = randomBetween(3, 6)
      const tx = Math.cos(angle) * distance
      const ty = Math.sin(angle) * distance

      el.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        border-radius: 1px;
        background: white;
        top: 50%;
        left: 50%;
        margin-top: -${size / 2}px;
        margin-left: -${size / 2}px;
        pointer-events: none;
        transform: rotate(45deg);
      `

      el.animate([
        { transform: 'translate(0, 0) scale(1) rotate(0deg)', opacity: 1 },
        { transform: `translate(${tx}px, ${ty}px) scale(0) rotate(180deg)`, opacity: 0 },
      ], {
        duration: randomBetween(300, 600),
        easing: 'ease-out',
        fill: 'forwards',
        delay: randomBetween(0, 100),
      })

      container.appendChild(el)
      particles.push(el)
    }

    const cleanup = setTimeout(() => {
      particles.forEach(p => p.remove())
    }, 800)

    return () => {
      clearTimeout(cleanup)
      particles.forEach(p => p.remove())
    }
  }, [color])

  return (
    <div
      ref={containerRef}
      className="absolute pointer-events-none"
      style={{ width: 0, height: 0, top: '50%', left: '50%' }}
    />
  )
}

