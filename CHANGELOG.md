# Changelog

All notable changes to Exhale are recorded here. Versions follow [semantic versioning](https://semver.org),
and the format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Release notes for the build you are running are also bundled into the app itself, under
**Settings → Updates → Changelog**. Every later version's notes are pulled from this repository's
[GitHub releases](https://github.com/ozyern/Exhale/releases).

## [1.0.102] — 2026-08-15

First public release.

### Added

**Your Sound Chem** — a monthly listening capsule, replacing the old statistics table.

- Month-by-month only: the month is the page title, and a pair of chevrons steps through the months
  you have history for. The mode dropdown (Continuous / Weeks / Months / Years) and the period chip
  rail are gone — a capsule is a period, so the period is no longer something you configure.
- Total listening time counts up on every period change, with an hours line beneath the minutes.
- Top artist and top song as a two-up spotlight row, each tinted with its own accent.
- Share of listening: the dominant artist at poster size with their percentage, shown only when one
  artist is actually ≥5% of the month.
- Listening breakdown: the top five artists as proportional bars that grow from empty. This replaces
  a pie chart made of cropped artist photographs.
- Share button emits a plain-text summary of the month.

**Lock screen lyrics on OxygenOS / ColorOS ("Live Space")**

- Timed lyrics are published to the active media session under the OPlus `lyricInfo` key, so the
  lock screen's music focus mode shows real lyrics instead of "No lyrics".
- Lyrics already in the cache are attached to the next few queue items *before* they start, so the
  track's very first metadata push already carries them. This is what the OPlus spec asks for, and
  it avoids the debounce that eats a payload patched on a few milliseconds late.
- When lyrics only resolve after playback has started there is nowhere to hide, so the payload is
  forced through — at most twice, with the retry window the spec allows. Position updates,
  pause/resume and notification refreshes never rewrite it.
- Forcing works by also changing `requestMetadata.mediaUri`, the one field Media3's session layer
  compares that this app does not use. Both `ExoPlayerImpl` and `MediaSessionLegacyStub` decide
  whether metadata changed with `MediaMetadata.equals`, which compares `extras` only by whether it
  is null — so a change that lives *only* in a lyric payload is invisible to both, and every item
  here already carries extras. Without this the payload was written into a void.
- TTML lyrics are flattened to LRC rather than dropped. The word-synced providers return TTML, so
  the format the lyrics screen looks best with was the one the lock screen never saw.
- Word timings, where a provider supplies them, are published as `rawLyric` for ROMs that
  highlight per syllable.
- Unsynced lyrics are deliberately never published — the lock screen cannot scroll them, and a
  payload it cannot use displaces the fallback it would otherwise show.
- Manifest opt-in for the OPlus media-history stack, so the ColorOS media card survives the app
  being fully stopped.
- Toggle under **Settings → Content → Lock screen lyrics**. Inert on every other ROM.
  `adb logcat -s LiveLyrics` reports which path each track took.

**In-app updates**

- Updates download and install inside the app instead of handing you off to a browser: real byte
  progress, a spring-eased transfer bar, and a direct handoff to the system installer.
- Release notes for the installed build are compiled in, so they are readable offline, on first
  launch, and before any release exists on GitHub. They fold out of the changelog card and
  disappear once you are past the version they describe.
- Software Update page rebuilt: a breathing glass hero whose glyph spins while a check runs, a
  status line that slides between states, and three glass groups.

**Home**

- Spotify-style shortcut grid at the top: two columns of squat tiles, artwork flush to the left
  edge, six destinations on screen at once.
- Apple Music large title — the weekday over a heavy display-size heading, inside the scroll content
  so it passes under the compact chrome as you scroll.

**About**

- Hidden breathing pacer behind seven taps on the app mark: an orb that swells over four seconds
  and empties over six, inside a field of a hundred and fifty motes that the inhale draws in and
  the exhale throws back out. Three counter-rotating sweeps cross the field, a ring closes once per
  half-breath, and touching anywhere pushes a ripple out from your finger. The whole thing is one
  canvas driven by one clock read inside the draw pass, so a frame costs a draw and nothing else.
  The app mark visibly winds up from the fourth tap onward.

**Recommendations**

- `Accept-Language` is now sent on every Innertube request. The `hl`/`gl` in the JSON context were
  the only statement of language we made, and the surrounding Google frontend does not read them —
  which is how a request that said "English" could still be answered out of the caller's IP region.
- The cached visitor id is re-minted when the content language or country changes. It used to be
  minted once on first launch and kept forever, so a user who installed in one region and *then*
  chose a language kept introducing themselves with the identity from before the choice. Skipped
  when a cookie or poToken is present, since a poToken is bound to the visitor id it was made for.
- Picking a language during onboarding now always takes effect. Two different "unset" sentinels had
  been written to the content-language preference over the app's history, and onboarding only
  recognised one of them, so anyone who had ever chosen "System default" in Settings could pick a
  language in onboarding and have it silently ignored.
- The feed filter now recognises romanized titles. It could only judge a title by its script, which
  passes "Kesariya" and "Laung Laachi" at 100% Latin — the actual shape of the leak on an Indian
  connection, rather than an edge case. A deliberately small vocabulary of distinctive whole words
  now marks those. It narrows the leak; a one-word title that is simply a name stays
  unclassifiable from the client.

### Changed

**Design system**

- New shared liquid-glass surface: gradient fill, diagonal sheen, hairline rim, and an optional
  accent tint. Used by the Sound Chem deck, the Updates page, the account sheet and About, so they
  can no longer drift apart.
- New animated aurora backdrop — three colour blobs drifting on a 26-second cycle, driven entirely
  through `graphicsLayer` so a frame of drift costs no recomposition and no layout.

**Accounts sheet** — redesigned.

- One identity block instead of two. The sheet used to open with a large centred avatar and then
  repeat the same avatar, name and email in a card directly beneath it.
- The four things people open the sheet for — Sound Chem, History, Settings, Sign in/out — are now
  tiles under the identity card rather than rows scattered across three captioned groups.
- Groups are real glass rather than a flat 5% ink wash, which on a translucent sheet read as a grey
  smudge.

**Settings**

- A visible search field under the large title, replacing a magnifier in the app bar.
- Every group is labelled. The quick actions and integrations were previously bare cards with
  nothing naming them.
- Divider indent now derives from row geometry (16 + 36 + 14 = 66dp). At 60dp every hairline in
  Settings stopped 6dp short of the text it aligned to.
- Group headers sit 20dp from the screen edge instead of 36dp, where they lined up with nothing.
- Rows fill with a neutral grey on press instead of shrinking to 98% and tinting themselves with the
  brand colour — that is button behaviour, and a grouped table is a list of destinations.
- The header plate is now a real destination: it opens About.

**About** — rebuilt as an inset grouped table on the same ground as the rest of Settings, replacing a
stack of elevated cards with shadows, shimmer sweeps and fake hover states.

- Maintainer credited as Aditya Jha, avatar served from the GitHub profile.
- Social links reduced to GitHub, Telegram and Instagram.
- App information group: version and build.

**Home**

- Section headers end in a small grey chevron rather than a full-size filled forward arrow.
- Keep Listening became the shortcut grid and moved to the top of the page.

### Removed

- Update channel picker. Only one channel is published, so the choice was between one real option
  and one that does not exist.
- The Development section (raw commit feed) from the Updates page — it answered a question nobody
  standing on an update screen is asking. Full history remains under Changelog.
- Contributors section from About.
- Website, PayPal and Facebook links.
- The pie chart of cropped artist photographs from the statistics screen.

[1.0.102]: https://github.com/ozyern/Exhale/releases/tag/v1.0.102
