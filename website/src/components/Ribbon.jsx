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

    // Halo first, then core. Widths and alphas chosen so the sum peaks a little
    // under 1 at the centre of a single band — the rest of the headroom is what
    // the crossings spend to reach white.
    // `fade` is where each pass reaches full strength along its own length. The
    // halo comes up later and dies earlier than the core, so it never sticks out
    // past the tip — a halo as long as the band is what turns a pointed lens
    // into a streak with blunt ends.
    const PASSES = [
      { width: 2.05, alpha: 0.28, fade: [0.14, 0.52, 0.9] },
      { width: 1, alpha: 0.7, fade: [0.2, 0.58, 1] },
      // The spine. Narrow and hot: five of these overlapping additively is what
      // makes the white core the reference has running through the middle,
      // rather than a white that only appears where two bands happen to cross.
      { width: 0.4, alpha: 0.36, fade: [0.26, 0.62, 0.99] },
    ]

    // The bands read as one body or as five streaks depending on how far apart
    // they sit at rest. Pulled in from their authored spread until they overlap
    // at the waist and only separate where the weave pushes them apart.
    const SPREAD = 0.82

    const TAU = Math.PI * 2
    let w = 0
    let h = 0
    // Segment count follows the canvas: a crest needs enough points to stay a
    // curve, and on a phone there are barely enough pixels for half as many.
    let steps = 88

    const resize = () => {
      const rect = wrap.getBoundingClientRect()

      // Resolution has to RISE as the element shrinks, which is the opposite of
      // the intuition. Drawing at 55% is free on a 1020px hero because the blur
      // hides the missing detail — but a phone's ribbon is a third that size, so
      // 55% of it left each band about three pixels tall, and there was no
      // detail left for the blur to soften. It read as a loading bar.
      const scale = rect.width < 560 ? 0.9 : 0.55
      steps = rect.width < 560 ? 56 : 88

      w = Math.max(1, Math.round(rect.width * scale))
      h = Math.max(1, Math.round(rect.height * scale))
      canvas.width = w
      canvas.height = h

      // And the blur has to follow the object rather than being a fixed number.
      // 9px over a 232px-tall hero is a soft edge; the same 9px over a 69px-tall
      // phone ribbon is wider than the bands themselves, which is what turned
      // five distinct ribbons into one grey smear.
      const blur = Math.min(13, Math.max(3.5, rect.height * 0.041))
      wrap.style.setProperty('--ribbon-blur', `${blur.toFixed(2)}px`)
    }

    resize()
    const observer = new ResizeObserver(resize)
    observer.observe(wrap)

    const draw = (t) => {
      ctx.clearRect(0, 0, w, h)
      // Additive: every crossing climbs towards white the way real light does,
      // so the bright core is produced by the weave rather than painted in.
      ctx.globalCompositeOperation = 'lighter'

      // ── The whole object's own motion ────────────────────────────────────
      //
      // The weave alone made five ribbons that happened to be near each other.
      // What the reference has on top of that is behaviour belonging to the
      // WHOLE form: it breathes, it leans, and its light swells. Those are
      // shared terms, which is what makes five paths read as one object —
      // and they run on their own long periods so the composite never lands
      // in the same state twice inside anyone's attention span.

      // Breath. Amplitude and thickness together, because a form that swells
      // without spreading reads as a zoom rather than an inhale.
      const breath = 0.84 + 0.16 * Math.sin((t / 13) * TAU)

      // Lean. A degree or two of shared tilt, slower than the breath.
      const tilt = 0.052 + Math.sin((t / 21) * TAU) * 0.038

      // Swell. The light gets brighter as the form opens, slightly out of
      // phase so the peak of the glow is not the peak of the shape.
      const bloom = 0.88 + 0.12 * Math.sin((t / 9.5) * TAU + 1.2)

      // Entrance. Cold-starting mid-motion looks like a video that was already
      // playing; this opens from a flat line over about a second and a half.
      const raw = Math.min(1, t / 1.6)
      const intro = raw * raw * (3 - 2 * raw)

      // The spine every band follows.
      //
      // Individual drift made the bands weave, which is right, but weaving alone
      // still reads as several ribbons sharing a neighbourhood. What the
      // reference has is ONE path with an S in it that everything rides — so a
      // band's own wander is a departure from a shared line rather than its
      // whole existence. Two components on unrelated periods so the S migrates
      // instead of standing still.
      const spine = (u) =>
        Math.sin(u * Math.PI * 0.92 + t * 0.17) * h * 0.055 +
        Math.sin(u * Math.PI * 1.9 - t * 0.11) * h * 0.021

      // The twist.
      //
      // A flat ribbon rotating in space goes thin where it turns edge-on, and
      // that pinch travels along its length. It is the single cue that separates
      // "a ribbon" from "a stack of glowing bars", and it costs one multiply.
      const pinch = (u, band) =>
        0.42 +
        0.58 * Math.abs(Math.sin(u * Math.PI * 0.85 + t * 0.13 + band.xphase * 0.12))

      bands.forEach((band, index) => {
        const [r, g, b] = rgb[index]

        // Where this band sits right now. The drift is the weave; the waves are
        // the flex along its own length; the tilt is shared, so the group still
        // reads as one ribbon rather than five unrelated streaks.
        //
        // Two waves, not one. A single sine is a shape a viewer solves in about
        // two seconds; a second, faster one at an unrelated phase travelling
        // the other way keeps the profile from ever being symmetrical.
        const centre = (u) =>
          h * 0.5 +
          spine(u) * intro +
          h * band.base * SPREAD * breath +
          (u - 0.5) * h * tilt +
          Math.sin((t / band.period) * TAU + band.phase) * h * band.drift * breath * intro +
          Math.sin(u * TAU * band.freq + band.xphase + t * band.speed) * h * 0.045 * intro +
          Math.sin(u * TAU * band.freq * 2.3 + band.xphase * 1.7 - t * band.speed * 0.63) *
            h *
            0.017 *
            intro

        // Lens: full through the middle, brought to a point at each end. The
        // exponent is what makes the tips fine rather than blunt.
        // The exponent is the tip. Below 1 the lens stays wide almost to the end
        // and finishes on a blunt edge; above it the profile falls away fast and
        // the band comes to a point, which is what the reference does.
        const half = (u) =>
          Math.sin(Math.PI * u) ** 0.86 *
          h *
          band.thick *
          pinch(u, band) *
          (0.32 + 0.68 * breath) *
          intro

        // Drawn twice: a wide, faint halo and a narrow core inside it. One pass
        // at a single width gives a band with an edge; light does not have an
        // edge, it has a bright middle that falls away, and stacking the two
        // additively is what produces that without a second blur pass.
        for (const pass of PASSES) {
          const gradient = ctx.createLinearGradient(0, 0, w, 0)
          const peak = band.alpha * pass.alpha * bloom * intro
          const [rise, crest, end] = pass.fade
          gradient.addColorStop(0, `rgba(${r},${g},${b},0)`)
          gradient.addColorStop(rise, `rgba(${r},${g},${b},${peak * 0.5})`)
          gradient.addColorStop(crest, `rgba(${r},${g},${b},${peak})`)
          gradient.addColorStop(end, `rgba(${r},${g},${b},0)`)
          if (end < 1) gradient.addColorStop(1, `rgba(${r},${g},${b},0)`)
          ctx.fillStyle = gradient

          ctx.beginPath()
          for (let i = 0; i <= steps; i += 1) {
            const u = i / steps
            const y = centre(u) - half(u) * pass.width
            if (i === 0) ctx.moveTo(u * w, y)
            else ctx.lineTo(u * w, y)
          }
          for (let i = steps; i >= 0; i -= 1) {
            const u = i / steps
            ctx.lineTo(u * w, centre(u) + half(u) * pass.width)
          }
          ctx.closePath()
          ctx.fill()
        }
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

    // A hidden tab still services rAF in some browsers, and a background tab
    // painting a canvas nobody is looking at is pure battery.
    const onVisibility = () => {
      if (document.hidden) {
        if (frame) cancelAnimationFrame(frame)
        frame = 0
      } else if (onScreen && !frame && !prefersReducedMotion()) {
        last = performance.now()
        frame = requestAnimationFrame(tick)
      }
    }
    document.addEventListener('visibilitychange', onVisibility)

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
      document.removeEventListener('visibilitychange', onVisibility)
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
