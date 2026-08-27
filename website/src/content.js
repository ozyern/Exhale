/*
 * Everything the page says, in one place.
 *
 * Nothing here is aspirational. Every feature below is a string, a module or a
 * settings screen that exists today (strings.xml, kizzy/, lastfm/, lrclib/,
 * innertube/). Shipped goes here; planned doesn't.
 */

export const REPO = 'https://github.com/ozyern/Exhale'
export const RELEASES = `${REPO}/releases/latest`
export const VERSION = '1.0.102'

/**
 * Palettes. `rose` is the app's DefaultThemeColor; the other two are what
 * the drifting backdrop mixes with.
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
 * Real captures of the shipping build, in the order the hero cycles them.
 * Alt text says what each screen does, not what it looks like.
 */
export const SHOTS = [
  { src: '/shots/home.jpg', alt: 'The Home screen: rows of recommendations under a liquid-glass dock.' },
  { src: '/shots/player.jpg', alt: 'The full-screen player, tinted by the cover art behind it.' },
  { src: '/shots/lyrics.jpg', alt: 'Synced lyrics, with the current line lit and the rest falling away.' },
  { src: '/shots/artist.jpg', alt: 'An artist page: portrait, biography, and top songs.' },
  { src: '/shots/about.jpg', alt: 'The About screen, showing version and build architecture.' },
]

export const SHOT = { home: 0, player: 1, lyrics: 2, artist: 3, about: 4 }

/** Screen capture, re-encoded for the web: 30fps, no audio track, ~230KB. */
export const LYRICS_VIDEO = {
  src: '/media/lyrics.mp4',
  poster: '/media/lyrics-poster.jpg',
}

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

/**
 * `visual` names the miniature drawn on the card (see `Gallery`). Ordered:
 * the two everyone wants, the two that leave the phone, the two that talk to
 * other services, the two about keeping things.
 */
export const FEATURES = [
  {
    visual: 'downloads',
    title: 'Stream it or keep it',
    body: 'Play straight through, or take a track, an album or a whole playlist off the network and listen with the radio off.',
  },
  {
    visual: 'eq',
    title: 'A real equalizer',
    body: 'Full band EQ with presets, plus playback speed and pitch that stay exactly where you left them between sessions.',
  },
  {
    visual: 'sleep',
    title: 'Sleep timer',
    body: 'Set it and stop thinking about it. The queue fades out when the time is up instead of cutting mid-bar.',
  },
  {
    visual: 'auto',
    title: 'Android Auto',
    body: 'Liked songs, history, downloads and your YouTube playlists on the dashboard, with a simplified mode for the road.',
  },
  {
    visual: 'discord',
    title: 'Discord Rich Presence',
    body: 'What you are playing, on your profile, with the cover art and buttons — and the option to keep showing it while paused.',
  },
  {
    visual: 'scrobble',
    title: 'Last.fm and ListenBrainz',
    body: 'Scrobble to either service or to both, so the record of what you listened to belongs to you rather than to this app.',
  },
  {
    visual: 'together',
    title: 'Music Together',
    body: 'Host a session, share the code, and everyone hears the same song at the same second. Guests can queue, if you let them.',
  },
  {
    visual: 'backup',
    title: 'Backup and restore',
    body: 'Library, playlists and settings out to a file and back again. Changing phones should not mean starting over.',
  },
]

/** `art` names the card's illustration (see `Specs`). Numbers off Gradle. */
export const SPECS = [
  {
    art: 'sdk',
    value: 'Android 13+',
    body: 'Compiled against 37, targeting 36, with a floor of API 33 — so it uses what those releases added rather than working around their absence.',
  },
  {
    art: 'kotlin',
    value: '100% Kotlin',
    body: 'Jetpack Compose top to bottom. There is not a single XML layout in the project; the glass, the dock and the lyrics are all drawn by the same composition.',
  },
  {
    art: 'license',
    value: 'GPL-3.0',
    body: 'Source you can read, build and fork, with the same freedom passed on to whoever you hand it to. The repository is the whole app, not a wrapper around a binary.',
  },
  {
    art: 'abi',
    value: 'One APK',
    body: 'arm64, arm32 and both x86 targets in a single universal build, so there is nothing to pick. The project still defines a flavour per ABI if you would rather compile a smaller one yourself.',
  },
  {
    art: 'release',
    value: `v${VERSION}`,
    body: 'The current release, checked straight against GitHub Releases. No store, no account, and nothing phoning home to ask permission.',
  },
]

/** What's worth knowing before tapping Download. Read off the release. */
export const ABOUT_FACTS = [
  { label: 'Version', value: VERSION },
  { label: 'Architecture', value: 'Universal' },
  { label: 'Size', value: '40 MB' },
]
