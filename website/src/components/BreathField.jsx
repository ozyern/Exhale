import { useEffect, useRef } from 'react'
import { prefersReducedMotion } from '../hooks.js'

/**
 * The build number, drawn as a sky.
 *
 * Not type with an effect on it. The digits are a mask, sampled into points,
 * and every point is then a star with its own magnitude, color and orbit — so
 * the number is only ever *almost* there. It tightens into itself on the
 * inhale and loosens on the exhale, which makes it read as something being
 * held together rather than as a word set in a font.
 *
 * The breath is the app's own: four seconds in, six seconds out, the numbers
 * off the pacer hidden behind seven taps on the mark in About. Given what the
 * app is called, the number announcing a release of it should breathe.
 *
 * Four things do the work, and leaving any of them out is what makes this kind
 * of thing look cheap:
 *
 *   · **Chains, not a filled stencil.** The one that matters most, and the
 *     reason is the subject. The obvious reference for this sort of thing is a
 *     spiral galaxy resolving into a 6, and that works because a 6 *is* a
 *     spiral — the glyph and the natural form are the same shape. 203 gets no
 *     such help, so the flow has to be built. A distance transform says how
 *     deep inside the stroke each pixel is; sampling concentrates hard on the
 *     ridge, so every stroke is a chain rather than a filled region, and each
 *     star's bloom is stretched along the local stroke direction so
 *     neighbours merge into a continuous ribbon of light instead of staying a
 *     row of separate dots. Fill a letterform evenly and you get dots in the
 *     shape of a letter; run them down the spine and blur them along it and
 *     you get an arm of something.
 *   · **Magnitude.** A cube law, so most points are a pixel and a few are the
 *     size of a fingernail. An even scatter of mid-size dots is confetti.
 *   · **Two passes per star.** A sharp core and, under it, a bloom several
 *     times its width at a tenth of its brightness. One soft blob can be
 *     either a small bright star or a large dim one; a core with a halo is
 *     unambiguously a bright thing seen through air.
 *   · **A nebula on the spines.** Large, very faint smears placed only where
 *     the stroke is thickest, so the stars sit *in* a continuous band of haze
 *     rather than floating in a row.
 *
 * Canvas, because none of that is a shape CSS has. Everything is blitted from
 * eight pre-rendered sprites with `lighter` compositing, so a frame is a few
 * thousand small image draws and not one gradient — and it stops entirely when
 * it scrolls away, when the tab hides, or when the reader asked for less motion.
 */

/** How many stars the number is made of, in landscape. */
const COUNT = 1750

/** Large faint smears under it, on the same points. */
const NEBULA = 240

/** Points forced to maximum magnitude, spread along the spines. */
const HOTSPOTS = 26

/** And a few far beyond that: the handful of things you see before anything else. */
const GIANTS = 7

/**
 * Stars that are not part of the number.
 *
 * A lit object needs somewhere to be. The portrait figure is much more of it
 * than the landscape one is, and the sky around it has to keep up or the
 * screen is a number in a void — so this scales with how much room is left.
 */
const DUST_LANDSCAPE = 150
const DUST_PORTRAIT = 320

const TAU = Math.PI * 2

/** Four seconds in, six seconds out. The app's numbers, unchanged. */
const INHALE = 4
const EXHALE = 6
const BREATH = INHALE + EXHALE

/**
 * The entrance.
 *
 * The number does not appear; it assembles. Stars fade up scattered across the
 * whole frame, hang there for a moment, and then draw together into the shape
 * — so the first thing the page does is form its own title in front of you.
 *
 * This is the whole difference between an image that happens to move and
 * something that reads as alive: a field that is simply *there* on load has no
 * first moment, and every viewer's first moment is the one they judge it by.
 *
 * `HOLD` is the pause before anything travels. Without it the stars fade in
 * already moving, and the scatter never registers as a state of its own.
 */
const FADE = 0.9
const HOLD = 0.85
const ASSEMBLE = 4.4

/** Extra seconds spread across the field, so it settles from the middle out. */
const SPREAD = 0.8

