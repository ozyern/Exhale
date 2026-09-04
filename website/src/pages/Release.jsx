import { useEffect, useRef, useState } from 'react'
import { RELEASE_PATH, SiteFooter, TopBar } from '../components/Chrome.jsx'
import { Link } from '../router.jsx'
import { RELEASE } from '../release.js'
import { RELEASES, REPO, SHOTS } from '../content.js'
import { useReveals } from '../hooks.js'
import { ArrowLeftIcon, LinkIcon, ReplayIcon } from '../icons.jsx'
import BreathField from '../components/BreathField.jsx'

/*
 * The release announcement, as a long read.
 *
 * The rest of the site is a product page: enormous type, one claim per screen,
 * an object rather than a headline. That is the right shape for someone who has
 * never heard of the app and the wrong one for someone who already has it
 * installed and wants to know what changed and why — so this page is a
 * document. Article measure, a rail you can jump from, and prose that is
 * allowed to be several paragraphs long.
 *
 * What it keeps from the rest of the site: the black, the greys, the three
 * retinting custom properties, and the rule that the weight comes from the size
 * rather than from the font.
 */

/* ------------------------------------------------------------------- hero */

/**
 * A word that arrives one letter at a time, from the inside out.
 *
 * `from` says which end is nearest the number, and that letter goes first, so
 * both words appear to be pushed outward by the thing between them rather than
 * typed left to right on either side of it. It is a small thing that costs one
 * custom property and is the difference between two labels and a title.
 */
function Word({ text, from = 'start' }) {
  const letters = [...text]
  return (
    <span className="rh-word" aria-hidden="true">
      {letters.map((ch, i) => (
        <span
          key={`${ch}-${i}`}
          style={{ '--i': from === 'end' ? letters.length - 1 - i : i }}
        >
          {ch}
        </span>
      ))}
    </span>
  )
}

/**
 * The title, as an object.
 *
 * The whole first screen is the release: a field of light in the middle and
 * the name split to the two outside edges, so the type frames the object
 * instead of sitting on top of it. Nothing else is above the fold — no nav
 * bar over the artwork, no scroll hint, no button. The one thing a reader can
 * do here is look at it, and then scroll.
 *
 * It carries the h1, so the article below does not repeat the name at poster
 * size two screens running.
 */
function SplitTitle({ onReplay }) {
  const [key, setKey] = useState(0)
  const [tall, setTall] = useState(false)
  const box = useRef(null)

  // One decision, read off the box itself rather than off the viewport.
  //
  // The canvas needs it to know whether to set the digits in a row or one per
  // line, and the stylesheet needs it to know whether the two words sit either
  // side of the number or at opposite corners of it. A media query would be a
  // second opinion, and a short landscape window is exactly where the two
  // would disagree — the words stacked over a number that is still in a row.
  useEffect(() => {
    const node = box.current
    if (!node) return
    const read = () => {
      const rect = node.getBoundingClientRect()
      setTall(rect.height / Math.max(1, rect.width) > 1.12)
    }
    read()

    // Both, because they fail in different places. ResizeObserver catches the
    // box changing without the window doing anything; `resize` catches the
    // window when the observer's callback is not being delivered, which is
    // what happens whenever the rendering pipeline is throttled.
    window.addEventListener('resize', read)
    window.addEventListener('orientationchange', read)
    const ro = 'ResizeObserver' in window ? new ResizeObserver(read) : null
    ro?.observe(node)

    return () => {
      window.removeEventListener('resize', read)
      window.removeEventListener('orientationchange', read)
      ro?.disconnect()
    }
  }, [])

  const replay = () => {
    setKey((n) => n + 1)
    onReplay?.()
  }

  return (
    <div className="rh" ref={box} data-tall={tall}>
      <BreathField text={String(RELEASE.code)} tall={tall} replayKey={key} />

      {/* The build number in the middle is drawn on the canvas, which is
          aria-hidden, and the two words are split into per-letter spans — so
          the heading carries the whole thing once, as text, for anything that
          reads the page rather than looks at it. */}
      {/* Keyed on the replay, so pressing it plays the words in again with
          the field rather than leaving them sitting there already arrived.
          The letters are split into spans and hidden from assistive tech; the
          heading's real text is the one line in the middle. */}
      <h1 className="rh-type" key={key}>
        <Word text="Exhale" from="end" />{' '}
        <span className="rh-said">Exhale {RELEASE.version}, Universal</span>{' '}
        <Word text="Universal" />
      </h1>

      <button
        type="button"
        className="rh-replay"
        onClick={replay}
        aria-label="Play the animation again"
      >
        <ReplayIcon />
      </button>
    </div>
  )
}

