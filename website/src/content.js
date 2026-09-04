/*
 * Everything the page says, in one place.
 *
 * Nothing here is aspirational. Every feature below is a string, a module or a
 * settings screen that exists today (strings.xml, kizzy/, lastfm/, lrclib/,
 * innertube/). Shipped goes here; planned doesn't.
 */

export const REPO = 'https://github.com/ozyern/Exhale'
export const RELEASES = `${REPO}/releases/latest`
export const VERSION = '1.0.203'

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
  { label: 'Size', value: '37 MB' },
  { label: 'Requires', value: 'Android 13' },
]

/**
 * The release notes, written for someone deciding whether to update.
 *
 * CHANGELOG.md is the same release said properly — every fix, with the reason
 * it broke. That is the right document for anyone reading the diff and the
 * wrong one for anyone standing at a download button, so this is the short
 * version: four groups, a sentence each. Newest first; the sheet reads
 * `RELEASE_NOTES[0]` as current.
 */
export const RELEASE_NOTES = [
  {
    version: '1.0.203',
    date: 'September 5, 2026',
    tag: 'Current release',
    summary:
      'Everything that moved since the first public release: the interface at your size, a library shaped like a library, a sleep timer you can read half asleep, and glass that bends the pixels actually behind it.',
    groups: [
      {
        title: 'New',
        items: [
          {
            title: 'Interface scale',
            body: 'Exhale’s own copy of Android’s Display Size, for this app only. Ten steps, a scale model of a real screen that redraws as you drag, and an explicit Apply. Settings → Appearance → Display.',
          },
          {
            title: 'App icon packs',
            body: 'Default (a black mark on gold) and Gold (a gold mark lit on black). The pack also supplies the app-bar disc and the boot splash mark, so the choice carries past the home screen.',
          },
          {
            title: 'A sleep timer you can reach',
            body: 'It lives in the player menu now, reachable from every player design, and counts songs as well as minutes. Every option carries the clock time it lands on, and the ring drains rather than fills.',
          },
          {
            title: 'Refracting in-content glass',
            body: 'The round controls are cut from the search bar’s own material. Press one and the pane deforms toward your finger, with a specular bloom tracking the touch.',
          },
        ],
      },
      {
        title: 'Improved',
        items: [
          {
            title: 'The Library, rebuilt',
            body: 'A large title, an inset list of destinations, pinned collections in the same list, and Recently Added as an artwork grid. Filter chips became real pages with a working back gesture.',
          },
          {
            title: 'The player menu is one level deep',
            body: 'Five round actions on top, one grouped list of places underneath. It used to be six nested containers before it reached a verb.',
          },
          {
            title: 'Lyrics keep up',
            body: 'Blur is forward-only, the word swell peaks when you hear it rather than after, and the scroll runs 220ms ahead of the highlight so a line arrives at the anchor about when it lights.',
          },
          {
            title: 'Updates finish inside the app',
            body: '“Update Now” goes to the real transfer instead of the system browser, and the prompt is a proper Software Update sheet with a decline action it never had.',
          },
          {
            title: 'The page is lit rather than painted',
            body: 'The artwork glow drifts a lap every thirty-odd seconds. Home arrives shelf by shelf, the shortcut tiles cascade, and the large title shrinks toward the compact one as it leaves.',
          },
        ],
      },
      {
        title: 'Fixed',
        items: [
          {
            title: 'The player’s seek bar was stuck',
            body: 'LiquidSlider watched a plain parameter through snapshotFlow, which reads no snapshot state and so never re-emitted.',
          },
          {
            title: '“Update Now” crashed the app',
            body: 'The prompt was resolving navigate() against a lateinit property nothing ever assigned. It compiled, it looked right, and it threw on press.',
          },
          {
            title: 'Settings bars cut the artwork off dead',
            body: 'They were a flat fill of the page color, slicing the ambient wash along the bar’s bottom edge. Eighteen of them now take the same ground as the page.',
          },
          {
            title: 'The launcher icon was wrong under a square mask',
            body: 'Its background layer carried the source artwork’s own edge vignette — invisible under a circle, glaring under a square. The in-app mark was the unfixed copy.',
          },
        ],
      },
      {
        title: 'Removed',
        items: [
          {
            title: 'Three quarters of the launch animation',
            body: 'Two shockwave rings, a ten-degree entrance tilt and a diagonal sheen sweep. Four things competing for attention inside one second is what a splash looks like when it is trying to impress you.',
          },
          {
            title: 'The duplicate playlist list',
            body: 'The Playlists tab already owned it, and that is the one with reordering.',
          },
          {
            title: 'A second account circle',
            body: 'It briefly sat inside Home’s large title, directly under the app bar that already had one.',
          },
        ],
      },
    ],
  },
  {
    version: '1.0.102',
    date: 'August 26, 2026',
    tag: 'First public release',
    summary:
      'Everything below shipped in one go, so the list is longer than a release note usually is. From here on each one covers only what moved since the last.',
    groups: [
      {
        title: 'New',
        items: [
          {
            title: 'Your Sound Chem',
            body: 'A listening capsule for each month: total time, your top artist and song, and the five artists that took up most of it. Step through the months with the arrows.',
          },
          {
            title: 'Lyrics on the lock screen',
            body: 'On OnePlus and Oppo phones the lock screen music panel shows real synced lyrics instead of "No lyrics". Off by default, under Settings → Content.',
          },
          {
            title: 'Updates inside the app',
            body: 'Exhale checks GitHub for newer versions, downloads them with real progress, and hands them to the installer. The notes for the build you are on are compiled in, so they work offline.',
          },
          {
            title: 'A new Home screen',
            body: 'Six shortcuts in a grid at the top, and a large title that slides under the toolbar as you scroll.',
          },
          {
            title: 'Android Auto, Discord, scrobbling',
            body: 'Your library on the dashboard, what you are playing on your Discord profile, and scrobbles to Last.fm, ListenBrainz or both.',
          },
          {
            title: 'A breathing pacer, hidden',
            body: 'Tap the app mark in About seven times. Four seconds in, six seconds out, and a hundred and fifty motes that move with it.',
          },
        ],
      },
      {
        title: 'Improved',
        items: [
          {
            title: 'One glass, everywhere',
            body: 'The dock, the sheets, the search field and the player are all built from the same material now, so nothing on screen looks like it came from a different app.',
          },
          {
            title: 'The dock moves as one object',
            body: 'Opening and closing it is one shape changing rather than two panels fading through each other, and the selected tab stretches into the move before it settles.',
          },
          {
            title: 'Sheets can be thrown away',
            body: 'Drag the handle to dismiss one. The room behind it lightens as you drag, and the sheet springs back if you change your mind.',
          },
          {
            title: 'Settings you can find things in',
            body: 'A search field under the title, a label on every group, and dividers that line up with the text instead of stopping six pixels short.',
          },
          {
            title: 'Highest quality means highest',
            body: 'Max audio quality used to take the first source that answered. It now checks them all and keeps the best one.',
          },
          {
            title: 'Recommendations in your language',
            body: 'Picking a language during setup now actually takes effect, and the feed stops leaking songs in a language you did not ask for.',
          },
        ],
      },
      {
        title: 'Fixed',
        items: [
          {
            title: 'Update alerts could point backwards',
            body: 'Anyone running a build newer than the latest release was told to "update" to an older one.',
          },
          {
            title: 'Release builds were never signed',
            body: 'Which meant no phone would install them. They are signed, named and built from a tag now.',
          },
          {
            title: 'The dock was not really glass',
            body: 'It was blurring an empty layer, so the only thing it ever drew was its own grey film. It blurs the real screen behind it now.',
          },
          {
            title: 'Search stuttered while you typed',
            body: 'Every keystroke opened its own connection and the results blanked between them. Seven letters used to mean seven requests to throw six away.',
          },
          {
            title: 'Some songs showed another song\u2019s lyrics',
            body: 'Lyrics were being matched on track length alone, so any song of about the same length could win. Title and artist decide it now.',
          },
          {
            title: 'Home could crash when you opened it',
            body: 'Two sections arriving with the same id was enough to take the whole screen down.',
          },
        ],
      },
      {
        title: 'Removed',
        items: [
          {
            title: 'The update channel picker',
            body: 'There is one channel, so the choice was between one real option and one that does not exist.',
          },
          {
            title: 'The commit feed on the Updates page',
            body: 'Nobody standing on an update screen is asking what the last thirty commits were. The full history is still in the changelog.',
          },
          {
            title: 'The pie chart of cropped artist photos',
            body: 'Replaced by bars you can actually read a proportion off.',
          },
        ],
      },
    ],
  },
]
