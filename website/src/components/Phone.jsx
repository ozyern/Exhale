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
export default function Phone({ shots, index }) {
  return (
    <div className="phone">
      <div className="phone-screen">
        {shots.map((shot, i) => (
          <img
            key={shot.src}
            className="phone-shot"
            data-on={i === index}
            src={shot.src}
            alt={i === index ? shot.alt : ''}
            aria-hidden={i !== index}
            width="592"
            height="1322"
            loading={i === 0 ? 'eager' : 'lazy'}
            decoding="async"
          />
        ))}
      </div>
    </div>
  )
}
