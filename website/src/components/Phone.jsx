import { useEffect, useRef } from 'react'
import { prefersReducedMotion } from '../hooks.js'

/**
 * The device — a frame around real screenshots.
 *
 * This used to rebuild the app's UI in markup, which was the right call while
 * the only screenshots in the repo were inherited from OpenTune: wrong app
 * name, wrong language, years older than the current design. With real ones in
 * hand a recreation is strictly worse — it is a drawing of the product that
 * drifts the moment the product moves.
 *
 * The one piece still rebuilt is the dock, and only because you can put your
 * hands on that one (see `Dock`). A screenshot cannot be dragged.
 *
 * Every screen is mounted and cross-fades on `data-on`, so the frame never
 * resizes between beats and the device never jumps while it is pinned.
 */

/**
 * A screen recording, layered over the stills.
 *
 * Some things a still cannot show. Lyrics land word by word, and the whole
 * claim is in the timing — a frozen frame of it is just text with one word
 * lit, which proves nothing about when it lit up.
 *
 * It only plays while its beat owns the device and the page is not paused,
 * and it rewinds when it loses the beat, so scrolling back to it starts the
 * line again rather than joining halfway.
 */
function PhoneVideo({ src, poster, on, running }) {
  const ref = useRef(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (on && running && !prefersReducedMotion()) {
      // Autoplay can still be refused (a data saver, a strict setting); the
      // poster is a real frame, so a refusal degrades to a screenshot.
      const started = el.play()
      if (started?.catch) started.catch(() => {})
    } else {
      el.pause()
      if (!on) el.currentTime = 0
    }
  }, [on, running])

  return (
    <video
      ref={ref}
      className="phone-shot"
      data-on={on}
      src={src}
      poster={poster}
      muted
      loop
      playsInline
      preload="metadata"
      aria-hidden="true"
    />
  )
}

export default function Phone({ shots, index, video }) {
  return (
    <div className="phone">
      <div className="phone-screen">
        {shots.map((shot, i) => (
          <img
            key={shot.src}
            className="phone-shot"
            data-on={i === index && !video?.on}
            src={shot.src}
            alt={i === index ? shot.alt : ''}
            aria-hidden={i !== index}
            width="592"
            height="1322"
            loading={i === 0 ? 'eager' : 'lazy'}
            decoding="async"
          />
        ))}

        {video ? <PhoneVideo {...video} /> : null}
      </div>
    </div>
  )
}
