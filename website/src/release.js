/*
 * The 1.0.203 announcement, as a document.
 *
 * `content.js` holds what the landing page says about the app in general, and
 * `RELEASE_NOTES` in that file holds the short version of a release — four
 * groups, a sentence each, for someone standing at a download button.
 *
 * This is the third document, and it is a different genre from both: the long
 * read. Why each thing changed, what it was before, and what it cost. It is
 * modelled on the shape a research lab uses to announce a version — an eyebrow,
 * one enormous headline, a deck, a table of facts, then sections you can jump
 * between — because that shape is built for exactly this: a reader who came for
 * one heading and will stay for eleven if the prose earns it.
 *
 * Everything below is a commit. Nothing here is a plan.
 */

import { SHOT } from './content.js'

/**
 * Block types the article renderer understands. Adding a case here means
 * adding one to `Blocks` in `pages/Release.jsx`; there is deliberately no
 * generic HTML escape hatch, so the article can never smuggle in a layout.
 *
 *   lede     one large opening paragraph
 *   p        body text
 *   list     titled items — the workhorse
 *   bullets  plain lines, for things too small to title
 *   note     an aside in a box: context, caveats, the thing under the thing
 *   quote    a pull quote, for the one sentence a section is really about
 *   figure   a real screenshot, with a caption that says what to look at
 *   cards    a small grid of figures
 *   code     a shell block
 *   table    label/value rows
 */