/**
 * The raster the glyph is sampled out of, in each orientation.
 *
 * Two of them, because a three-digit number is about three units wide by one
 * tall and a phone held upright is one by two. Scaled to fit, that number is a
 * thin bright band across the middle of a large black rectangle — which is the
 * single thing that makes an effect like this look like a placeholder. So in
 * portrait the digits are set one per line instead, and the figure becomes
 * taller than it is wide, which is the shape the screen actually is.
 */
const MASK = {
  wide: { w: 1200, h: 460, step: 1, track: 26 },
  tall: { w: 520, h: 1320, step: 1, track: 0 },
}

/**
 * Star colors, not brand colors.
 *
 * This is the one object on the site that does not take its palette from the
 * album swatches, and the reason is that it is a sky: hot stars are blue-white
 * and cool ones are amber, and a sky tinted rose and iris instead reads as
 * confetti rather than as distance. The amber end is the app's own `--ember`,
 * which is close enough to a K-type star to be both at once.
 */
const MIX = [
  { pick: 0.44, css: '#eef3ff' },
  { pick: 0.7, css: '#b6cdff' },
  { pick: 0.88, css: '#ffe3bb' },
  { pick: 1.0, css: 'var(--ember)', fallback: '#ff8a6b' },
]

const easeInOut = (x) => (x < 0.5 ? 2 * x * x : 1 - (-2 * x + 2) ** 2 / 2)

/** Slow away, slow home, and no corner at either end. */
const smoother = (x) => x * x * x * (x * (x * 6 - 15) + 10)

const clamp01 = (x) => (x < 0 ? 0 : x > 1 ? 1 : x)

/** Box–Muller, so nothing in here is ever evenly spread. */
function gauss() {
  let u = 0
  let v = 0
  while (u === 0) u = Math.random()
  while (v === 0) v = Math.random()
  return Math.sqrt(-2 * Math.log(u)) * Math.cos(TAU * v)
}

function parse(hex) {
  const raw = hex.replace('#', '').trim()
  const full = raw.length === 3 ? raw.split('').map((c) => c + c).join('') : raw
  return [
    parseInt(full.slice(0, 2), 16),
    parseInt(full.slice(2, 4), 16),
    parseInt(full.slice(4, 6), 16),
  ]
}

/**
 * Two sprites per color.
 *
 * `core` is nearly a disc: opaque to a sixth of its radius and gone by a
 * third, so at small sizes it lands as an actual point of light rather than a
 * smudge. `halo` never gets above twelve percent and reaches the full radius,
 * which is the part that makes the core look bright.
 */
function sprite(hex, core) {
  const size = 64
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  const [r, g, b] = parse(hex)
  // Built from numbers rather than from a CSS string, because `addColorStop`
  // takes a colour and not an expression — `color-mix()` in here is silently
  // the wrong colour in some engines and a thrown error in others.
  const at = (a) => `rgba(${r},${g},${b},${a})`
  const grad = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)

  if (core) {
    grad.addColorStop(0, 'rgba(255,255,255,1)')
    grad.addColorStop(0.16, at(1))
    grad.addColorStop(0.34, at(0.34))
    grad.addColorStop(1, at(0))
  } else {
    grad.addColorStop(0, at(0.55))
    grad.addColorStop(0.42, at(0.12))
    grad.addColorStop(1, at(0))
  }

  ctx.fillStyle = grad
  ctx.fillRect(0, 0, size, size)
  return canvas
}

/** Cheap two-frequency noise. Only ever asked for a number between 0 and 1. */
const clumpAt = (x, y) =>
  0.5 +
  0.25 * Math.sin(x * 21.3 + y * 13.7) +
  0.25 * Math.sin(x * 41.1 - y * 31.9 + 1.7)

/**
 * The number, as points.
 *
 * Rasterised at a fixed size and returned normalised against the ink rather
 * than against the canvas, so `span` on screen is the width of the number
 * itself and the arrangement is identical on a phone and on a 5K display.
 * Sampled on a grid and then jittered, because a grid you can see is worse
 * than no structure at all — this has to look like a sky that happens to be a
 * number, not a halftone screen of one.
 *
 * `tall` sets the digits one per line. See `MASK`.
 */
