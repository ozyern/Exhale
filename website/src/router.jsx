import { useEffect, useState } from 'react'

/*
 * Twenty lines of router, and no dependency.
 *
 * There are two pages. react-router is 12KB to answer a question that is one
 * string comparison, and it would be the only runtime dependency on the site
 * that is not React itself.
 *
 * Paths rather than hashes, because these URLs get pasted into places that
 * render a preview card, and `#` fragments are the part of a URL that never
 * reaches the server. GitHub Pages has no rewrite rule, so `vite.config.js`
 * copies the built `index.html` to `404.html` — Pages serves that for any
 * unknown path, with a 404 status the crawler sees and the reader does not.
 */

/** Normalised: always leading slash, never trailing (except the root). */
export const currentPath = () => {
  const path = window.location.pathname.replace(/\/+$/, '')
  return path === '' ? '/' : path
}

export function useRoute() {
  const [path, setPath] = useState(currentPath)

  useEffect(() => {
    const sync = () => setPath(currentPath())
    window.addEventListener('popstate', sync)
    // Fired by `navigate` below: pushState does not notify anybody, including
    // the code that called it.
    window.addEventListener('exhale:route', sync)
    return () => {
      window.removeEventListener('popstate', sync)
      window.removeEventListener('exhale:route', sync)
    }
  }, [])

  return path
}

export function navigate(to, { replace = false } = {}) {
  if (currentPath() === to.replace(/\/+$/, '')) return
  window.history[replace ? 'replaceState' : 'pushState']({}, '', to)

  // pushState keeps the scroll position, and the browser will happily put it
  // back after the new page has rendered — so arriving at a release page from
  // halfway down the home page drops you halfway down the release page. Reset
  // it here, synchronously, before anything renders; a link that carries its
  // own anchor is left alone for the page to handle.
  if ('scrollRestoration' in window.history) window.history.scrollRestoration = 'manual'
  if (!to.includes('#')) window.scrollTo(0, 0)

  window.dispatchEvent(new Event('exhale:route'))
}

/**
 * An internal link that stays a real `<a>`.
 *
 * Middle-click, ⌘-click and "copy link address" all have to keep working, so
 * the href is always real and only an unmodified left click is intercepted.
 */
export function Link({ to, children, onNavigate, ...rest }) {
  const onClick = (event) => {
    if (
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    ) {
      return
    }
    event.preventDefault()
    navigate(to)
    onNavigate?.()
  }

  return (
    <a href={to} onClick={onClick} {...rest}>
      {children}
    </a>
  )
}