/**
 * Three real captures on a lit ground, under the facts.
 *
 * The object above is the release; this is the app. Announcing a version with
 * nothing but generative artwork is a poster, and a poster is not evidence.
 */
function ReleaseShots({ hero }) {
  return (
    <figure className="rl-hero reveal">
      {/* The stage clips; the caption sits outside it, so the phones can run
          off the bottom edge without taking the words with them. */}
      <div className="rl-hero-stage">
        <div className="rl-hero-ground" aria-hidden="true">
          <span className="rl-hero-blob rl-hero-blob-a" />
          <span className="rl-hero-blob rl-hero-blob-b" />
          <span className="rl-hero-blob rl-hero-blob-c" />
        </div>

        <div className="rl-hero-fan">
          {hero.shots.map((index, i) => (
            <div className="rl-hero-slot" key={index} data-slot={i}>
              <img
                src={SHOTS[index].src}
                alt={i === 1 ? SHOTS[index].alt : ''}
                aria-hidden={i !== 1}
                width="592"
                height="1322"
                loading={i === 1 ? 'eager' : 'lazy'}
                decoding="async"
              />
            </div>
          ))}
        </div>
      </div>

      <figcaption>{hero.caption}</figcaption>
    </figure>
  )
}

/* -------------------------------------------------------------------- toc */

/**
 * The rail.
 *
 * Eleven sections is more than anybody reads top to bottom, so the rail is the
 * page's real table of contents rather than decoration: it says how long this
 * is, and it lets someone who came for the sleep timer go straight to it.
 *
 * The active item is chosen by whichever heading last crossed the top third of
 * the viewport, not by whichever section is "most visible" — a long section
 * followed by a short one otherwise hands the highlight over halfway through a
 * paragraph you are still reading.
 */
function Toc({ sections, active, onJump }) {
  return (
    <nav className="rl-toc" aria-label="On this page">
      <p className="rl-toc-head">On this page</p>
      <ol>
        {sections.map((section, index) => (
          <li key={section.id}>
            <a
              href={`#${section.id}`}
              data-on={active === section.id}
              onClick={() => onJump(section.id)}
            >
              <i aria-hidden="true">{String(index + 1).padStart(2, '0')}</i>
              {section.nav}
            </a>
          </li>
        ))}
      </ol>
    </nav>
  )
}

/* ----------------------------------------------------------------- blocks */

