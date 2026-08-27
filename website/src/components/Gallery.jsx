import { useCallback, useEffect, useRef, useState } from 'react'
import { PauseIcon, PlayIcon } from '../icons.jsx'
import { useOnScreen, prefersReducedMotion } from '../hooks.js'

/*
 * A rail of large cards, caption inside at the top, the thing it describes
 * drawn underneath. Advances on its own; the next card peeks in from the right
 * so the rail announces itself without needing a scrollbar.
 *
 * Each card holds a working miniature rather than a gradient. A card with a
 * gradient in it is a headline with decoration; a card with the EQ in it is
 * the EQ.
 */

const DURATION = 5600

/* ------------------------------------------------------------- miniatures */

const Row = ({ title, artist, tint, children }) => (
  <div className="g-row">
    <span className="g-cover" style={{ '--tint': tint }} />
    <div className="g-meta">
      <b>{title}</b>
      <span>{artist}</span>
    </div>
    {children}
  </div>
)

const Downloads = () => (
  <div className="g-panel g-stack">
    <Row title="Slow Exhale" artist="Nightwork" tint="var(--rose)">
      <span className="g-tick" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path
            d="m6 12.4 4 4 8-8.8"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
    </Row>
    <Row title="Blue Hour" artist="Marin" tint="var(--iris)">
      <span className="g-pct">64%</span>
    </Row>
    <div className="g-bar">
      <i style={{ width: '64%' }} />
    </div>
    <Row title="Citrus, Later" artist="Ovoid" tint="var(--ember)">
      <span className="g-pct">queued</span>
    </Row>
  </div>
)

const Equalizer = () => (
  <div className="g-panel g-eq">
    <div className="g-eq-bands">
      {[42, 68, 54, 86, 62, 78, 48, 70, 38].map((h, i) => (
        <span key={i} style={{ '--h': `${h}%`, '--d': `${i * 70}ms` }}>
          <i />
        </span>
      ))}
    </div>
    <div className="g-eq-scale">
      <span>32</span>
      <span>125</span>
      <span>500</span>
      <span>2k</span>
      <span>8k</span>
      <span>16k</span>
    </div>
  </div>
)

const Sleep = () => (
  <div className="g-panel g-centre">
    <div className="g-dial">
      <span className="g-dial-face">
        <b>30</b>
        <em>minutes</em>
      </span>
    </div>
    <p className="g-caption-sm">Fades out on the last track, rather than cutting</p>
  </div>
)

const Auto = () => (
  <div className="g-panel g-auto">
    <div className="g-auto-grid">
      {['Liked', 'History', 'Downloads', 'Playlists'].map((label) => (
        <span key={label}>{label}</span>
      ))}
    </div>
    <Row title="Slow Exhale" artist="Nightwork" tint="var(--rose)">
      <span className="g-pct">Now playing</span>
    </Row>
  </div>
)

const Discord = () => (
  <div className="g-panel g-discord">
    <p className="g-eyebrow">Listening to Exhale</p>
    <Row title="Slow Exhale" artist="Nightwork · Hours Later" tint="var(--iris)" />
    <div className="g-chips">
      <span className="g-chip g-chip-solid">Listen along</span>
      <span className="g-chip">View on YouTube</span>
    </div>
  </div>
)

const Scrobble = () => (
  <div className="g-panel g-stack">
    <div className="g-chips">
      <span className="g-chip g-chip-on">Last.fm — connected</span>
      <span className="g-chip g-chip-on">ListenBrainz — connected</span>
    </div>
    <Row title="Blue Hour" artist="Marin" tint="var(--iris)">
      <span className="g-pct">scrobbled</span>
    </Row>
    <Row title="Moss Garden" artist="Kalyna" tint="var(--rose)">
      <span className="g-pct">scrobbled</span>
    </Row>
  </div>
)

const Together = () => (
  <div className="g-panel g-centre">
    <p className="g-eyebrow">Session code</p>
    <p className="g-code">EXH·482</p>
    <div className="g-faces" aria-hidden="true">
      <i />
      <i />
      <i />
      <i />
    </div>
    <p className="g-caption-sm">Four listeners, same second</p>
  </div>
)