export const RELEASE = {
  version: '1.0.203',
  code: 203,
  date: 'September 5, 2026',
  dateISO: '2026-09-05',
  kicker: 'Release',
  read: '9 min read',

  title: 'Exhale 1.0.203',

  dek: 'The whole interface at your size, a library shaped like a library, a sleep timer you can read half asleep, and glass that bends the pixels actually behind it. This is everything that moved since the first public release — one hundred and one version codes in ten days.',

  /** Read off `app/build.gradle.kts` and the signed universal APK. */
  facts: [
    { label: 'Version', value: '1.0.203' },
    { label: 'Version code', value: '203' },
    { label: 'Download', value: '37 MB' },
    { label: 'Architecture', value: 'Universal' },
    { label: 'Requires', value: 'Android 13' },
    { label: 'License', value: 'GPL-3.0' },
  ],

  /** The three screens the release is most visible on, for the hero. */
  hero: {
    shots: [SHOT.home, SHOT.player, SHOT.lyrics],
    caption:
      'Home, the player and the lyrics screen, captured from the 1.0.203 universal build.',
  },

  sections: [
    {
      id: 'summary',
      nav: 'Summary',
      title: 'A release about size, shape and light',
      blocks: [
        {
          type: 'lede',
          text: '1.0.102 was the first build anyone outside this repository could install. It was also the first build anyone outside this repository could complain about, and almost everything in 1.0.203 comes from that: type too small to read on a big phone, a Library page with six ways of showing a collection and no answer to “where are my albums”, a sleep timer nothing could open, and a seek bar that had been stuck in one place for weeks.',
        },
        {
          type: 'p',
          text: 'The through-line is that a music player is a thing you use in the dark, one-handed, half awake, while something else has your attention. Every change below is either about making a surface readable at a glance, making it reachable from where you already are, or making the light on it behave like light.',
        },
        {
          type: 'cards',
          items: [
            { value: '5', label: 'commits', body: 'Since the 1.0.102 tag.' },
            { value: '101', label: 'version codes', body: '102 to 203, in ten days.' },
            { value: '18', label: 'settings screens', body: 'Re-grounded so the artwork wash runs under them.' },
            { value: '48', label: 'call sites', body: 'Now sharing one back button.' },
          ],
        },
        {
          type: 'note',
          title: 'A note on the version number',
          text: 'The version code is the version name. 1.0.203 is build 203, so “which build is this” and “is this newer than that” are the same question with the same answer, and the in-app updater never has to guess.',
        },
      ],
    },

    {
      id: 'scale',
      nav: 'Interface scale',
      title: 'The whole interface, at your size',
      blocks: [
        {
          type: 'p',
          text: 'Android has a Display Size setting that scales every app on the phone. Exhale now has its own, for Exhale only, under Settings → Appearance → Display: ten discrete steps, a scale model of a real screen that redraws as you drag, and an explicit Apply.',
        },
        {
          type: 'quote',
          text: 'Recreating the Activity is not something to do on every tick while someone is still deciding.',
        },
        {
          type: 'p',
          text: 'The obvious implementation is a `LocalDensity` override in the theme, and it is wrong in two ways that only show up once you go looking. Both were found by turning it to 130% and opening things.',
        },
        {
          type: 'list',
          items: [
            {
              title: 'LocalConfiguration does not follow a density override',
              body: 'So every layout sized from screenWidthDp or screenHeightDp — the player artwork, the always-on display, the artist hero, all thirteen bottom-sheet menus — overshoots by the scale factor. At 130% a song menu is taller than the screen it has to fit inside.',
            },
            {
              title: 'Dialogs and popups do not follow it either',
              body: 'Each one is a new window, and its AndroidComposeView re-provides density from its own resources. So the page scales and the dialog on top of it does not.',
            },
          ],
        },
        {
          type: 'p',
          text: 'The fix is to override `densityDpi` in a Configuration in `attachBaseContext`, and to divide the dp screen dimensions by the same factor — which the framework does not do for you. That fixes both at the source and makes resource qualifiers resolve exactly the way they do for a real system Display Size change. `fontScale` is deliberately untouched, so this never compounds with the accessibility font setting into type nobody asked for.',
        },
        {
          type: 'p',
          text: 'Steps that would take the layout under 300dp are not offered on devices too narrow for them. And once there is no room to read a tab label, the dock drops its labels rather than ellipsising them, because half a word is worse than no word next to an icon that already says it.',
        },
        { type: 'figure', shot: SHOT.home, caption: 'Home at the default step. The scale picker previews this exact page live as you move through the ten steps.' },
      ],
    },

    {
      id: 'library',
      nav: 'Library',
      title: 'A library shaped like a library',
      blocks: [
        {
          type: 'p',
          text: 'The old Library page had six different ways of presenting a collection and no answer to the only question anyone brings to it. It is rebuilt on Apple Music’s shape: a large title, an inset list of destinations with hairlines that start where the labels start, pinned collections in that same list, and Recently Added underneath as a two-column artwork grid.',
        },
        {
          type: 'list',
          items: [
            {
              title: 'Filter chips became pages',
              body: 'A chip that changes what a list contains, without changing where you are, gives you no way back. Each filter is now a real sub-page with its own header and a working back gesture.',
            },
            {
              title: 'The duplicate playlist list is gone',
              body: 'The Playlists tab already owns that list, and it is the one with reordering. Two lists of the same thing where only one can be reordered is a bug that looks like a feature.',
            },
          ],
        },
        { type: 'figure', shot: SHOT.artist, caption: 'Artist pages keep your chosen theme. Color only ever follows the music — see below.' },
      ],
    },

    {
      id: 'sleep',
      nav: 'Sleep timer',
      title: 'A sleep timer you can read half asleep',
      blocks: [
        {
          type: 'p',
          text: 'The sleep timer dialog existed in 1.0.102 and nothing could open it. No code path ever set its flag, and the only entry that had ever worked was a collapsed queue bar the default player design does not have. It now lives in the player’s overflow menu, reachable from every player design, and it counts songs as well as minutes.',
        },
        {
          type: 'p',
          text: 'Then it was redesigned twice. The version that shipped is a dial and a grid.',
        },
        {
          type: 'list',
          items: [
            {
              title: 'Nine fixed values are a keypad, not a list',
              body: 'The scrolling list it replaces gave every option the same shape and the same size, so nothing was findable without reading it, and the whole thing was taller than the phone. In a grid you find “30 min” by where it sits.',
            },
            {
              title: 'Every option carries the clock time it lands on',
              body: 'The choice is really “stop at about a quarter to twelve”. A timer is a claim about the future, and “45” is not a claim anyone can check. Showing it for both modes also makes them comparable: three songs is visibly about eleven minutes, without the arithmetic that made you reach for minutes in the first place.',
            },
            {
              title: 'The ring drains rather than fills',
              body: 'An almost-empty dial means the music is almost over, which is the reading you want at 1am. Remaining time alone cannot express that, which is why the timer now keeps the moment it started and the count it was set to, not just its deadline.',
            },
            {
              title: '+15 min extends without restarting',
              body: 'It is offered on a clock timer and deliberately not on a song counter, where it would have to silently change which kind of timer you had set.',
            },
          ],
        },
        {
          type: 'note',
          title: 'You cannot skip through a sleep timer',
          text: 'Only finished tracks count against the song counter — AUTO and REPEAT transitions. Skipping is how you keep listening, so it should not be how you cut the timer short.',
        },
      ],
    },

    {
      id: 'menu',
      nav: 'Player menu',
      title: 'Six containers deep before it reached a verb',
      blocks: [
        {
          type: 'p',
          text: 'The player’s overflow menu was a header card, then a volume card, then a three-by-two grid of tiles, then three more list cards. The grid gave “Music Together” and “Always On Display” exactly the same weight as liking the song that is playing.',
        },
        {
          type: 'p',
          text: 'It is now a bare header, five round actions — like, add to playlist, download, radio, copy link — volume as a line rather than a box, and one grouped list for everything that is a place rather than an action. Actions are round and on top. Places are rows and underneath. Nothing has to be read twice to find out which it is.',
        },
        { type: 'figure', shot: SHOT.player, caption: 'The full-screen player, tinted by the artwork behind it. The overflow menu opens from the control row.' },
      ],
    },

    {
      id: 'glass',
      nav: 'Liquid glass',
      title: 'Glass that bends the pixels actually behind it',
      blocks: [
        {
          type: 'p',
          text: 'The round controls — the back arrow, the app-bar search button, the clear-search × — are now cut from the search bar’s own material: the same modifier, called with the same numbers, rather than a second recipe tuned until it looked close. Pressing one deforms the pane and pulls it toward your finger, and a specular bloom tracks the touch.',
        },
        {
          type: 'p',
          text: 'In-content glass can refract now, which it could not before, for a specific reason: a real lens needs real pixels, and sampling the NavHost’s recording from inside the NavHost is a re-entrant layer draw that throws on the first frame. So a page records only what is painted beneath its own content and publishes that; the root publishes the ambient color field. Two surfaces that touch — the × inside the search bar, say — are handed the same backdrop, so they bend the same pixels rather than each inventing their own.',
        },
        {
          type: 'p',
          text: 'Settings picked up the same treatment: the album-art wash now runs behind every settings route, the groups are cards, and one liquid back button replaced forty-eight separate ones.',
        },
      ],
    },

    {
      id: 'lyrics',
      nav: 'Lyrics',
      title: 'Four things that made word-perfect sync still feel late',
      blocks: [
        {
          type: 'p',
          text: 'The lyrics screen was already matching on title and artist rather than duration, and already word-synced. It still felt half a beat behind, and there turned out to be four separate reasons for that.',
        },
        {
          type: 'list',
          items: [
            {
              title: 'Blur is forward-only',
              body: 'It keyed off the absolute distance from the current line, so lines you had already sung were blurred exactly as hard as lines coming up. A lens racks focus onto what you are about to read; sung lines stay sharp and merely dim.',
            },
            {
              title: 'The word swell is an envelope now, not a sine',
              body: 'sin(π · progress) peaks halfway through a word, so a long held syllable was brightest well after you heard it — the specific way perfect sync can still read as late. It is an ADSR envelope instead.',
            },
            {
              title: 'The scroll spring was slower than the lines',
              body: 'At StiffnessLow, a glide took longer than many lines last, so each new line cancelled the last one mid-flight and the active line wandered around the anchor. It is 340 now, and the scroll runs on its own clock 220ms ahead of the highlight, so the line arrives at the anchor about when it lights.',
            },
            {
              title: 'A line advancing past the fold is not a seek',
              body: 'It used to get the same treatment as jumping across the song, which is a much larger movement than the moment deserves.',
            },
          ],
        },
        {
          type: 'figure',
          shot: SHOT.lyrics,
          caption: 'The active line is sharp and lit; the lines above it stay sharp and dim, and only the lines ahead are blurred.',
        },
      ],
    },

    {
      id: 'updates',
      nav: 'Updates',
      title: 'An update that finishes inside the app',
      blocks: [
        {
          type: 'p',
          text: 'The launch prompt used to hand the download URL to the system browser — while the Updates page, two taps deeper in Settings, already owned a real transfer with byte progress, a reused cache file and a hand-off to the package installer. “Update Now” now navigates straight to that page, which arrives already checking and starts the transfer on its first frame.',
        },
        {
          type: 'p',
          text: 'The prompt itself is rebuilt as an iOS Software Update sheet: identity as a row, both version numbers on one line, the notes labelled and scrolling in their own box, a filled capsule over a plain accent link, and a footnote saying what the button will actually do. It drops an OutlinedButton whose onClick was empty — a control that looked pressable and did nothing — and adds the decline action the sheet never had.',
        },
        {
          type: 'note',
          title: 'The crash behind the button',
          text: 'Pressing “Update Now” threw UninitializedPropertyAccessException. The prompt was declared about two hundred lines above the NavHost, so its navigate() resolved to MainActivity’s own lateinit navController — a property nothing ever assigns, because onNewIntent guards it with isInitialized and quietly falls through. It compiled, it looked right, and it crashed on press. The sheet now lives below the real controller, and the property is published, so deep links arriving at a running process stop being dropped as well.',
        },
        {
          type: 'p',
          text: 'And the Updates page’s own primary action — the one control that must be unmissable — had been a translucent glass button reading “Download and Install” behind a download glyph: made of the same material as its background, and describing its own implementation. It is a solid capsule and two words.',
        },
      ],
    },

    {
      id: 'motion',
      nav: 'Motion',
      title: 'A page that is lit rather than painted',
      blocks: [
        {
          type: 'p',
          text: 'The ambient artwork glow drifts now. A lap every thirty-odd seconds and a slight swell — slow enough that you never catch it moving, and only notice that the page is not quite where you left it.',
        },
        {
          type: 'p',
          text: 'It had been deliberately static, on the argument that animating the tab the app opens on is a cost paid forever. The answer is that every animated value is read inside the draw lambda, so a frame of drift costs one layer invalidation and three gradients: no recomposition, and nothing at all for the list scrolling above it.',
        },
        {
          type: 'quote',
          text: 'The phase comes from the shared frame clock, not a per-instance transition — because the settings app bar draws its own copy of this wash, and two independent transitions would sit permanently out of phase and seam along its bottom edge.',
        },
        {
          type: 'list',
          items: [
            {
              title: 'Home arrives as a page',
              body: 'One clock, each shelf offset by its position, read entirely inside graphicsLayer. Shelves settle in from two percent under size as well as from below.',
            },
            {
              title: 'The shortcut tiles cascade',
              body: 'One by one rather than as a block. They are the only shelf whose parts are all on screen at once, so they are the only one where a cascade is something you can actually watch.',
            },
            {
              title: 'The large title shrinks as it leaves',
              body: 'Toward the size the compact bar’s title already is, anchored at the corner the two share, so the eye reads one title changing state instead of one word fading while another appears above it.',
            },
            {
              title: 'Cold start is three placeholder shelves',
              body: 'Rather than a black page with a title on it.',
            },
            {
              title: 'The launch animation lost three of its four ideas',
              body: 'Two shockwave rings, a ten-degree entrance tilt and a diagonal sheen sweep are gone. Four things competing for attention inside one second is what a splash screen looks like when it is trying to impress you. What is left: the mark settles from 0.90 with no rebound, breathes 3.5% through the hold, and the aperture opens over 500ms.',
            },
          ],
        },
      ],
    },

    {
      id: 'icons',
      nav: 'Icons',
      title: 'Two icon packs, and one mark that is finally the same mark',
      blocks: [
        {
          type: 'p',
          text: 'Gold (a gold mark lit on black, and the one the app ships wearing) and Classic (the black mark on gold), selected by enabling one of two activity-aliases — the incoming one first, because a moment with no enabled launcher component is a moment some launchers use to forget the app exists. The pack also supplies the app-bar disc and the boot splash mark, so the choice carries past the home screen. Both read from PackageManager rather than the preference, which is what makes them correct on the first frame and what makes them survive a reinstall.',
        },
        {
          type: 'p',
          text: 'Two older bugs went with it. The launcher icon’s background layer carried the source artwork’s own edge vignette — invisible under a circle mask and glaring under a square one; it is a straight ramp fitted from the artwork’s middle third and extrapolated, with the glyph alpha-matted so it no longer composites the plate color twice at its edges. And the in-app mark was a copy of the raw source artwork, so your home screen showed the fixed icon and the app’s own top bar showed the unfixed one.',
        },
      ],
    },

    {
      id: 'fixes',
      nav: 'Fixes',
      title: 'Fixes',
      blocks: [
        {
          type: 'list',
          items: [
            {
              title: 'The player’s seek bar was stuck',
              body: 'LiquidSlider was watching a plain parameter through snapshotFlow, which reads no snapshot state and therefore never re-emits. The progress bar had been sitting at one position for weeks because of it.',
            },
            {
              title: 'Settings app bars cut the artwork off dead',
              body: 'They were a flat fill of the page color, which sliced the ambient wash in half along the bar’s bottom edge. Eighteen of them now take the same ground as the page they sit on.',
            },
            {
              title: '“Update Now” crashed the app',
              body: 'Fixed as described above: the prompt was resolving navigate() against a lateinit property nothing ever assigned.',
            },
            {
              title: 'The account circle appeared twice',
              body: 'It briefly sat inside Home’s large title, directly under the app bar that already had one.',
            },
            {
              title: 'The contributing guide described a different app',
              body: 'It was OpenTune’s, inherited whole. It is now about this repository, this build system and this signing setup.',
            },
          ],
        },
      ],
    },

    {
      id: 'get',
      nav: 'Get it',
      title: 'Availability',
      blocks: [
        {
          type: 'p',
          text: 'One universal APK — arm64, arm32 and both x86 targets in a single file, so there is nothing to pick. No store, no account, no telemetry, and nothing asking permission on your behalf. Sideloading asks you once; after that the app checks GitHub for its own updates and installs them itself.',
        },
        {
          type: 'p',
          text: 'If you would rather not trust a binary, the whole app is the repository, and building it is three commands:',
        },
        {
          type: 'code',
          lines: [
            'git clone https://github.com/ozyern/Exhale.git',
            'cd Exhale',
            './gradlew assembleUniversalRelease',
          ],
        },
      ],
    },
  ],

  /** Small print that would break the prose if it were in the prose. */
  footnotes: [
    'Interface scale changes the app’s density, not its font scale, so it composes with the system accessibility font size instead of multiplying against it.',
    'Lock screen lyrics remain OxygenOS and ColorOS only, and off by default, under Settings → Content. They are inert on every other ROM.',
    'Download size is the signed universal release APK. An ABI-specific build compiled from the same source is smaller; the project still defines a Gradle flavor per ABI.',
  ],
}
