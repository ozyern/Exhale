/*
 * Everything the page says, in one place.
 *
 * The feature list is not aspirational: every entry below is a string, a
 * module or a settings screen that exists in the app today (`strings.xml`,
 * `kizzy/`, `lastfm/`, `lrclib/`, `innertube/`). If a feature ships, it can
 * go here; if it is planned, it does not.
 */

export const REPO = 'https://github.com/ozyern/Exhale'
export const RELEASES = `${REPO}/releases/latest`
export const VERSION = '1.0.102'

/**
 * The palettes the colour beat swaps between.
 *
 * `rose` is the app's `DefaultThemeColor`; the other two are the partners the
 * drifting backdrop mixes with. Choosing an album here rewrites those three
 * custom properties on `:root`, which is the same move the app makes when it
 * pulls a palette out of the cover — one source of colour, and every surface
 * downstream of it follows without being told.
 */
export const ALBUMS = [
  {
    id: 'exhale',
    title: 'Slow Exhale',
    artist: 'Nightwork',
    rose: '#ed5564',
    ember: '#ff8a6b',
    iris: '#7b6cff',
  },
  {
    id: 'blue',
    title: 'Blue Hour',
    artist: 'Marin',
    rose: '#4a8bff',
    ember: '#3ad6c4',
    iris: '#6c5cff',
  },
  {
    id: 'citrus',
    title: 'Citrus, Later',
    artist: 'Ovoid',
    rose: '#f5a524',
    ember: '#ffd166',
    iris: '#ff6b6b',
  },
  {
    id: 'moss',
    title: 'Moss Garden',
    artist: 'Kalyna',
    rose: '#46bf7b',
    ember: '#a8e063',
    iris: '#2fa3a0',
  },
]

export const artFor = (album, angle = 145) =>
  `linear-gradient(${angle}deg, ${album.ember} 0%, ${album.rose} 46%, ${album.iris} 100%)`

/** The app's own tagline, off its About screen. */
export const TAGLINE = 'Music, exhaled.'

export const MAINTAINER = { name: 'Aditya Jha', role: 'Lead developer', handle: '@ozyern' }
export const TELEGRAM = 'https://t.me/ozyern'

/**
 * The screens, in the order the hero cycles them.
 *
 * These are real captures of the shipping build, not recreations — see
 * `Phone`. Alt text describes what the screen DOES, because that is what a
 * reader who cannot see it needs from a product page.
 */
export const SHOTS = [
  { src: '/shots/home.jpg', alt: 'The Home screen: rows of recommendations under a liquid-glass dock.' },
  { src: '/shots/player.jpg', alt: 'The full-screen player, tinted by the cover art behind it.' },
  { src: '/shots/lyrics.jpg', alt: 'Synced lyrics, with the current line lit and the rest falling away.' },
  { src: '/shots/artist.jpg', alt: 'An artist page: portrait, biography, and top songs.' },
  { src: '/shots/about.jpg', alt: 'The About screen, showing version and build architecture.' },
]

export const SHOT = { home: 0, player: 1, lyrics: 2, artist: 3, about: 4 }

/** Placeholder words, written for the demo — nobody's lyrics but ours. */
export const LYRICS = [
  'and the room goes quiet',
  'the way a room does',
  'when the light gets low enough to lean on',
  'I can hear it coming back',
  'slow, and then all at once',
  'hold the note until it lets go',
  'one more turn around the dark',
  'breathe out',
]

export const FEATURES = [
  {
    icon: 'cloud',
    title: 'Stream or keep it',
    body: 'Play straight through, or download a track, an album or a playlist and take it off the network entirely.',
  },
  {
    icon: 'wave',
    title: 'Equalizer',
    body: 'A full band EQ with presets, plus playback speed and pitch that stay where you put them.',
  },
  {
    icon: 'moon',
    title: 'Sleep timer',
    body: 'Set it and stop worrying about it. The queue fades out on time rather than cutting.',
  },
  {
    icon: 'link',
    title: 'Android Auto',
    body: 'Liked songs, history, downloads and your YouTube playlists on the dashboard, with a simplified mode for the road.',
  },
  {
    icon: 'link',
    title: 'Discord Rich Presence',
    body: 'What you are playing, on your profile, with cover art and buttons — and an option to keep showing it while paused.',
  },
  {
    icon: 'cloud',
    title: 'Last.fm and ListenBrainz',
    body: 'Scrobbling to either service or both, so your listening history belongs to you rather than to this app.',
  },
  {
    icon: 'wave',
    title: 'Music Together',
    body: 'Host a session, share the code, and everyone hears the same song at the same second. Guests can queue if you let them.',
  },
  {
    icon: 'glass',
    title: 'Backup and restore',
    body: 'Library, playlists and settings out to a file and back again. Moving phones is not a reason to start over.',
  },
]

export const SPECS = [
  { value: 'Android 13+', label: 'API 33 and newer' },
  { value: '100% Kotlin', label: 'Jetpack Compose, no Views' },
  { value: 'GPL-3.0', label: 'Source you can read' },
  { value: 'arm64 · arm32', label: 'Split builds per ABI' },
  { value: `v${VERSION}`, label: 'Current release' },
]

/** Read straight off the app's own About screen, so the two cannot disagree. */
export const ABOUT_FACTS = [
  { label: 'Version', value: VERSION },
  { label: 'Architecture', value: 'arm64' },
]
