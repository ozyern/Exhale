---
name: Bug Report
about: Report something broken in Exhale
title: "[BUG] "
labels: bug
---

## What happened

<!-- What went wrong, in a sentence or two. -->

## What you expected

<!-- What should have happened instead. -->

## Steps to reproduce

1.
2.
3.

## Environment

| | |
|---|---|
| Exhale version | <!-- Settings → About, e.g. 1.0.202 --> |
| Android version | <!-- e.g. Android 14 --> |
| Device | <!-- e.g. Pixel 8 Pro, Xiaomi Mi 10 --> |
| ROM / skin | <!-- e.g. stock, HyperOS 2, One UI 6 — often the deciding detail --> |

The ROM matters more than you would think. Manufacturer battery management, audio
routing and blur support all differ, and plenty of bugs only exist on one OEM's build.

## How often

- [ ] Every time
- [ ] Sometimes
- [ ] Once, and I could not reproduce it

## Evidence

<!-- Screenshots or a screen recording, if it is something you can see. -->

## Logs

<!-- Optional but very helpful. With the device connected over ADB:
     adb logcat | grep -i exhale
-->

```
paste logs here
```

## Checklist

- [ ] I searched for an existing report
- [ ] I am on the latest version of Exhale
- [ ] I can reproduce this