const Backup = () => (
  <div className="g-panel g-centre">
    <div className="g-file">
      <b>exhale-backup.backup</b>
      <span>Library · Playlists · Settings</span>
    </div>
    <div className="g-chips">
      <span className="g-chip">Export</span>
      <span className="g-chip">Restore</span>
    </div>
  </div>
)

const VISUALS = {
  downloads: Downloads,
  eq: Equalizer,
  sleep: Sleep,
  auto: Auto,
  discord: Discord,
  scrobble: Scrobble,
  together: Together,
  backup: Backup,
}

/* ------------------------------------------------------------------ rail */

export default function Gallery({ items }) {
  const railRef = useRef(null)
  const [index, setIndex] = useState(0)
  const [playing, setPlaying] = useState(!prefersReducedMotion())
  const live = useOnScreen(railRef, '0px')
  const awake = playing && live && !prefersReducedMotion()

  // offsetLeft includes the rail's leading padding, so take it back off to get
  // the scroll position that puts a card flush with the content start.
  const go = useCallback((next, smooth = true) => {
    const rail = railRef.current
    const card = rail?.children[next]
    if (!rail || !card) return
    const pad = parseFloat(getComputedStyle(rail).paddingLeft) || 0
    rail.scrollTo({ left: card.offsetLeft - pad, behavior: smooth ? 'smooth' : 'auto' })
  }, [])

  // Read the current card off the scroll position rather than tracking it
  // separately, so a drag, a wheel and the timer agree.
  useEffect(() => {
    const rail = railRef.current
    if (!rail) return
    let frame = 0
    const onScroll = () => {
      if (frame) return
      frame = window.requestAnimationFrame(() => {
        frame = 0
        const pad = parseFloat(getComputedStyle(rail).paddingLeft) || 0
        let best = 0
        let bestGap = Infinity
        Array.from(rail.children).forEach((card, i) => {
          const gap = Math.abs(card.offsetLeft - pad - rail.scrollLeft)
          if (gap < bestGap) {
            bestGap = gap
            best = i
          }
        })
        setIndex(best)
      })
    }
    rail.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      rail.removeEventListener('scroll', onScroll)
      if (frame) window.cancelAnimationFrame(frame)
    }
  }, [])

  useEffect(() => {
    if (!awake) return
    const id = window.setTimeout(() => go((index + 1) % items.length), DURATION)
    return () => window.clearTimeout(id)
  }, [awake, index, items.length, go])

  return (
    <div className="gallery">
      <div className="gallery-rail" ref={railRef}>
        {items.map((item, i) => {
          const Art = VISUALS[item.visual]
          return (
            <article
              className="gcard"
              key={item.title}
              data-on={i === index}
            >
              <p className="gcard-cap">
                <b>{item.title}.</b> {item.body}
              </p>
              <div className="gcard-art">{Art ? <Art /> : null}</div>
            </article>
          )
        })}
      </div>

      {/* Dots and stop button in one cluster, so the control that halts the
          motion sits next to the thing showing it. */}
      <div className="gallery-nav">
        <div className="gnav-dots" role="tablist" aria-label="Features">
          {items.map((item, i) => (
            <button
              type="button"
              key={item.title}
              role="tab"
              aria-selected={i === index}
              aria-label={item.title}
              data-on={i === index}
              onClick={() => go(i)}
            >
              {i === index ? (
                <i
                  key={`${index}-${awake}`}
                  style={{
                    animationDuration: `${DURATION}ms`,
                    animationPlayState: awake ? 'running' : 'paused',
                  }}
                />
              ) : null}
            </button>
          ))}
        </div>

        <button
          type="button"
          className="gnav-toggle"
          onClick={() => setPlaying((p) => !p)}
          aria-label={playing ? 'Pause the gallery' : 'Play the gallery'}
        >
          {playing ? <PauseIcon /> : <PlayIcon />}
        </button>
      </div>
    </div>
  )
}
