import { useEffect, useRef, useState } from 'react'
import { RELEASE_NOTES, REPO } from '../content.js'
import { CloseIcon } from '../icons.jsx'

/*
 * What changed, without leaving the page.
 *
 * The button used to hand people to a three-hundred-line markdown file on
 * GitHub. That file is the right document for someone reading the diff and the
 * wrong one for someone standing at a download button deciding whether this is
 * worth 40MB, so the short version lives here and the long one is still one
 * link away at the bottom.
 *
 * It stays mounted and hidden rather than unmounting on close, because a sheet
 * that vanishes on a single frame is the thing the app itself was rewritten to
 * stop doing.
 */
export default function Changelog({ open, onClose }) {
  const [picked, setPicked] = useState(0)
  const sheet = useRef(null)
  const returnTo = useRef(null)

  useEffect(() => {
    if (!open) return

    returnTo.current = document.activeElement
    sheet.current?.focus({ preventScroll: true })

    const onKey = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)

    // The sheet scrolls, so the page behind it must not — and taking the page's
    // scrollbar away shifts the whole layout left unless its width is paid back.
    const gutter = window.innerWidth - document.documentElement.clientWidth
    const previous = document.body.style.cssText
    document.body.style.overflow = 'hidden'
    document.body.style.paddingRight = `${gutter}px`

    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.cssText = previous
      returnTo.current?.focus?.({ preventScroll: true })
    }
  }, [open, onClose])

  const release = RELEASE_NOTES[picked] ?? RELEASE_NOTES[0]

  return (
    <div className="notes" data-open={open} inert={!open}>
      <div className="notes-veil" onClick={onClose} aria-hidden="true" />

      <div
        className="notes-sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="notes-title"
        tabIndex={-1}
        ref={sheet}
      >
        <header className="notes-head">
          <div>
            <p className="notes-eyebrow">What changed</p>
            <h2 id="notes-title">Exhale {release.version}</h2>
            <p className="notes-date">
              {release.date} · {release.tag}
            </p>
          </div>
          <button type="button" className="notes-close" onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </header>

        {/* Only earns its space once there is a second release to switch to. */}
        {RELEASE_NOTES.length > 1 && (
          <div className="notes-versions">
            {RELEASE_NOTES.map((item, index) => (
              <button
                type="button"
                key={item.version}
                aria-pressed={index === picked}
                data-on={index === picked}
                onClick={() => setPicked(index)}
              >
                {item.version}
              </button>
            ))}
          </div>
        )}

        <div className="notes-body">
          <p className="notes-summary">{release.summary}</p>

          {release.groups.map((group) => (
            <section className="notes-group" key={group.title}>
              <h3>{group.title}</h3>
              <ul>
                {group.items.map((item) => (
                  <li key={item.title}>
                    <b>{item.title}</b>
                    <span>{item.body}</span>
                  </li>
                ))}
              </ul>
            </section>
          ))}

          <p className="notes-foot">
            Every fix, with the reason it broke, is in{' '}
            <a href={`${REPO}/blob/master/CHANGELOG.md`} target="_blank" rel="noreferrer">
              CHANGELOG.md
            </a>
            . The same notes are in the app under Settings → Updates.
          </p>
        </div>
      </div>
    </div>
  )
}
