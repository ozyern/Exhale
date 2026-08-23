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

## Where things are

| File | What it is |
| --- | --- |
| `src/App.jsx` | Page structure, the scroll spy, and the album/colour state |
| `src/components/Ribbon.jsx` | The hero object — five ellipses of light, additively blended |
| `src/components/Segments.jsx` | The segmented nav; its capsule is the app's dock capsule |
| `src/components/Phone.jsx` | The device, with its three views stacked and cross-faded |
| `src/components/Dock.jsx` | A working rebuild of the app's `LiquidTabBar` |
| `src/content.js` | Every word and every feature claim on the page |
| `src/styles.css` | All of it |

There are no screenshots. The device is rebuilt in CSS instead, because the
screenshots in `fastlane/` are inherited from OpenTune — wrong app name, wrong
language, and years older than the current design. Rebuilding it means the
site demonstrates the material rather than picturing it, and it never goes
stale against the app.

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
