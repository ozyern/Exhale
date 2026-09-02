# Contributing to Exhale

Exhale is a music player for Android, written entirely in Kotlin and Jetpack Compose.
Pull requests are welcome — a bug fix, a UI tweak, a translation, even a typo.

Before you start, please read the [Code of Conduct](CODE_OF_CONDUCT.md). Everything you
contribute ships under the [GNU GPL v3](LICENSE), the same license as the rest of the app.

---

## Setting up

You need:

- **JDK 21.** Not 17 — the project compiles against `JavaVersion.VERSION_21` and targets
  `JVM_21`, and an older JDK fails at configuration time. Android Studio ships a suitable
  runtime in its bundled JBR.
- **Android Studio** recent enough to run AGP with `compileSdk = 37`.
- **SDK Platform 37** installed through the SDK Manager. The app *runs* on API 33+
  (`minSdk = 33`, `targetSdk = 36`) but it *compiles* against 37.

```bash
git clone https://github.com/ozyern/Exhale.git
cd Exhale
./gradlew assembleUniversalDebug
```

The APK lands in `app/build/outputs/apk/universal/debug/`.

> [!IMPORTANT]
> Use `assembleUniversalDebug`, not `assembleDebug`. The app has five ABI product
> flavors — `universal`, `arm64`, `armeabi`, `x86`, `x86_64` — so the flavorless task
> builds all five and takes roughly five times as long. `universal` carries every ABI
> and is the one to develop against; `arm64` is the fastest if you only ever test on a
> modern phone.

On Windows, point `JAVA_HOME` at the Studio runtime before invoking Gradle:

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
```

Debug builds sign themselves. Release builds need the signing keystore and will not work
from a fork — see [RELEASING.md](RELEASING.md), which is maintainer-only.

---

## Where the code lives

`:app` is the application: UI, playback, database, DI. Everything else is a library
module wrapping one external service, so a change to a single provider stays contained.

| Module | What it is |
| --- | --- |
| `:app` | The app. Compose UI, Media3 playback, Room database, Hilt graph, widgets |
| `:innertube` | YouTube Music client — search, browse, streams |
| `:spotify` | Spotify metadata and playlist import |
| `:lrclib`, `:kugou`, `:betterlyrics`, `:simpmusic` | Lyrics providers, tried in order |
| `:lastfm` | Last.fm scrobbling |
| `:kizzy` | Discord Rich Presence |
| `:canvas` | Looping canvas videos on the player |
| `:shazamkit` | Audio recognition |

Inside `:app`, the parts you are most likely to touch:

```
ui/screens     whole screens
ui/component   reusable widgets, including the glass surfaces
ui/player      the now-playing screen and its transitions
ui/theme       color, typography, album-art color extraction
playback       the Media3 service, queue, downloads
lyrics         provider selection and sync
db             Room entities, DAOs, migrations
```

---

## Code style

There is no ktlint, spotless, or detekt in this project. Review is the only formatter, so
the rule is: **match the code around your change** — its comment density, its naming, its
idiom.

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
  4 spaces, `camelCase` members, `PascalCase` types, `UPPER_SNAKE_CASE` constants.
- Composables are `PascalCase`. Take `modifier: Modifier = Modifier` as the first
  defaulted parameter, and hoist state to the caller rather than holding it inside.
- User-visible text goes in `app/src/main/res/values/strings.xml`. Never hardcode a
  string in Kotlin — it cannot be translated, and translators only ever see that file.
- **American spelling** throughout, in code, strings, and comments. `color`, not `colour`.
- Comments should say *why*, not *what*. The code already says what it does.

### Two things that will bite you

**Lint will not stop you from shipping a crash.** The app sets `abortOnError = false`, so
a `NewApi` violation is a warning you can walk straight past. It becomes a
`NoSuchMethodError` on a real device. If you call an API above `minSdk = 33`, guard it
yourself, and write the check as a literal comparison at the call site:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { ... }
```

Lint only follows literal `SDK_INT` comparisons. Folding the check into a boolean variable
is correct at runtime but invisible to lint, so nothing will warn you the next time
someone moves the call.

