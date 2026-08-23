import { useEffect, useRef } from 'react'
import { PauseIcon, PlayIcon } from '../icons.jsx'
import { prefersReducedMotion } from '../hooks.js'

/**
 * The hero object — five ribbons of light that weave, drawn on canvas.
 *
 * ### Why not CSS
 *
 * The first version was blurred ellipses on CSS keyframes, and it read as a
 * lava lamp no matter how the numbers were tuned. The reason is structural: an
 * ellipse can only translate, rotate and scale, so the *shape* never changes —
 * it can drift, but it cannot undulate. The reference is a video of something
 * genuinely fluid, and the eye reads the difference immediately.
 *
 * What is drawn is five lens-shaped ribbons — pointed at both ends, fullest in
 * the middle — that WEAVE through one another on unrelated periods, so the
 * color order keeps inverting the way the reference's does.
 *
 * ### How it stays cheap
 *
 * The canvas is drawn CRISP at 55% scale with `lighter` compositing, and the
 * blur is a CSS filter on the element. Blurring inside the 2D context
 * (`ctx.filter`) re-runs a convolution per path per frame on the CPU; doing it
 * on the element is one GPU pass on an already-composited layer, and since the
 * result is blurred anyway the missing resolution is invisible. The loop stops
 * entirely when the hero is off screen or the user pauses it.
 */