function glyphPoints(text, tall) {
  const spec = tall ? MASK.tall : MASK.wide
  const { w: MW, h: MH, step: STEP } = spec

  const canvas = document.createElement('canvas')
  canvas.width = MW
  canvas.height = MH
  const ctx = canvas.getContext('2d', { willReadFrequently: true })

  ctx.fillStyle = '#fff'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  // Tracked out in one line, because three digits set solid become one cloud
  // with two notches in it. Stacked, the leading does that job instead.
  if ('letterSpacing' in ctx) ctx.letterSpacing = `${spec.track}px`

  // Fitted rather than guessed: the number can change length between releases
  // and this should not need a new magic number when it does.
  const face = '"SF Pro Display", -apple-system, Inter, "Segoe UI", sans-serif'
  const glyphs = [...text]
  const lines = tall ? glyphs : [text]
  // Tight when stacked. The leading is the only thing standing between the
  // digits and the edges of the frame, and every point of it spent on air is a
  // point the digits themselves do not get.
  const lead = tall ? 0.9 : 1.02

  let size = tall ? Math.round((MH * 0.94) / (lines.length * lead)) : 420
  ctx.font = `500 ${size}px ${face}`
  const widest = Math.max(...lines.map((line) => ctx.measureText(line).width), 1)
  size = Math.round(size * Math.min(1, (MW * (tall ? 0.9 : 0.86)) / widest))
  ctx.font = `500 ${size}px ${face}`

  const block = (lines.length - 1) * size * lead
  lines.forEach((line, i) => {
    ctx.fillText(line, MW / 2, MH / 2 - block / 2 + i * size * lead)
  })

  const data = ctx.getImageData(0, 0, MW, MH).data

  // How deep inside the stroke every pixel is.
  //
  // A two-pass chamfer transform: background starts at zero, ink starts at
  // infinity, and each pass takes the cheapest route to a background pixel
  // from the three neighbours behind it. Weights of 3 orthogonally and 4
  // diagonally approximate a Euclidean distance closely enough for something
  // that is about to be scattered anyway, and it is two linear passes rather
  // than a search per pixel.
  const N = MW * MH
  const dist = new Int32Array(N)
  const BIG = 1 << 28
  for (let i = 0; i < N; i += 1) dist[i] = data[i * 4 + 3] > 140 ? BIG : 0

  for (let y = 1; y < MH; y += 1) {
    const row = y * MW
    for (let x = 1; x < MW - 1; x += 1) {
      const i = row + x
      if (dist[i] === 0) continue
      const up = i - MW
      let best = dist[i - 1] + 3
      if (dist[up] + 3 < best) best = dist[up] + 3
      if (dist[up - 1] + 4 < best) best = dist[up - 1] + 4
      if (dist[up + 1] + 4 < best) best = dist[up + 1] + 4
      if (best < dist[i]) dist[i] = best
    }
  }
  for (let y = MH - 2; y >= 0; y -= 1) {
    const row = y * MW
    for (let x = MW - 2; x >= 1; x -= 1) {
      const i = row + x
      if (dist[i] === 0) continue
      const down = i + MW
      let best = dist[i + 1] + 3
      if (dist[down] + 3 < best) best = dist[down] + 3
      if (dist[down + 1] + 4 < best) best = dist[down + 1] + 4
      if (dist[down - 1] + 4 < best) best = dist[down - 1] + 4
      if (best < dist[i]) dist[i] = best
    }
  }

  let deepest = 1
  for (let i = 0; i < N; i += 1) {
    if (dist[i] < BIG && dist[i] > deepest) deepest = dist[i]
  }

  const hits = []
  let minX = MW
  let maxX = 0
  let minY = MH
  let maxY = 0
  for (let y = 1; y < MH - 1; y += STEP) {
    for (let x = 1; x < MW - 1; x += STEP) {
      const i = y * MW + x
      if (dist[i] === 0) continue

      // 0 at the very edge of a stroke, 1 down its middle.
      const depth = Math.min(1, dist[i] / deepest)
      // Concentrated hard on the ridge. A fourth power leaves the rim almost
      // empty and piles nearly everything onto the middle of the stroke, which
      // is the difference between a chain of light and a filled shape. The few
      // percent that do survive out at the edge are what keeps the number's
      // outline from looking cut with a knife.
      if (Math.random() > 0.025 + 0.975 * depth ** 3.6) continue

      // Which way the stroke runs, here.
      //
      // The distance field climbs fastest straight across a stroke, so its
      // gradient is the normal and the perpendicular is the tangent. Each
      // star's bloom is stretched along it, which is what lets neighbours run
      // together into a ribbon instead of staying a row of dots.
      const gx = dist[i + 1] - dist[i - 1]
      const gy = dist[i + MW] - dist[i - MW]
      const len = Math.hypot(gx, gy) || 1
      const tx = -gy / len
      const ty = gx / len

      hits.push([x + gauss() * 1.5, y + gauss() * 1.5, depth, tx, ty])
      if (x < minX) minX = x
      if (x > maxX) maxX = x
      if (y < minY) minY = y
      if (y > maxY) maxY = y
    }
  }

  const bw = Math.max(1, maxX - minX)
  const bh = Math.max(1, maxY - minY)
  const midX = (minX + maxX) / 2
  const midY = (minY + maxY) / 2
  // Divided by the same number in both axes, so the number is never stretched.
  for (let i = 0; i < hits.length; i += 1) {
    hits[i][0] = (hits[i][0] - midX) / bw
    hits[i][1] = (hits[i][1] - midY) / bw
  }

  // Fisher–Yates, so thinning takes points from all over the number rather
  // than lopping the bottom off it.
  for (let i = hits.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    const swap = hits[i]
    hits[i] = hits[j]
    hits[j] = swap
  }

  // How tall the ink is per unit of its width. `measure` needs it to fit the
  // figure to the frame without ever guessing at the shape of the glyphs.
  return { hits, ratio: bh / bw }
}

