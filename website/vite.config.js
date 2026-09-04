import { copyFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * GitHub Pages has no rewrite rule, so a deep link like /release/1.0.203 is a
 * 404 as far as it is concerned. Pages serves 404.html for those, and if that
 * file is the app, the router picks the path up and renders the right page —
 * with a 404 status the crawler sees and the reader never does.
 *
 * It has to be a copy of the *built* index.html rather than a file in public/,
 * because the asset filenames are hashed and only exist after the bundle does.
 */
const pagesFallback = () => ({
  name: 'exhale-pages-fallback',
  apply: 'build',
  closeBundle() {
    const dist = resolve(import.meta.dirname, 'dist')
    copyFileSync(resolve(dist, 'index.html'), resolve(dist, '404.html'))
  },
})

export default defineConfig({
  plugins: [react(), pagesFallback()],
  // Served from the root of a custom domain (exhale.ozyern.me), not from
  // /Exhale/ — so the base stays '/'. If this ever moves to a project page
  // without the CNAME, this is the one line that has to change.
  base: '/',
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
  },
})
