import { useEffect, useRef, useState } from 'react'
import { HomeIcon, LibraryIcon, RadioIcon, SearchIcon } from '../icons.jsx'

export const TABS = [
  { label: 'Home', Icon: HomeIcon },
  { label: 'Mood & Genres', short: 'Moods', Icon: RadioIcon },
  { label: 'Library', Icon: LibraryIcon },
]

/**
 * The dock, rebuilt for the browser.
 *
 * Same construction as the app's `LiquidTabBar`: equal-width slots, and a
 * glass capsule that is a separate layer sliding between them rather than a
 * background colour switching from one tab to the next. It matters for the
 * same reason it matters in the app — a capsule that *moves* is one object the
 * eye can follow, where a highlight that jumps is two objects blinking.
 *
 * What the web cannot do is the app's lens: there is no `RuntimeShader` here,
 * so the capsule brightens and blurs what is behind it instead of bending it.
 * Everything else — the overshoot, the stretch in the direction of travel, the
 * press scale — is the real behaviour.
 */
export default function Dock({
  active,
  onSelect,
  compact = false,
  withSearch = true,
}) {
  const [stretching, setStretching] = useState(false)
  const timer = useRef(0)

  useEffect(() => () => window.clearTimeout(timer.current), [])

  const select = (index) => {
    if (index !== active) {
      // Stretch into the move, recover on arrival. One timeout per gesture,
      // not one per frame.
      setStretching(true)
      window.clearTimeout(timer.current)
      timer.current = window.setTimeout(() => setStretching(false), 300)
    }
    onSelect(index)
  }

  const slot = `calc((100% - 8px) / ${TABS.length})`

  return (
    <div className="dock-row">
      <div className="dock glass" role="tablist" aria-label="Exhale navigation">
        <span
          className="dock-capsule"
          aria-hidden="true"
          style={{
            width: slot,
            transform: `translateX(calc(${active} * 100%)) scaleX(${
              stretching ? 1.1 : 1
            }) scaleY(${stretching ? 0.94 : 1})`,
          }}
        />
        {TABS.map((tab, index) => {
          const on = index === active
          return (
            <button
              key={tab.label}
              type="button"
              role="tab"
              aria-selected={on}
              data-on={on}
              className="dock-tab"
              onClick={() => select(index)}
            >
              <tab.Icon filled={on} />
              <span>{compact ? tab.short ?? tab.label : tab.label}</span>
            </button>
          )
        })}
      </div>

      {withSearch && (
        <button
          type="button"
          className="dock-circle glass"
          aria-label="Search"
          onClick={() => onSelect(active)}
        >
          <SearchIcon />
        </button>
      )}
    </div>
  )
}
