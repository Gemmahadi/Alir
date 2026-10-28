# Alir — fork of Fulguris

**Alir** is an Android web browser by GEMMA WORKS, built from the open-source
[Fulguris](https://github.com/Slion/Fulguris) browser by Stéphane Lenclud.
Package: `com.alir.browser`, version 1.0.1 (code 2). Debug builds install as
`com.alir.browser.debug` (`.debug` suffix from the debug build type).

## What changed from Fulguris (v1 rebrand)

- App name: Fulguris → **Alir** (all locales, `app_name` + user-visible strings).
- Application ID: `net.slions.fulguris` → `com.alir.browser` (slions flavor,
  `.full` and `.fdroid` suffixes removed, so the fdroid release installs clean).
- Version: 1.0.1 / code 2 (v1.0.0 / code 1 was the first CI-built APK).
- Custom URL scheme: `fulguris://` → `alir://`.
- Launcher icon: new Alir mark (glowing cyan wave + white play triangle on navy).
- About screen: new "Berdasarkan Fulguris" row linking to the upstream repo
  (CPAL attribution). LICENSE files are kept as-is.
- Contribute screen: rows pointing at Fulguris money/Play/Crowdin/share are
  hidden for v1 (Alir has no Play page or sponsor page yet).
- Java package stays `fulguris.*` internally (renaming it is not needed: the
  applicationId is what Android and users see).

## Build

- Variant: `slionsFullFdroidDebug` (no Firebase, no google-services.json),
  built by `.github/workflows/build-alir.yml` on push to main. A signed
  release build needs a real keystore (later).
- Note: the fdroid flavor never runs the slions.net update check (that check is
  gated to the download flavor), so no update-check code had to change.
- **Local builds do NOT work in the robo VM** (Oct 2026): Java loopback TCP is
  broken there (a Java client connects but its first write gets EPIPE and the
  Java server never accepts; Python loopback works fine). Gradle's daemon is
  pure Java↔Java loopback, so it can never run there, on any Gradle version.
  Build via GitHub Actions instead (healthy runners, direct internet).

## Media Sniffer (Alir differentiator)

- Hook: `WebPageClient.shouldInterceptRequest`, right after the adblock check
  (blocked requests are skipped) → `MediaSniffer` (package `fulguris.media`).
- Detection: URL extension only (`.mp4/.webm/.mov/.m3u8/.mpd` = video;
  `.mp3/.m4a/.ogg/.oga/.wav/.flac/.aac` = audio), de-duplicated, capped at 200,
  newest first. YouTube and `blob:` streams are NOT detected (no extension).
- Hub: main-menu item "Media" → `MediaHubSheet` bottom sheet (list, tap to play).
  Video rows also offer "Dengarkan saja" (listen only) → background audio.
- Video: `PlayerActivity` (ExoPlayer, picture-in-picture when leaving).
- Audio: `AudioService` (`MediaSessionService`, notification controls, keeps
  playing while browsing).
- Deps: androidx.media3 1.7.1 (exoplayer, session, ui).
- Playback headers (v1.0.1): the sniffer keeps the page's Referer, the
  WebView User-Agent and the media host's cookies, and ExoPlayer replays the
  request with them (bare URLs got 403 from hotlink-protected hosts).
- Toolbar (v1.0.1): always-visible Media button with a session-total badge
  (hides at zero), next to the menu button; the menu bolt is tinted Alir cyan.
- Mini-player (v1.0.1): navy bar inside the toolbar layout (follows the
  toolbar top/bottom) while audio plays — title + play/pause + close, driven
  by `NowPlaying` from `AudioService`. Audio only, not video.
- Audio hardening (v1.0.1): the service goes foreground immediately with a
  loading notification (Android 12+ timeout), a failed load stops it cleanly
  instead of crashing, and a missing URL stops it at once.
- Menus (v1.0.1): Sessions and Incognito start hidden (`HiddenMenu`,
  restorable from menu settings) so first-run menus stay focused.
- Brand colors (v1.0.1): `alir_navy` #041c51 + `alir_cyan` #00d5ff for the new
  surfaces (badge, mini-player, toolbar accents). The Material theme itself
  is untouched — a full theme recolor needs on-device visual checks.
- Still NOT tested on a real phone (Oct 2026): CI compiles it, but playback,
  PiP, badge and mini-player behavior need on-device testing.
- Deferred: snackbar on first detection per page; per-page badge count
  (badge currently shows the session total).

## Build history (GitHub Actions, repo Gemmahadi/Alir)

- v1.0.0 debug (run 40631335804, 5th run): first success. Earlier runs failed
  on: `yes | sdkmanager` under pipefail (SIGPIPE), `setup-android@v3`,
  AAPT `show_controller` (no such PlayerView attr), Kotlin `when` missing the
  Media branch. Fixes are in `.github/workflows/build-alir.yml` and the code.
- v1.0.1: this batch (headers, listen-only, audio hardening, toolbar button
  + badge, mini-player, declutter, brand colors).

## Licenses

Files under Stéphane Lenclud's sole copyright: CPAL-1.0. All other files:
MPL-2.0. See LICENSE-CPAL-1.0 and LICENSE-MPL-2.0.