export default function BreathField({ text = '203', tall = false, replayKey = 0 }) {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d', { alpha: true })
    if (!ctx) return

    const styles = getComputedStyle(document.documentElement)
    const resolve = (entry) =>
      entry.css.startsWith('var(')
        ? styles.getPropertyValue(entry.css.slice(4, -1)).trim() || entry.fallback
        : entry.css

    const colors = MIX.map(resolve)
    const cores = colors.map((css) => sprite(css, true))
    const halos = colors.map((css) => sprite(css, false))
    const roll = () => {
      const r = Math.random()
      const i = MIX.findIndex((entry) => r <= entry.pick)
      return i < 0 ? 0 : i
    }

    let motes = []
    let nebula = []
    let dust = []
    let ratio = 1
    // Characters across a line. One in portrait, all of them in landscape --
    // it is what says how wide a single stroke is relative to the figure.
    let perLine = 1

    /** Everything that depends on which way up the screen is. */
    const build = (stacked) => {
      const glyph = glyphPoints(text, stacked)
      const hits = glyph.hits
      ratio = glyph.ratio
      perLine = stacked ? 1 : [...text].length

      // Stacked, the figure is taller and narrower, so it needs fewer points
      // to read at the same density — and it is usually on a phone.
      const count = Math.min(hits.length, stacked ? 1700 : COUNT)

      motes = hits.slice(0, count).map(([x, y, depth, tx, ty]) => {
        // Cube law. Most points are a pixel; one in fifty is a real star. Then
        // weighted by depth, so the big ones sit on the spine and the rim gets
        // pinpricks.
        const mag = Math.random() ** 3.1 * (0.28 + depth * 1.05)
        // Uneven along the stroke, the way star formation is. A number filled
        // at one density is a stencil, and reads like one.
        const clump = clumpAt(x, y) ** 1.6
        const tint = roll()
        return {
          x,
          y,
          depth,
          mag,
          // Its own orbit and its own rate. Shared ones make the whole number
          // shimmer in step, which reads as a compression artefact.
          phase: Math.random() * TAU,
          rate: 0.3 + Math.random() * 0.5,
          // The rim wanders further than the spine, so the number frays at its
          // edges on the exhale instead of coming apart down the middle.
          wander: (0.3 + Math.random() ** 2 * 1.9) * (1.5 - depth),
          tx,
          ty,
          // Where this star waits before it is called in. Across the whole
          // frame, in frame coordinates rather than figure coordinates,
          // because the scatter belongs to the screen and the shape does not.
          sx: Math.random(),
          sy: Math.random(),
          // Inner stars arrive first. A field that lands all at once is a
          // cross-fade; one that settles from the middle out is a thing
          // coming together.
          delay: Math.min(1, Math.hypot(x, y) / 0.55) * SPREAD,
          core: cores[tint],
          halo: halos[tint],
          size: (0.5 + mag * 8) * (0.62 + clump * 0.95),
          glow: (0.26 + mag * 1.05) * (0.55 + clump * 0.68) * (0.5 + depth * 0.72),
          hot: false,
          px: 0,
          py: 0,
        }
      })

      // A handful of proper stars. Every sky has a few things brighter than
      // everything else in it, and without them a field of even magnitudes
      // reads as texture rather than as distance.
      const spine = motes.filter((m) => m.depth > 0.62)
      const chosen = []
      for (let n = 0; n < HOTSPOTS * 14 && chosen.length < HOTSPOTS; n += 1) {
        const m = spine[Math.floor(Math.random() * spine.length)]
        if (!m || m.hot) continue
        // Kept apart. Two of these landing next to each other stop being two
        // stars and become one bright smear, which is the exact thing the
        // magnitude spread was for.
        if (chosen.some((c) => Math.hypot(c.x - m.x, c.y - m.y) < 0.075)) continue
        m.hot = true
        m.mag = 0.88 + Math.random() * 0.12
        m.size = 4.5 + Math.random() * 2.5
        m.glow = 0.95
        m.wander *= 0.35
        chosen.push(m)
      }

      // And a handful beyond even those. The eye finds the brightest thing in
      // a frame first and everything else second; without something to be
      // found first, a field of near-equal stars is a texture. Kept well
      // apart, and kept still, because they are the anchors.
      for (let n = 0; n < GIANTS * 20 && n < 900; n += 1) {
        const m = chosen[Math.floor(Math.random() * chosen.length)]
        if (!m || m.giant) continue
        const giants = chosen.filter((c) => c.giant)
        if (giants.length >= GIANTS) break
        if (giants.some((g) => Math.hypot(g.x - m.x, g.y - m.y) < 0.2)) continue
        m.giant = true
        m.size = 8 + Math.random() * 4
        m.glow = 1
        m.wander *= 0.25
      }

      // The band the stars sit in. Placed only where the stroke is thickest,
      // so the haze runs down the middle of each one as a ribbon instead of
      // pooling in a rectangle around the whole number.
      const deep = hits.filter((hit) => hit[2] > 0.5)
      nebula = Array.from({ length: NEBULA }, () => {
        const hit = deep[Math.floor(Math.random() * deep.length)] ?? [0, 0]
        return {
          x: hit[0],
          y: hit[1],
          size: 30 + Math.random() ** 2 * 78,
          glow: 0.014 + Math.random() ** 2 * 0.05,
          phase: Math.random() * TAU,
          halo: halos[Math.random() < 0.82 ? 0 : 1 + ((Math.random() * 3) | 0)],
        }
      })

      dust = Array.from({ length: stacked ? DUST_PORTRAIT : DUST_LANDSCAPE }, () => {
        const mag = Math.random() ** 3.4
        return {
          x: Math.random(),
          y: Math.random(),
          size: 0.55 + mag * 3.4,
          glow: 0.05 + mag * 0.5,
          phase: Math.random() * TAU,
          core: cores[Math.random() < 0.8 ? 0 : roll()],
        }
      })
    }

    let w = 0
    let h = 0
    let span = 0
    let dpr = 1

    const measure = () => {
      const rect = canvas.getBoundingClientRect()
      // Capped at 1.5 rather than 2. This is a few thousand soft blits on a
      // full-screen layer; the extra pixels buy nothing on something with no
      // edges, and they cost a third more fill on every frame.
      dpr = Math.min(1.5, window.devicePixelRatio || 1)
      w = Math.max(1, Math.round(rect.width))
      h = Math.max(1, Math.round(rect.height))
      canvas.width = Math.round(w * dpr)
      canvas.height = Math.round(h * dpr)
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

      // Fitted to whichever edge runs out first, using the figure's own
      // measured aspect. Portrait gets far more of the frame, because there
      // the number *is* the screen — the way the reference for this fills a
      // phone rather than sitting in the middle of one.
      // Portrait takes the whole height. A figure with clear air all round it
      // is a picture of an object; one that reaches both edges is the object,
      // and the screen is a window onto it. It stops exactly at the edges
      // rather than running past them, because the first and last characters
      // of a version number are not arms of a galaxy — losing the top of the
      // 2 costs legibility that the extra few percent does not buy back.
      const wide = tall ? 0.72 : 0.54
      const high = tall ? 1.0 : 0.62
      span = Math.min(w * wide, (h * high) / Math.max(0.2, ratio))
    }

    const draw = (clock) => {
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.clearRect(0, 0, w, h)
      ctx.globalCompositeOperation = 'lighter'

      const cycle = clock % BREATH
      // 0 at the bottom of an exhale, 1 at the top of an inhale.
      const breath =
        cycle < INHALE
          ? easeInOut(cycle / INHALE)
          : 1 - easeInOut((cycle - INHALE) / EXHALE)

      const cx = w / 2
      const cy = h / 2
      const scale = span * (1 - 0.04 * breath)

      // Stars are sized against the stroke they sit on, not against the
      // window. `span` is the width of the whole figure, so one stroke is
      // roughly that over the number of characters on a line — and in portrait,
      // where a single digit is as wide as three were, everything sitting on
      // it has to grow by the same factor or the chains turn to dust. Sizing
      // this off `min(width, height)` is the same expression on a laptop and
      // half of it on a phone, which is exactly wrong.
      const unit = (span / perLine) * (3 / 727)

      // The whole point: held in on the inhale, let go on the exhale. The
      // number is never quite finished assembling and you watch it try — but
      // never so loose that it stops being legible.
      const loosen = 0.16 + (1 - breath) * 0.84

      // How far through the entrance the whole field is. Used for the things
      // that have no business existing while the stars are still scattered.
      const formed = clamp01((clock - HOLD) / (ASSEMBLE + SPREAD))
      const lit = clamp01(clock / FADE)

      // 1. The nebula, first and underneath.
      for (let i = 0; i < nebula.length; i += 1) {
        const n = nebula[i]
        const px = n.size * unit
        // Nothing to lie along until the stars have arrived, so it arrives
        // with them — and late, on the square, so the haze is the last thing
        // to turn up rather than a fog the shape forms inside.
        ctx.globalAlpha = n.glow * (0.55 + 0.45 * breath) * formed * formed
        ctx.drawImage(
          n.halo,
          cx + n.x * scale - px / 2,
          cy + n.y * scale - px / 2,
          px,
          px,
        )
      }

      // 2. Every star's bloom -- twice, offset along the stroke.
      //
      // Two draws a third of a radius apart down the tangent make an ellipse
      // out of a circle without a per-star transform, and 2,600 setTransform
      // calls a frame is not free. It is the whole reason the chains read as
      // ribbons: a round bloom keeps its neighbours separate, a stretched one
      // runs into them.
      for (let i = 0; i < motes.length; i += 1) {
        const m = motes[i]
        const drift = m.wander * loosen * unit * 2.4
        const home = smoother(clamp01((clock - HOLD - m.delay) / ASSEMBLE))

        const tx = cx + m.x * scale + Math.sin(clock * m.rate + m.phase) * drift
        const ty = cy + m.y * scale + Math.cos(clock * m.rate * 0.87 + m.phase) * drift

        // Straight from where it was waiting to where it belongs. No curve:
        // two hundred curved paths crossing each other reads as a swarm, and
        // the thing being watched is the shape arriving, not the routes.
        m.px = m.sx * w + (tx - m.sx * w) * home
        m.py = m.sy * h + (ty - m.sy * h) * home
        // Small and faint out in the scatter, full size once home. A star that
        // is already its final size while it is still travelling looks like a
        // sprite being moved; one that grows into place looks like it is
        // getting closer.
        m.arrive = 0.5 + 0.5 * home
        m.on = lit * (0.42 + 0.58 * home)

        const px = m.size * (3.8 + m.mag * 9) * unit * m.arrive
        const lean = px * 0.3
        ctx.globalAlpha = m.glow * (0.04 + 0.042 * breath) * m.on
        ctx.drawImage(
          m.halo,
          m.px + m.tx * lean - px / 2,
          m.py + m.ty * lean - px / 2,
          px,
          px,
        )
        ctx.drawImage(
          m.halo,
          m.px - m.tx * lean - px / 2,
          m.py - m.ty * lean - px / 2,
          px,
          px,
        )
      }

      // 3. The cores, sharp, on top.
      for (let i = 0; i < motes.length; i += 1) {
        const m = motes[i]
        const px = m.size * (1 + 0.3 * breath) * 1.25 * unit * m.arrive
        ctx.globalAlpha = Math.min(1, m.glow * (0.64 + 0.3 * breath) * m.on)
        ctx.drawImage(m.core, m.px - px / 2, m.py - px / 2, px, px)
      }

      // 4. The rest of the sky.
      for (let i = 0; i < dust.length; i += 1) {
        const d = dust[i]
        // Twinkle, barely: enough that the background is never quite the same
        // twice, not enough that anything up there is blinking at you.
        ctx.globalAlpha =
          d.glow * (0.62 + 0.38 * Math.sin(clock * 0.45 + d.phase)) * lit
        // Off the frame rather than off the figure: these are the sky behind
        // it, and they should not change size when the number does.
        const px = d.size * 2.2 * (Math.min(w, h) / 720)
        ctx.drawImage(d.core, d.x * w - px / 2, d.y * h - px / 2, px, px)
      }

      ctx.globalAlpha = 1
      ctx.globalCompositeOperation = 'source-over'
    }

    let frame = 0
    let clock = 0
    let last = performance.now()
    let onScreen = true

    const tick = (now) => {
      clock += Math.min(0.05, (now - last) / 1000)
      last = now
      draw(clock)
      frame = onScreen ? requestAnimationFrame(tick) : 0
    }

    const start = () => {
      if (frame || !onScreen || prefersReducedMotion()) return
      last = performance.now()
      frame = requestAnimationFrame(tick)
    }

    const stop = () => {
      if (frame) cancelAnimationFrame(frame)
      frame = 0
    }

    // The first build has to happen before the first measure can fit anything
    // to the frame. `tall` comes from the page, which measures the hero box
    // once and hands the same answer to the canvas and to the stylesheet, so
    // the digits and the two words can never disagree about which way up the
    // screen is.
    build(tall)
    measure()

    // Paint once, synchronously, before asking for a frame. requestAnimationFrame
    // does not run in a background tab, so a field that only ever drew from the
    // loop would be an empty black screen for anyone who opened this link in a
    // tab and came back to it later.
    //
    // Reduced motion gets one frame, well past the end of the entrance, so it
    // sees the number formed and never sees it fly. Everyone else starts at
    // zero, which is a black screen for a third of a second and then a sky.
    if (prefersReducedMotion()) {
      draw(HOLD + ASSEMBLE + SPREAD + INHALE)
    } else {
      draw(0)
      start()
    }

    // On a phone, the address bar sliding away fires `resize` on nearly every
    // scroll, and each one would otherwise re-fit the figure mid-scroll. The
    // work is debounced; re-rasterising only ever happens when `tall` changes,
    // which re-runs this whole effect.
    let settle = 0
    const onResize = () => {
      window.clearTimeout(settle)
      settle = window.setTimeout(() => {
        measure()
        // Always, not only when the loop is idle. Resizing the canvas clears
        // it, and waiting for the next animation frame to fill it back in is a
        // frame of blank on every resize — and forever, in a tab that is not
        // getting frames at all. The clock is untouched, so a resize during
        // the entrance neither restarts it nor skips it.
        draw(clock)
      }, 140)
    }
    window.addEventListener('resize', onResize)
    window.addEventListener('orientationchange', onResize)

    // Some browsers still service rAF in a hidden tab.
    const onVisibility = () => {
      if (document.hidden) stop()
      else start()
    }
    document.addEventListener('visibilitychange', onVisibility)

    const io = new IntersectionObserver(
      ([entry]) => {
        onScreen = entry.isIntersecting
        if (onScreen) start()
        else stop()
      },
      { rootMargin: '120px' },
    )
    io.observe(canvas)

    return () => {
      stop()
      window.clearTimeout(settle)
      window.removeEventListener('resize', onResize)
      window.removeEventListener('orientationchange', onResize)
      document.removeEventListener('visibilitychange', onVisibility)
      io.disconnect()
    }
    // `replayKey` rebuilds the field from scratch: a replay of something
    // generative should not be the same arrangement played twice.
  }, [text, tall, replayKey])

  return <canvas className="rh-canvas" ref={canvasRef} aria-hidden="true" />
}
