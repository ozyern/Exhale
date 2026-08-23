# Releasing Exhale

A release is a tag. Everything else happens in CI.

```bash
# 1. Bump versionCode and versionName in app/build.gradle.kts
# 2. Write the entry in CHANGELOG.md under ## [1.0.103]
git commit -am "chore: 1.0.103"
git tag v1.0.103
git push origin master v1.0.103
```

`.github/workflows/release.yml` then builds all five ABI variants, signs them, and
publishes the release with the notes taken from `CHANGELOG.md`.

## The signing key

The keystore is the app's identity. An update signed with a different key is not an
update — Android refuses the install and the only way out is for every user to
uninstall and lose their library. There is no recovery if it is lost.

- It lives at `app/keystore/release.keystore`, which is gitignored.
- **Back it up somewhere that is not this machine**, together with its password.
- CI gets it from repository secrets, never from the repository.

Three secrets under *Settings → Secrets and variables → Actions*:

| Secret | What it is |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 app/keystore/release.keystore` |
| `STORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `exhale` |
| `KEY_PASSWORD` | the key password (same as the store password) |

Without them the release job fails on purpose. It does **not** fall back to the debug
key: a debug-signed APK on the release page can never be replaced by a properly signed
one, so it would poison the update path permanently.

A local release build needs the same three as environment variables:

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export STORE_PASSWORD=... KEY_ALIAS=exhale KEY_PASSWORD=...
./gradlew assembleUniversalRelease
```

Without them Gradle warns and signs with the debug key, which is fine for your own
device and is not a release.

## What the app expects to find

The in-app updater (`Updater.kt`) reads
`api.github.com/repos/ozyern/Exhale/releases` and downloads **`Exhale-universal.apk`**.

- **The tag must be semver in the 1.x line** — `v1.0.103`. Anything outside the app's
  current major line is ignored on purpose, so an old or foreign tag can never present
  itself as an update.
- **Only a strictly newer version counts.** Publishing a release equal to or older than
  what is installed leaves the Updates screen reading "Exhale is up to date".
- **The asset name is version-free** so that
  `releases/latest/download/Exhale-universal.apk` always resolves to the newest build.
  If it ever changes, installed copies fall back to any asset with `universal` in the
  name, then to any `.apk` — but the copy already on someone's phone is the one that
  cannot be fixed, so change it reluctantly.

The release job refuses to build when the tag and `versionName` disagree. A release
tagged 1.0.103 whose APK reports 1.0.102 would offer itself as an update forever.
