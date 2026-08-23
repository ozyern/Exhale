import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
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
