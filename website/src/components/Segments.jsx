import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { prefersReducedMotion } from '../hooks.js'

/**
 * The page's navigation is the app's dock.
 *
 * A segmented control with a capsule that *slides* between items rather than a
 * highlight that blinks from one to the next — the same construction as the
 * app's `LiquidTabBar`, and the same reason: a capsule that moves is one
 * object the eye can follow.
 *
 * The capsule is measured rather than computed, because these segments are
 * label-width, not equal-width. The app can multiply by a fixed pitch; this
 * has to ask the DOM where each item actually landed.
 */
export default function Segments({ items, active, onSelect }) {
  const railRef = useRef(null)
  const itemRefs = useRef([])
  const [box, setBox] = useState({ left: 0, width: 0 })

  const measure = useCallback(() => {
    const node = itemRefs.current[active]
    if (!node) return
    setBox({ left: node.offsetLeft, width: node.offsetWidth })
  }, [active])

  // On a phone the five labels are wider than the screen, so the last one or
  // two live off the right edge. A control you cannot reach the end of is
  // worse than no control, so the active pill scrolls itself into view —
  // which also means that scrolling the PAGE walks the bar along, and the
  // section you are in is always the one you can see.
  useEffect(() => {
    const rail = railRef.current
    const node = itemRefs.current[active]
    if (!rail || !node) return
    if (rail.scrollWidth <= rail.clientWidth + 1) return
    const pad = parseFloat(getComputedStyle(rail).paddingLeft) || 0
    const target = node.offsetLeft - (rail.clientWidth - node.offsetWidth) / 2
    rail.scrollTo({
      left: Math.max(0, target - pad),
      behavior: prefersReducedMotion() ? 'auto' : 'smooth',
    })
  }, [active])

  // Layout effect, not an effect: the capsule has to be in the right place on
  // the frame the label renders, or it visibly slides in from zero on load.
  useLayoutEffect(measure, [measure])

  useEffect(() => {
    const onResize = () => measure()
    window.addEventListener('resize', onResize)
    // Web fonts land after first paint and change every label's width.
    document.fonts?.ready.then(measure).catch(() => {})
    return () => window.removeEventListener('resize', onResize)
  }, [measure])

  return (
    <div className="segments" ref={railRef} role="tablist" aria-label="Sections">
      <span
        className="segment-capsule"
        aria-hidden="true"
        style={{ transform: `translateX(${box.left}px)`, width: box.width }}
      />
      {items.map((item, index) => (
        <a
          key={item.id}
          ref={(node) => {
            itemRefs.current[index] = node
          }}
          className="segment"
          href={`#${item.id}`}
          role="tab"
          aria-selected={index === active}
          data-on={index === active}
          onClick={() => onSelect(index)}
        >
          {item.label}
        </a>
      ))}
    </div>
  )
}