/** One case per block type in `release.js`, and no generic HTML escape hatch. */
function Block({ block }) {
  switch (block.type) {
    case 'lede':
      return <p className="rl-lede">{block.text}</p>

    case 'p':
      return <p className="rl-p">{block.text}</p>

    case 'quote':
      return (
        <blockquote className="rl-quote">
          <p>{block.text}</p>
        </blockquote>
      )

    case 'note':
      return (
        <aside className="rl-note">
          <p className="rl-note-title">{block.title}</p>
          <p>{block.text}</p>
        </aside>
      )

    case 'list':
      return (
        <ul className="rl-list">
          {block.items.map((item) => (
            <li key={item.title}>
              <b>{item.title}</b>
              <span>{item.body}</span>
            </li>
          ))}
        </ul>
      )

    case 'bullets':
      return (
        <ul className="rl-bullets">
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )

    case 'cards':
      return (
        <div className="rl-cards">
          {block.items.map((item) => (
            <div className="rl-card" key={item.label}>
              <b>{item.value}</b>
              <i>{item.label}</i>
              <span>{item.body}</span>
            </div>
          ))}
        </div>
      )

    case 'code':
      return (
        <pre className="rl-code">
          {block.lines.map((line, i) => (
            <span key={line}>
              {line}
              {i < block.lines.length - 1 ? '\n' : ''}
            </span>
          ))}
        </pre>
      )

    case 'figure':
      return (
        <figure className="rl-figure">
          <div className="rl-figure-frame">
            <img
              src={SHOTS[block.shot].src}
              alt={SHOTS[block.shot].alt}
              width="592"
              height="1322"
              loading="lazy"
              decoding="async"
            />
          </div>
          <figcaption>{block.caption}</figcaption>
        </figure>
      )

    default:
      return null
  }
}

/* ------------------------------------------------------------------ share */

/** Copy the canonical URL. The one action a document page owes a reader. */
function CopyLink() {
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (!done) return
    const id = window.setTimeout(() => setDone(false), 1800)
    return () => window.clearTimeout(id)
  }, [done])

  const copy = async () => {
    const url = `https://exhale.ozyern.me${RELEASE_PATH}`
    try {
      await navigator.clipboard.writeText(url)
      setDone(true)
    } catch {
      // Denied, insecure context, or an old browser. Selecting the address bar
      // is the fallback everyone already knows, so say nothing and do nothing.
    }
  }

  return (
    <button type="button" className="rl-copy" onClick={copy} data-done={done}>
      <LinkIcon />
      {done ? 'Link copied' : 'Copy link'}
    </button>
  )
}

/* ------------------------------------------------------------------- page */

