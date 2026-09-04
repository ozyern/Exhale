# exhale.ozyern.me

The marketing site. Vite + React, no UI framework, no CSS framework — one
stylesheet and a handful of components.

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # -> dist/
npm run preview  # serve the built output
```

## Design rules

Borrowed from Apple's OS pages, which is what the site is modelled on:

- The page opens with an **object**, not a headline — light in a black room.
- Pure `#000`. Colour only ever comes from the light itself.
- One neutral face at semibold. Never heavier than 600; the weight comes from
  the size.
- Secondary text is `#86868b` and does all the quiet work.
- A section is allowed to be one sentence.

The part that is ours rather than borrowed: **every colour on the page is
three custom properties** (`--rose`, `--ember`, `--iris`) that the album
swatches overwrite at runtime. The ribbon, the device, the accents and the
links all retint together, because that is how the app itself works — a
palette extracted from the cover, pushed through one source of truth.

## Two pages

`/` is a **product page**: one claim per screen, poster-size type, an object
rather than a headline. It is for someone who has never heard of the app.

`/release/<version>` is a **document**: article measure, a rail you can jump
from, and prose allowed to run several paragraphs. It is for someone who
already has the app installed and wants to know what changed and why.

They deliberately do not look the same. The release page keeps the black, the
greys and the three retinting properties and nothing else.

Routing is `src/router.jsx` — twenty lines and no dependency, because there are
two pages and the question is one string comparison. Paths rather than hashes,
so the URLs survive being pasted somewhere that renders a preview card. GitHub
Pages has no rewrite rule, so `vite.config.js` copies the built `index.html` to
`404.html`; Pages serves that for any unknown path, with a 404 status the
crawler sees and the reader does not.

## Where things are

| File | What it is |
| --- | --- |
| `src/Site.jsx` | Which page, and the `<title>`/canonical that goes with it |
| `src/router.jsx` | The router |
| `src/App.jsx` | The home page: structure, scroll spy, album/colour state |
| `src/pages/Release.jsx` | The release page: hero, rail, article, footnotes |
| `src/components/Chrome.jsx` | The announcement strip, the top bar, the footer |
| `src/components/BreathField.jsx` | The build number drawn as a sky |
| `src/components/Ribbon.jsx` | The home hero — five ellipses of light, additively blended |
| `src/components/Segments.jsx` | The segmented nav; its capsule is the app's dock capsule |
| `src/components/Phone.jsx` | The device, with its views stacked and cross-faded |
| `src/components/Dock.jsx` | A working rebuild of the app's `LiquidTabBar` |
| `src/content.js` | Every word and every feature claim on the home page |
| `src/release.js` | The release announcement, as data |
| `src/styles.css` | All of it |

## The number

The object at the top of the release page is the build number, and it is not
type with an effect on it. `BreathField` rasterises the digits, runs a chamfer
distance transform over the mask to find how deep inside each stroke every
pixel is, and scatters points weighted by that depth — so each stroke is a
bright chain down its middle thinning to nothing at its edges. Every point is
then a star with its own magnitude, colour and orbit, drawn in three passes:
haze, bloom, core.

Three things carry it, and dropping any one of them is what makes this sort of
thing look cheap:

- **Magnitude.** A cube law, so most points are a pixel and a few are the size
  of a fingernail. An even scatter of mid-size dots is confetti.
- **Filaments, not a filled stencil.** Fill a letterform evenly with dots and
  you get dots in the shape of a letter. Run them along the spine and you get
  an arm of something.
- **A ground that is not `#000`.** The one place on the site that breaks the
  pure-black rule. Stars on pure black read as dots on a page rather than as
  light at a distance.

It breathes on the app's own numbers — four seconds in, six seconds out, off
the pacer hidden behind seven taps on the mark in About — tightening on the
inhale and loosening on the exhale, so the number is never quite finished
assembling. It stops drawing entirely when it scrolls off screen, when the tab
hides, or when the reader asked for less motion.

## Releasing

When a new version ships:

1. `VERSION` in `src/content.js`, and prepend an entry to `RELEASE_NOTES` —
   four groups, a sentence each, for someone standing at a download button.
2. `RELEASE` in `src/release.js` — the long read. Why each thing changed, what
   it was before, and what it cost.
3. That is all. The announcement strip, the top bar, the footer, the number in
   the hero and the `/release/<version>` URL all read from those two.

The screenshots in `public/shots/` are captures of the shipping build, so the
site cannot drift from what the app looks like. The one part rebuilt in the
browser is the dock, because that is the one part you can put your hands on
and a screenshot cannot be dragged. The mark in `public/` is cut out of
`assets/splash_dark.png` — the app's own dark splash, with the black turned
into transparency, since the artwork is additive and its luminance is already
its coverage.

## Feature claims

Everything in `FEATURES` is a string, a module or a settings screen that
exists in the app **today** (`app/src/main/res/values/strings.xml`, `kizzy/`,
`lastfm/`, `lrclib/`, `innertube/`). If a feature ships, it can go here. If it
is planned, it does not.

## Deployment

`.github/workflows/website.yml` builds and publishes to GitHub Pages on any
push to `master` that touches `website/**`.

Two things have to be set up once, outside this repo:

1. **Repo → Settings → Pages → Source: GitHub Actions.**
2. **DNS:** a `CNAME` record for `exhale` pointing at `ozyern.github.io.`
   `public/CNAME` already carries the domain into the build, so Pages keeps
   the custom domain across deploys. Tick *Enforce HTTPS* once the certificate
   is issued.

If the site is ever served from a project path instead of the custom domain,
`base` in `vite.config.js` is the one line that changes.
