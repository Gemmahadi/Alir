# Alir — fork of Fulguris

**Alir** is an Android web browser by GEMMA WORKS, built from the open-source
[Fulguris](https://github.com/Slion/Fulguris) browser by Stéphane Lenclud.
Package: `com.alir.browser`, version 1.0.0 (code 1).

## What changed from Fulguris (v1 rebrand)

- App name: Fulguris → **Alir** (all locales, `app_name` + user-visible strings).
- Application ID: `net.slions.fulguris` → `com.alir.browser` (slions flavor,
  `.full` and `.fdroid` suffixes removed, so the fdroid release installs clean).
- Version: 1.0.0 / code 1.
- Custom URL scheme: `fulguris://` → `alir://`.
- Launcher icon: new Alir mark (glowing cyan wave + white play triangle on navy).
- About screen: new "Berdasarkan Fulguris" row linking to the upstream repo
  (CPAL attribution). LICENSE files are kept as-is.
- Contribute screen: rows pointing at Fulguris money/Play/Crowdin/share are
  hidden for v1 (Alir has no Play page or sponsor page yet).
- Java package stays `fulguris.*` internally (renaming it is not needed: the
  applicationId is what Android and users see).

## Build

- Variant: `slionsFullFdroidRelease` (no Firebase, no google-services.json).
- Note: the fdroid flavor never runs the slions.net update check (that check is
  gated to the download flavor), so no update-check code had to change.
- **Local builds do NOT work in the robo VM** (Oct 2026): Java loopback TCP is
  broken there (a Java client connects but its first write gets EPIPE and the
  Java server never accepts; Python loopback works fine). Gradle's daemon is
  pure Java↔Java loopback, so it can never run there, on any Gradle version.
  Build via GitHub Actions instead (healthy runners, direct internet).

## Media Sniffer (Alir differentiator, written Oct 2026, NOT yet compiled)

- Hook: `WebPageClient.shouldInterceptRequest`, right after the adblock check
  (blocked requests are skipped) → `MediaSniffer` (package `fulguris.media`).
- Detection: URL extension only (`.mp4/.webm/.mov/.m3u8/.mpd` = video;
  `.mp3/.m4a/.ogg/.oga/.wav/.flac/.aac` = audio), de-duplicated, capped at 200,
  newest first.
- Hub: main-menu item "Media" → `MediaHubSheet` bottom sheet (list, tap to play).
- Video: `PlayerActivity` (ExoPlayer, picture-in-picture when leaving).
- Audio: `AudioService` (`MediaSessionService`, notification controls, keeps
  playing while browsing).
- Deps: androidx.media3 1.7.1 (exoplayer, session, ui).
- NOT verified by compilation: local Gradle builds cannot run in the robo VM
  (Java loopback broken), so this code has only been checked by inspection
  (balanced syntax, valid XML, every referenced resource exists). The first
  CI build must verify it.
- Deferred to v1.1: mini-player bar inside the browser, snackbar on first
  detection per page.

## Licenses

Files under Stéphane Lenclud's sole copyright: CPAL-1.0. All other files:
MPL-2.0. See LICENSE-CPAL-1.0 and LICENSE-MPL-2.0.
