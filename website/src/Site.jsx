import { useEffect } from 'react'
import App from './App.jsx'
import Release from './pages/Release.jsx'
import { RELEASE } from './release.js'
import { useRoute } from './router.jsx'

/**
 * Two pages, one string comparison.
 *
 * `/release` without a version resolves to the current one, so the short URL
 * keeps working after the next release rather than rotting into a 404 — and any
 * path that is not a release is the home page, which is what a marketing site
 * should do with a typo.
 */
const isRelease = (path) => path === '/release' || path.startsWith('/release/')

/**
 * The title and the canonical link are the two things a crawler and a preview
 * card read, and neither follows a client-side navigation on its own.
 */
const HEAD = {
  home: {
    title: 'Exhale — a music player that breathes',
    description:
      'A fast, open-source music player for Android. Live liquid glass, lyrics on the beat, and color that follows your album art.',
    path: '/',
  },
  release: {
    title: `Exhale ${RELEASE.version} — release notes`,
    description: RELEASE.dek,
    path: `/release/${RELEASE.version}`,
  },
}

export default function Site() {
  const path = useRoute()
  const release = isRelease(path)

  useEffect(() => {
    const head = release ? HEAD.release : HEAD.home
    document.title = head.title

    const set = (selector, attribute, value) => {
      const node = document.head.querySelector(selector)
      if (node) node.setAttribute(attribute, value)
    }

    set('meta[name="description"]', 'content', head.description)
    set('meta[property="og:title"]', 'content', head.title)
    set('meta[property="og:description"]', 'content', head.description)
    set('meta[property="og:url"]', 'content', `https://exhale.ozyern.me${head.path}`)
    set('link[rel="canonical"]', 'href', `https://exhale.ozyern.me${head.path}`)
  }, [release])

  return release ? <Release /> : <App />
}
