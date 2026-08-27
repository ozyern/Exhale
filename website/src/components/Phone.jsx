import { useEffect, useRef } from 'react'
import { prefersReducedMotion } from '../hooks.js'

/**
 * A frame around real screenshots of the app.
 *
 * Every screen stays mounted and cross-fades on `data-on`, so the frame never
 * resizes between beats and the device doesn't jump while it's pinned.
 *
 * The dock is the one piece still rebuilt in markup (see `Dock`) — you can drag
 * that one, and a screenshot can't be dragged.
 */

/**
 * A screen recording layered over the stills.
 *
 * Lyrics land word by word and the claim is entirely in the timing, which a
 * still frame can't show. Plays only while its beat owns the device and the
 * page isn't paused; rewinds when it loses the beat so scrolling back starts
 * the line again instead of joining halfway.
 */
function PhoneVideo({ src, poster, on, running }) {
  const ref = useRef(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (on && running && !prefersReducedMotion()) {
      // Autoplay can be refused. The poster is a real frame, so that degrades
      // to a screenshot rather than a black hole.
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
