import { useEffect, useMemo, useRef, useState } from 'react'
import Dock from './components/Dock.jsx'
import Words from './components/Words.jsx'
import Phone from './components/Phone.jsx'
import Ribbon from './components/Ribbon.jsx'
import Segments from './components/Segments.jsx'
import {
  ABOUT_FACTS,
  FEATURES,
  MAINTAINER,
  RELEASES,
  REPO,
  SHOT,
  SHOTS,
  SPECS,
  TAGLINE,
  TELEGRAM,
  VERSION,
} from './content.js'
import { useHeroScroll, useOnScreen, useReveals, prefersReducedMotion } from './hooks.js'
import {
  CloudIcon,
  DownloadIcon,
  GithubIcon,
  GlassIcon,
  LinkIcon,
  Mark,
  MoonIcon,
  WaveIcon,
} from './icons.jsx'

const GLYPHS = {
  cloud: CloudIcon,
  wave: WaveIcon,
  moon: MoonIcon,
  link: LinkIcon,
  glass: GlassIcon,
}

const SECTIONS = [
  { id: 'overview', label: 'Overview' },
  { id: 'material', label: 'Material' },
  { id: 'lyrics', label: 'Lyrics' },
  { id: 'colour', label: 'Colour' },
  { id: 'download', label: 'Download' },
]

/* ------------------------------------------------------------------ bars */

function Bars({ active, onSelect }) {
  return (
    <>
      {/* One centred group — mark, then the site-wide links evenly spaced, then
          utility icons — rather than a left/right split. This row is about the
          PROJECT (where the code and the builds live); the segmented control
          below it is about this page. Keeping those two jobs in separate rows
          is why neither has to carry the other's links. */}
      <div className="topbar">
        <nav className="topbar-inner" aria-label="Project">
          <a className="wordmark" href="#top" aria-label="Exhale, back to top">
            <Mark />
          </a>
          <a href={RELEASES} target="_blank" rel="noreferrer">
            Download
          </a>
          <a className="hide-sm" href={`${REPO}/releases`} target="_blank" rel="noreferrer">
            Releases
          </a>
          <a
            className="hide-sm"
            href={`${REPO}/blob/master/CHANGELOG.md`}
            target="_blank"
            rel="noreferrer"
          >
            Changelog
          </a>
          <a className="hide-sm" href={REPO} target="_blank" rel="noreferrer">
            Source
          </a>
          <a className="hide-sm" href={`${REPO}/issues`} target="_blank" rel="noreferrer">
            Issues
          </a>
          <a
            className="hide-sm"
            href={`${REPO}/blob/master/CONTRIBUTING.md`}
            target="_blank"
            rel="noreferrer"
          >
            Contributing
          </a>
          <a href={`${REPO}/blob/master/LICENSE`} target="_blank" rel="noreferrer">
            GPL-3.0
          </a>
          <a className="topicon" href={REPO} target="_blank" rel="noreferrer" aria-label="Source on GitHub">
            <GithubIcon />
          </a>
          <a className="topicon" href={RELEASES} target="_blank" rel="noreferrer" aria-label="Download the APK">
            <DownloadIcon />
          </a>
        </nav>
      </div>

      <div className="segbar">
        <Segments items={SECTIONS} active={active} onSelect={onSelect} />
      </div>
    </>
  )
}

/* ----------------------------------------------------------- hero device */

/**
 * The device under the headline, cycling on its own.
 *
 * The reference pins its statement and rotates the hardware underneath it —
 * iPhone, then Watch, then iPad, each scaling in while the words stay put.
 * Exhale is one app on one device, so what rotates here is the app's screens
 * instead: Home, the player, the lyrics. Same idea — the copy makes one claim
 * and the thing below it shows you three proofs of it without being asked.
 *
 * It answers the hero's pause button, because a viewer who stopped the light
 * at the top of the page meant "stop moving", not "stop that one element".
 */