**The glass surfaces are shared.** The blur-and-translucency system under `ui/component`
backs the dock, the sheets, and the player. It is tuned, it degrades deliberately on
devices without cross-window blur, and it is easy to break everywhere at once. Change it
in one place, explain why in the PR, and expect the visual result to be looked at closely.

---

## Tests

Unit tests live in `app/src/test/`. There are no instrumentation tests.

```bash
./gradlew testUniversalDebugUnitTest
```

Add tests for logic that is worth being sure about — parsing, ordering, version
comparison, sync planning. That is what the existing suites cover. Don't write Compose
snapshot tests; nothing here is set up for them.

---

## Commits

[Conventional Commits](https://www.conventionalcommits.org/), with a subject line that
says what changed in plain language:

```
feat: one APK per release, named after the build it is
fix: an empty release list is "up to date", not a failed check
```

Types in use: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`,
`revert`.

---

## Pull requests

`master` is the only long-lived branch. Fork, branch off `master`, and open the PR against
it.

1. Make the change and check it runs on a device or emulator — this is a music player, and
   most of what can go wrong is visible or audible, not compilable.
2. Run the tests.
3. Update `CHANGELOG.md` if the change is user-facing.
4. Open the PR.

CI builds a debug APK on every push to an open PR and comments with a link to it, so
reviewers can install your branch without building it. Doc-only changes skip the build.

Your description should cover:

- **What** the change does
- **Why** it is worth making
- **How**, briefly, if the approach isn't obvious from the diff
- **Screenshots or a screen recording** for anything visual — required, not optional
- **Related issues**, as `Closes #123`

```
### Summary
Falls back to the next lyrics provider when LRCLIB returns a track with no
timestamps, instead of showing an empty lyrics sheet.

### Motivation
LRCLIB will happily return a plain-text match for a song that has a synced
version elsewhere, and we were treating any 200 as success.

### Changes
- Treat a result with no timed lines as a miss, not a hit
- Continue to KuGou / BetterLyrics instead of stopping
- Added a test for the untimed-response case

Closes #45
```

Discuss anything large in an issue before you build it. A feature that doesn't fit the app
is a painful thing to find out about after you've written it.

---

## Translations

Translations are plain resource files — edit them directly and open a PR. There is no
Crowdin project for Exhale.

To improve an existing language, edit `app/src/main/res/values-<code>/strings.xml`.
Currently translated: Arabic, German, Spanish, Estonian, French, Hindi, Indonesian,
Italian, Hebrew, Japanese, Korean, Malayalam, Malay, Dutch, Portuguese (Brazil), Russian,
Turkish, Ukrainian, Vietnamese, Chinese (Simplified).

To add a language, create `values-<code>/strings.xml` using the Android locale code —
`values-fi`, `values-pt-rPT` — and copy across only the strings you have translated.
Missing strings fall back to English on purpose, so a partial translation is genuinely
useful and lint won't complain about it.

One hard rule: if you translate a `<plurals>`, include **every** quantity your language
requires. `MissingQuantity` is set to error, and an incomplete plural fails the build.

---

## Design

- The app is Material 3, extended with the glass surfaces and album-art color extraction.
  Read [the M3 guidelines](https://m3.material.io/), then look at what the app actually
  does — it deviates deliberately in places.
- Mockups are welcome as an issue labeled `design`. Include what problem the design
  solves; "it looks nicer" is hard to act on.
- Dark and light both have to work, and so does dynamic color pulled from album art. A
  design that only holds up against one wallpaper isn't finished.

---

## Reporting bugs

Open an [issue](https://github.com/ozyern/Exhale/issues), searching first in case it is
already there. Include:

- What you expected, and what happened instead
- Steps to reproduce
- **App version** (Settings → About), **Android version**, and **device**
- A screenshot or recording if it is visual
- A log if you have one

The device matters more than you would think: manufacturer battery management, audio
routing, and blur support all vary, and a bug that only appears on one OEM's ROM is a
common outcome.

## Suggesting features

Also an [issue](https://github.com/ozyern/Exhale/issues). Say what problem it solves
rather than what button to add, and mention how you would expect it to behave. Wait for a
maintainer to agree before writing the code.

---

Thanks for contributing. Questions go in an issue labeled `question`.
