import { useEffect, useMemo, useRef, useState } from 'react'
import Dock from './components/Dock.jsx'
import Gallery from './components/Gallery.jsx'
import Words from './components/Words.jsx'
import Phone from './components/Phone.jsx'
import Ribbon from './components/Ribbon.jsx'
import Segments from './components/Segments.jsx'
import Specs from './components/Specs.jsx'
import {
  ABOUT_FACTS,
  FEATURES,
  LYRICS_VIDEO,
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
import { DownloadIcon, GithubIcon, Mark } from './icons.jsx'

const SECTIONS = [
  { id: 'overview', label: 'Overview' },
  { id: 'material', label: 'Material' },
  { id: 'lyrics', label: 'Lyrics' },
  { id: 'color', label: 'Color' },
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
 * The screens either side of the phone.
 *
 * `x` and `y` are multiples of a CARD's own size, not the container's, so the
 * arrangement holds its proportions at every width — the phone shrinks, the
 * cards shrink with it, and the fan keeps its shape instead of collapsing into
 * the middle on a laptop and flying apart on a desktop.
 *
 * The outer pair are deliberately the two screens that are least like a player
 * (an artist page, the About screen), because a fan of five near-identical
 * now-playing screens reads as one screenshot printed five times.
 */
const FAN = [
  { shot: SHOT.about, x: '-186%', y: '10%', r: '-9deg', s: 0.58, o: 0.5, crop: 'top center', d: 620 },
  { shot: SHOT.artist, x: '-130%', y: '3%', r: '-5.5deg', s: 0.76, o: 0.76, crop: '50% 18%', d: 540 },
  { shot: SHOT.lyrics, x: '-72%', y: '-2%', r: '-2.5deg', s: 0.95, o: 0.96, crop: '50% 30%', d: 460 },
  { shot: SHOT.player, x: '72%', y: '-2%', r: '2.5deg', s: 0.95, o: 0.96, crop: '50% 72%', d: 460 },
  { shot: SHOT.home, x: '130%', y: '3%', r: '5.5deg', s: 0.76, o: 0.76, crop: '50% 78%', d: 540 },
  { shot: SHOT.about, x: '186%', y: '10%', r: '9deg', s: 0.58, o: 0.5, crop: '50% 44%', d: 620 },
]

/**
 * The device under the headline, cycling on its own, flanked by the rest.
 *
 * The reference pins its statement and puts one device in the middle with a
 * spread of the things it can do fanned out behind it — so the claim above is
 * made once and answered six ways at a glance, before you have scrolled at
 * all. Exhale is one app on one device, so what fans out here is its own
 * screens: the phone cycles them, and the cards hold them still.
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
    <div className="hero-fan reveal" ref={ref} style={{ '--d': '360ms' }}>
      {/* Decorative: the phone in the middle carries all five of these in
          turn, with the alt text, so announcing them again would read the
          same app to a screen reader six times. */}
      <div className="fan" aria-hidden="true">
        {FAN.map((card, i) => (
          <figure
            className="fan-card"
            key={`${card.shot}-${i}`}
            style={{
              '--x': card.x,
              '--y': card.y,
              '--r': card.r,
              '--s': card.s,
              '--o': card.o,
              '--crop': card.crop,
              '--d': `${card.d}ms`,
            }}
          >
            <img src={SHOTS[card.shot].src} alt="" loading="lazy" decoding="async" />
          </figure>
        ))}
      </div>

      <div className="device-hero">
        <Phone shots={SHOTS} index={index} />
      </div>
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
function Stage({ running }) {
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
        {/* The lyrics beat gets the recording rather than the still: the whole
            claim is in the timing, and a frozen frame of it is just text with
            one word lit. */}
        <Phone
          shots={SHOTS}
          index={shown}
          video={{ ...LYRICS_VIDEO, on: beat === 1, running }}
        />
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
          <p className="footnote credit">
            Above: a screen recording of the shipping build, at normal speed.
          </p>
        </section>

        <section className="beat" data-index="2" data-on={beat === 2} id="color">
          <p className="kicker">Color</p>
          <h2 className="headline-sm">The song brings its own color.</h2>
          <p className="lede">
            Open a track and the player takes its palette from the cover — the
            background wash, the progress bar, the lyric highlight. Close it and
            the rest of the app is back to the color you chose. Nothing here
            was picked by hand; it is the artwork, sampled.
          </p>
          <p className="footnote">
            The player above is tinted by the art behind it. The artist and
            About screens in the same build are not — they keep your theme,
            which is the point: the color follows the music, not the app.
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
              color that follows whatever is playing.
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
          <Stage running={ambient} />
        </div>

        {/* The eight features, as a gallery of large cards that runs itself.
            Each card holds a working miniature of the thing it names rather
            than a coloured wash — the objection to the version before this one
            was that eight abstract gradients on a rail is filler with a
            scrollbar, and it was right. A card showing the EQ is the EQ. */}
        <section className="everything" id="everything">
          <div className="shell">
            <h2 className="bigtitle">
              <Words text="Everything else it does." />
            </h2>
          </div>
          <Gallery items={FEATURES} />
        </section>

        {/* The build and the download were two sections making one argument,
            back to back: here is what it is made of, here is how to get it.
            Split, the second one had to re-introduce itself and the first ended
            on a spec sheet with nowhere to go. Joined, the cards are the
            evidence and the panel below them is the conclusion. */}
        <section className="block build" id="build">
          <div className="shell center">
            <p className="kicker reveal">The build</p>
            <h2 className="headline reveal" style={{ '--d': '80ms' }}>
              Yours to read.
              <br />
              Yours to compile.
            </h2>
            <p className="lede reveal" style={{ '--d': '160ms' }}>
              Every figure below comes out of the same{' '}
              <code>build.gradle.kts</code> the release APK is built from.
              Nothing here is rounded, and nothing is a plan.
            </p>
          </div>

          <Specs items={SPECS} />

          <div className="shell">
            <div className="getit reveal" id="download">
              <h2 className="headline-sm">
                <Words text="So put it on your phone." />
              </h2>
              <p className="lede reveal" style={{ '--d': '160ms' }}>
                One APK that runs on every architecture. No account, no ads,
                nothing phoning home, and no store deciding whether you may have
                it. If you would rather build it yourself, it is three commands.
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

              <dl className="facts reveal" style={{ '--d': '300ms' }}>
                {ABOUT_FACTS.map((fact) => (
                  <div key={fact.label}>
                    <dt>{fact.label}</dt>
                    <dd>{fact.value}</dd>
                  </div>
                ))}
              </dl>

              <pre className="code reveal" style={{ '--d': '360ms' }}>
                <b>git</b> clone {REPO}.git{'\n'}
                <b>cd</b> Exhale{'\n'}
                <b>./gradlew</b> assembleUniversalDebug
              </pre>

              <p className="subnote getit-note">
                Android 13 or newer. Sideloading asks for permission once; the
                app checks GitHub for its own updates after that.
              </p>
            </div>
          </div>
        </section>

      </main>

      <footer className="footer">
        <div className="shell">
          <div className="footer-mark">
            <img src="/logo.png" alt="" width="30" height="30" />
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
                  <a href="#color">Color</a>
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