function HeroDevice({ running }) {
  const [index, setIndex] = useState(0)
  const ref = useRef(null)
  const live = useOnScreen(ref, '140px')
  const awake = running && live && !prefersReducedMotion()

  useEffect(() => {
    if (!awake) return
    const id = window.setInterval(() => {
      setIndex((current) => (current + 1) % SHOTS.length)
    }, 3600)
    return () => window.clearInterval(id)
  }, [awake])

  return (
    <div className="device-hero reveal" ref={ref} style={{ '--d': '360ms' }}>
      <Phone shots={SHOTS} index={index} />
    </div>
  )
}

/* ----------------------------------------------------------------- stage */

/**
 * Three claims, one device.
 *
 * The beats are ordinary blocks of text; one observer watches which is
 * crossing the middle of the viewport and that decides what the pinned phone
 * shows. Scroll only picks a state — the state changes are springs, which is
 * why flicking past three beats at once still resolves cleanly.
 */
function Stage() {
  const [beat, setBeat] = useState(0)
  const [dock, setDock] = useState(0)
  const beatsRef = useRef(null)

  useEffect(() => {
    const nodes = beatsRef.current?.querySelectorAll('.beat')
    if (!nodes?.length || !('IntersectionObserver' in window)) return

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) setBeat(Number(entry.target.dataset.index))
        })
      },
      // A thin band across the middle: whichever beat is in it owns the
      // device. Wider bands let two beats claim it at once.
      { rootMargin: '-48% 0px -48% 0px', threshold: 0 },
    )

    nodes.forEach((node) => io.observe(node))
    return () => io.disconnect()
  }, [])

  const shown = [SHOT.home, SHOT.lyrics, SHOT.player][beat] ?? SHOT.home

  return (
    <div className="stage shell">
      <div className="stage-device reveal">
        <Phone shots={SHOTS} index={shown} />
      </div>

      <div className="stage-beats" ref={beatsRef}>
        <section className="beat" data-index="0" data-on={beat === 0} id="material">
          <p className="kicker">The material</p>
          <h2 className="headline-sm">Liquid glass, not a picture of one.</h2>
          <p className="lede">
            The dock, the sheets and the search field blur what is actually
            behind them and bend it at the rim, every frame, from the pixels the
            app has already drawn.
          </p>

          {/* The one part of the app rebuilt rather than photographed — because
              this is the one part you can put your hands on. A screenshot of a
              dock cannot be dragged. */}
          <div className="dock-demo">
            <div className="dock-demo-art" aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
            <Dock active={dock} onSelect={setDock} compact />
          </div>

          <p className="footnote">
            Drag it. The selected tab is a capsule that stretches in the
            direction it travels and overshoots before it settles — one object
            to follow, rather than a highlight blinking between slots.
          </p>
        </section>

        <section className="beat" data-index="1" data-on={beat === 1} id="lyrics">
          <p className="kicker">Lyrics</p>
          <h2 className="headline-sm">Every word, on time.</h2>
          <p className="lede">
            Line-by-line synced lyrics from LRCLIB, matched on title and artist
            rather than duration alone — so you get this song's words, not a
            same-length stranger's.
          </p>
          <p className="footnote">
            The active line lifts and sharpens a beat before it lands, and the
            lines around it fall away by distance. Cached for offline, and
            adjustable per track when a file runs early.
          </p>
        </section>

        <section className="beat" data-index="2" data-on={beat === 2} id="colour">
          <p className="kicker">Colour</p>
          <h2 className="headline-sm">The song brings its own colour.</h2>
          <p className="lede">
            Open a track and the player takes its palette from the cover — the
            background wash, the progress bar, the lyric highlight. Close it and
            the rest of the app is back to the colour you chose. Nothing here
            was picked by hand; it is the artwork, sampled.
          </p>
          <p className="footnote">
            The player above is tinted by the art behind it. The artist and
            About screens in the same build are not — they keep your theme,
            which is the point: the colour follows the music, not the app.
          </p>
        </section>
      </div>
    </div>
  )
}

/* ------------------------------------------------------------------ page */