export default function Release() {
  const [active, setActive] = useState(RELEASE.sections[0].id)
  const pending = useRef(null)
  const barRef = useRef(null)

  useReveals()

  // Every page mount starts at the top. Without this, arriving from the home
  // page's announcement strip drops you wherever the home page was scrolled to.
  useEffect(() => {
    const hash = window.location.hash.slice(1)
    const target = hash && document.getElementById(hash)
    if (target) target.scrollIntoView()
    else window.scrollTo(0, 0)
  }, [])

  // Reading progress, written straight to a custom property from a throttled
  // handler. Nothing here re-renders React; the browser scales one bar.
  useEffect(() => {
    const bar = barRef.current
    if (!bar) return

    let frame = 0
    const apply = () => {
      frame = 0
      const doc = document.documentElement
      const span = doc.scrollHeight - window.innerHeight
      const read = span > 0 ? window.scrollY / span : 0
      bar.style.setProperty('--read', Math.max(0, Math.min(1, read)).toFixed(4))
    }
    const onScroll = () => {
      if (!frame) frame = requestAnimationFrame(apply)
    }

    apply()
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll)
    return () => {
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('resize', onScroll)
      if (frame) cancelAnimationFrame(frame)
    }
  }, [])

  // Rail spy. The band is the top third: a heading owns the rail from the
  // moment it reaches the top of the reading area until the next one does.
  useEffect(() => {
    if (!('IntersectionObserver' in window)) return

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          // A click names its destination and everything it scrolls past is
          // ignored until that destination arrives, or the rail stutters
          // through nine positions on its way to the tenth.
          if (pending.current) {
            if (entry.target.id !== pending.current) return
            pending.current = null
          }
          setActive(entry.target.id)
        })
      },
      { rootMargin: '-14% 0px -72% 0px', threshold: 0 },
    )

    RELEASE.sections.forEach(({ id }) => {
      const node = document.getElementById(id)
      if (node) io.observe(node)
    })
    return () => io.disconnect()
  }, [])

  const jump = (id) => {
    pending.current = id
    setActive(id)
    // The lock always expires: a destination that never arrives — an anchor
    // already on screen, an interrupted scroll — would otherwise leave the spy
    // deaf for the rest of the session.
    window.setTimeout(() => {
      pending.current = null
    }, 1400)
  }

  return (
    <div id="top" className="release">
      <TopBar />
      <div className="rl-progress" ref={barRef} aria-hidden="true">
        <span />
      </div>

      <main>
        <article>
          <SplitTitle />

          <header className="rl-head">
            <div className="rl-measure">
              <Link className="rl-back" to="/">
                <span className="rl-back-arrow" aria-hidden="true">
                  <ArrowLeftIcon />
                </span>
                Exhale
              </Link>

              <p className="rl-eyebrow reveal">
                <span>{RELEASE.kicker}</span>
                <time dateTime={RELEASE.dateISO}>{RELEASE.date}</time>
                <span className="rl-read">{RELEASE.read}</span>
              </p>

              <p className="rl-dek reveal" style={{ '--d': '90ms' }}>
                {RELEASE.dek}
              </p>

              <div className="rl-actions reveal" style={{ '--d': '180ms' }}>
                <a className="btn" href={RELEASES} target="_blank" rel="noreferrer">
                  Download {RELEASE.version}
                </a>
                <a className="btn btn-ghost" href={REPO} target="_blank" rel="noreferrer">
                  Read the source
                </a>
                <CopyLink />
              </div>

              <dl className="rl-facts reveal" style={{ '--d': '260ms' }}>
                {RELEASE.facts.map((fact) => (
                  <div key={fact.label}>
                    <dt>{fact.label}</dt>
                    <dd>{fact.value}</dd>
                  </div>
                ))}
              </dl>
            </div>

            <ReleaseShots hero={RELEASE.hero} />
          </header>

          <div className="rl-body">
            <div className="rl-rail">
              <Toc sections={RELEASE.sections} active={active} onJump={jump} />
            </div>

            <div className="rl-flow">
              {RELEASE.sections.map((section, index) => (
                <section className="rl-section" id={section.id} key={section.id}>
                  <p className="rl-num" aria-hidden="true">
                    {String(index + 1).padStart(2, '0')}
                  </p>
                  <h2 className="rl-h2">{section.title}</h2>
                  {section.blocks.map((block, i) => (
                    <Block block={block} key={`${section.id}-${i}`} />
                  ))}
                </section>
              ))}

              <section className="rl-section rl-foot">
                <h2 className="rl-h2">Footnotes</h2>
                <ol className="rl-footnotes">
                  {RELEASE.footnotes.map((note) => (
                    <li key={note}>{note}</li>
                  ))}
                </ol>
              </section>

              <div className="rl-more">
                <h2 className="rl-h2">More</h2>
                <div className="rl-more-grid">
                  <Link className="rl-more-card" to="/">
                    <span className="rl-more-tag">Overview</span>
                    <b>Exhale — a music player that breathes</b>
                    <span className="rl-more-body">
                      Live glass on every surface, lyrics that land on the beat,
                      and color that follows whatever is playing.
                    </span>
                  </Link>
                  <a
                    className="rl-more-card"
                    href={`${REPO}/blob/master/CHANGELOG.md`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <span className="rl-more-tag">Changelog</span>
                    <b>Every fix, with the reason it broke</b>
                    <span className="rl-more-body">
                      The full document, written for someone reading the diff
                      rather than deciding whether to install.
                    </span>
                  </a>
                  <a
                    className="rl-more-card"
                    href={`${REPO}/blob/master/CONTRIBUTING.md`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <span className="rl-more-tag">Contributing</span>
                    <b>Build it, change it, send it back</b>
                    <span className="rl-more-body">
                      The toolchain, the flavors, the signing setup and what a
                      good pull request looks like here.
                    </span>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </article>
      </main>

      <SiteFooter />
    </div>
  )
}