export default function Ribbon({ playing, onToggle }) {
  const canvasRef = useRef(null)
  const wrapRef = useRef(null)
  const playingRef = useRef(playing)
  playingRef.current = playing

  useEffect(() => {
    const canvas = canvasRef.current
    const wrap = wrapRef.current
    if (!canvas || !wrap) return

    const ctx = canvas.getContext('2d', { alpha: true })
    if (!ctx) return

    const styles = getComputedStyle(document.documentElement)
    const hue = (name, fallback) =>
      styles.getPropertyValue(name).trim() || fallback

    // Separate ribbons that WEAVE through each other.
    //
    // Three attempts to get here. Blurred CSS ellipses could drift but never
    // change shape. Parallel wavy bands could flex but stayed stacked in the
    // same order forever. One shared centreline made a single coherent body —
    // and locked the colors into a fixed spectrum, which the reference plainly
    // is not: watch it for four seconds and blue is above red, then red is
    // above blue, then green is above both. The bands pass THROUGH one another.
    //
    // So each band is its own lens with its own slow vertical drift on an
    // unrelated period. They cross because their drifts are out of phase, the
    // crossings are what produce the white, and the pattern never repeats
    // inside anyone's attention span because the periods share no factors.
    // `base` is where a band lives when its drift is at zero; `drift` is how far
    // it wanders from there. Both matter: with base alone the order can never
    // change, and with drift alone all five pile onto the same line and the
    // additive blending sums the whole ribbon to white. Together they keep a
    // cool-above-warm tendency that the weave is free to invert.
    const bands = [
      { css: hue('--iris', '#7b6cff'), base: -0.1, drift: 0.105, period: 9, phase: 0, freq: 1, xphase: 0, speed: 0.16, thick: 0.095, alpha: 0.8 },
      { css: hue('--cyan', '#38d3d6'), base: -0.05, drift: 0.095, period: 11.5, phase: 1.7, freq: 1.25, xphase: 1.1, speed: -0.13, thick: 0.078, alpha: 0.7 },
      { css: '#fff2dc', base: 0, drift: 0.055, period: 7.5, phase: 3.1, freq: 1.1, xphase: 2.4, speed: 0.1, thick: 0.055, alpha: 0.55 },
      { css: hue('--ember', '#ff8a6b'), base: 0.05, drift: 0.095, period: 13, phase: 4.4, freq: 1.35, xphase: 3.3, speed: -0.17, thick: 0.078, alpha: 0.7 },
      { css: hue('--rose', '#ed5564'), base: 0.1, drift: 0.105, period: 10, phase: 5.6, freq: 0.95, xphase: 4.9, speed: 0.14, thick: 0.095, alpha: 0.8 },
    ]

    const parse = (css) => {
      const hex = css.replace('#', '').trim()
      const full = hex.length === 3 ? hex.split('').map((c) => c + c).join('') : hex
      return [
        parseInt(full.slice(0, 2), 16),
        parseInt(full.slice(2, 4), 16),
        parseInt(full.slice(4, 6), 16),
      ]
    }

    const rgb = bands.map((band) => parse(band.css))

    const SCALE = 0.55
    const STEPS = 52
    let w = 0
    let h = 0

    const resize = () => {
      const rect = wrap.getBoundingClientRect()
      w = Math.max(1, Math.round(rect.width * SCALE))
      h = Math.max(1, Math.round(rect.height * SCALE))
      canvas.width = w
      canvas.height = h
    }

    resize()
    const observer = new ResizeObserver(resize)
    observer.observe(wrap)

    const draw = (t) => {
      ctx.clearRect(0, 0, w, h)
      // Additive: every crossing climbs towards white the way real light does,
      // so the bright core is produced by the weave rather than painted in.
      ctx.globalCompositeOperation = 'lighter'

      bands.forEach((band, index) => {
        const [r, g, b] = rgb[index]

        // Where this band sits right now. The drift is the weave; the wave is
        // the flex along its own length; the tilt is shared, so the group still
        // reads as one ribbon rather than five unrelated streaks.
        const centre = (u) =>
          h * 0.5 +
          h * band.base +
          (u - 0.5) * h * 0.08 +
          Math.sin((t / band.period) * Math.PI * 2 + band.phase) * h * band.drift +
          Math.sin(u * Math.PI * 2 * band.freq + band.xphase + t * band.speed) * h * 0.045

        // Lens: full through the middle, brought to a point at each end.
        const half = (u) => Math.sin(Math.PI * u) ** 0.6 * h * band.thick

        const gradient = ctx.createLinearGradient(0, 0, w, 0)
        gradient.addColorStop(0, `rgba(${r},${g},${b},0)`)
        gradient.addColorStop(0.22, `rgba(${r},${g},${b},0.5)`)
        gradient.addColorStop(0.58, `rgba(${r},${g},${b},${band.alpha})`)
        gradient.addColorStop(1, `rgba(${r},${g},${b},0)`)
        ctx.fillStyle = gradient

        ctx.beginPath()
        for (let i = 0; i <= STEPS; i += 1) {
          const u = i / STEPS
          const y = centre(u) - half(u)
          if (i === 0) ctx.moveTo(u * w, y)
          else ctx.lineTo(u * w, y)
        }
        for (let i = STEPS; i >= 0; i -= 1) {
          const u = i / STEPS
          ctx.lineTo(u * w, centre(u) + half(u))
        }
        ctx.closePath()
        ctx.fill()
      })

      ctx.globalCompositeOperation = 'source-over'
    }

    let frame = 0
    let clock = 0
    let last = performance.now()
    let onScreen = true

    const tick = (now) => {
      const dt = Math.min(0.05, (now - last) / 1000)
      last = now
      if (playingRef.current) clock += dt
      draw(clock)
      frame = onScreen ? requestAnimationFrame(tick) : 0
    }

    // Reduced motion gets one frame of the shape, never a loop.
    if (prefersReducedMotion()) {
      draw(0)
    } else {
      frame = requestAnimationFrame(tick)
    }

    const io = new IntersectionObserver(
      ([entry]) => {
        onScreen = entry.isIntersecting
        if (onScreen && !frame && !prefersReducedMotion()) {
          last = performance.now()
          frame = requestAnimationFrame(tick)
        }
      },
      { rootMargin: '80px' },
    )
    io.observe(wrap)

    return () => {
      if (frame) cancelAnimationFrame(frame)
      observer.disconnect()
      io.disconnect()
    }
  }, [])

  return (
    <div className="ribbon-wrap">
      <div className="ribbon" ref={wrapRef}>
        <canvas className="ribbon-canvas" ref={canvasRef} aria-hidden="true" />
      </div>

      <button
        type="button"
        className="ambient-toggle"
        onClick={onToggle}
        aria-label={playing ? 'Pause the animation' : 'Play the animation'}
      >
        {playing ? <PauseIcon /> : <PlayIcon />}
      </button>
    </div>
  )
}
