import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { prefersReducedMotion } from '../hooks.js'

/**
 * Segmented control, built like the app's `LiquidTabBar`: the capsule slides
 * between items instead of blinking from one to the next.
 *
 * Measured rather than computed. These segments are label-width, so there's no
 * fixed pitch to multiply by — we have to ask the DOM where each one landed.
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

  // The five labels are wider than a phone screen, so the last couple sit off
  // the right edge with no way to reach them. Scrolling the active pill into
  // view also means scrolling the page walks the bar along with it.
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
