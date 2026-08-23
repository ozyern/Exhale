import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'

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