export default function App() {
  const [ambient, setAmbient] = useState(!prefersReducedMotion())
  const [section, setSection] = useState(0)

  useReveals()
  useHeroScroll()

  // Scroll spy for the segmented control — the capsule tracks where you are
  // rather than only where you clicked.
  //
  // The click case needs a lock. Smooth-scrolling from Overview to Download
  // crosses every section in between, the observer reports each one, and the
  // capsule stutters through four positions before landing — which read as the
  // animation being broken when it was the spy doing exactly what it was told.
  // So a click names its destination, and until that destination reports in,
  // every other section's entry is ignored.
  const pending = useRef(null)

  useEffect(() => {
    if (!('IntersectionObserver' in window)) return
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          if (pending.current) {
            if (entry.target.id !== pending.current) return
            pending.current = null
          }
          const index = SECTIONS.findIndex((s) => s.id === entry.target.id)
          if (index >= 0) setSection(index)
        })
      },
      { rootMargin: '-45% 0px -50% 0px', threshold: 0 },
    )
    SECTIONS.forEach(({ id }) => {
      const node = document.getElementById(id)
      if (node) io.observe(node)
    })
    return () => io.disconnect()
  }, [])

  // A destination that never arrives — an anchor already on screen, a scroll
  // the user interrupts — would leave the spy deaf, so the lock always expires.
  const goToSection = (index) => {
    pending.current = SECTIONS[index]?.id ?? null
    setSection(index)
    window.setTimeout(() => {
      pending.current = null
    }, 1400)
  }

  const year = useMemo(() => new Date().getFullYear(), [])

  return (
    <div id="top">
      <Bars active={section} onSelect={goToSection} />

      <main>
        <Ribbon playing={ambient} onToggle={() => setAmbient((p) => !p)} />

        <section className="block" id="overview">
          <div className="shell statement center">
            <p className="kicker reveal">{TAGLINE}</p>
            <h1 className="headline reveal" style={{ '--d': '90ms' }}>
              A music player
              <br />
              that breathes.
            </h1>
            <p className="lede reveal" style={{ '--d': '180ms' }}>
              Live glass on every surface, lyrics that land on the beat, and
              colour that follows whatever is playing.
              <br />
              Open source, for Android 13 and newer.
            </p>
            <div className="linkrow reveal" style={{ '--d': '270ms' }}>
              <a className="textlink" href={RELEASES} target="_blank" rel="noreferrer">
                Get the APK <i>›</i>
              </a>
              <a className="textlink" href={REPO} target="_blank" rel="noreferrer">
                Read the source <i>›</i>
              </a>
            </div>

            <HeroDevice running={ambient} />
          </div>
        </section>

        <div className="block-tight">
          <Stage />
        </div>

        {/* Eight features, as the app's own settings list rather than eight
            near-identical gradient cards on a rail. The rail was borrowed from
            a reference whose cards each carry a real photograph; with nothing
            to put in them mine were eight abstract washes in a row, which is
            filler with a scrollbar. A list of rows with hairline dividers is
            what this content actually is — and it is the shape the app uses for
            exactly the same job. */}
        <section className="everything" id="everything">
          <div className="shell">
            <h2 className="bigtitle">
              <Words text="Everything else it does." />
            </h2>

            <div className="featlist reveal">
              {FEATURES.map((feature, index) => {
                const Glyph = GLYPHS[feature.icon]
                return (
                  <div className="featrow" key={feature.title} style={{ '--d': `${index * 45}ms` }}>
                    <span className="featglyph">
                      <Glyph />
                    </span>
                    <div>
                      <h3>{feature.title}</h3>
                      <p>{feature.body}</p>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        <section className="block">
          <div className="shell">
            <div className="specs reveal">
              {SPECS.map((spec) => (
                <div className="spec" key={spec.value}>
                  <b>{spec.value}</b>
                  <span>{spec.label}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="block-tight" id="download">
          <div className="shell">
            <div className="getit reveal">
              <h2 className="headline-sm">
                <Words text="Put it on your phone." />
              </h2>
              <p className="lede reveal" style={{ '--d': '160ms' }}>
                One APK, split by architecture. No account, no ads, nothing
                phoning home. If you would rather build it yourself, it is three
                commands.
              </p>
              <div className="btnrow reveal" style={{ '--d': '240ms' }}>
                <a className="btn" href={RELEASES} target="_blank" rel="noreferrer">
                  Download v{VERSION}
                </a>
                <a
                  className="btn btn-ghost"
                  href={`${REPO}/blob/master/CHANGELOG.md`}
                  target="_blank"
                  rel="noreferrer"
                >
                  What changed
                </a>
              </div>

              <dl className="facts">
                {ABOUT_FACTS.map((fact) => (
                  <div key={fact.label}>
                    <dt>{fact.label}</dt>
                    <dd>{fact.value}</dd>
                  </div>
                ))}
              </dl>

              <pre className="code">
                <b>git</b> clone {REPO}.git{'\n'}
                <b>cd</b> Exhale{'\n'}
                <b>./gradlew</b> assembleArm64Debug
              </pre>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer">
        <div className="shell">
          <div className="footer-mark">
            <img src="/logo.png" alt="" width="28" height="28" />
            Exhale
          </div>

          <div className="footer-grid">
            <div>
              <h4>Exhale</h4>
              <ul>
                <li>
                  <a href="#material">Material</a>
                </li>
                <li>
                  <a href="#lyrics">Lyrics</a>
                </li>
                <li>
                  <a href="#colour">Colour</a>
                </li>
                <li>
                  <a href="#download">Download</a>
                </li>
              </ul>
            </div>
            <div>
              <h4>Source</h4>
              <ul>
                <li>
                  <a href={REPO} target="_blank" rel="noreferrer">
                    Repository
                  </a>
                </li>
                <li>
                  <a href={`${REPO}/releases`} target="_blank" rel="noreferrer">
                    Releases
                  </a>
                </li>
                <li>
                  <a href={`${REPO}/issues`} target="_blank" rel="noreferrer">
                    Report a bug
                  </a>
                </li>
                <li>
                  <a
                    href={`${REPO}/blob/master/CONTRIBUTING.md`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Contributing
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4>Built on</h4>
              <ul>
                <li>
                  <a
                    href="https://github.com/Arturo254/OpenTune"
                    target="_blank"
                    rel="noreferrer"
                  >
                    OpenTune
                  </a>
                </li>
                <li>
                  <a href="https://lrclib.net" target="_blank" rel="noreferrer">
                    LRCLIB
                  </a>
                </li>
                <li>
                  <a
                    href="https://github.com/Kyant0/AndroidLiquidGlass"
                    target="_blank"
                    rel="noreferrer"
                  >
                    AndroidLiquidGlass
                  </a>
                </li>
                <li>
                  <a
                    href="https://github.com/chrisbanes/haze"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Haze
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4>Maintainer</h4>
              <ul>
                <li>
                  {MAINTAINER.name} — {MAINTAINER.role}
                </li>
                <li>
                  <a href={REPO.replace('/Exhale', '')} target="_blank" rel="noreferrer">
                    GitHub {MAINTAINER.handle}
                  </a>
                </li>
                <li>
                  <a href={TELEGRAM} target="_blank" rel="noreferrer">
                    Telegram {MAINTAINER.handle}
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4>Legal</h4>
              <ul>
                <li>
                  <a href={`${REPO}/blob/master/LICENSE`} target="_blank" rel="noreferrer">
                    GPL-3.0
                  </a>
                </li>
                <li>
                  <a
                    href={`${REPO}/blob/master/CODE_OF_CONDUCT.md`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Code of conduct
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <div className="footer-legal">
            <p>
              © {year} ozyern. Exhale is free software under the GNU GPL v3, built
              on the work of OpenTune.
            </p>
            <p>
              Not affiliated with, endorsed by, or connected to Apple, Google or
              YouTube. Album titles, artists and lyrics shown on this page are
              placeholders written for the demo.
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}
