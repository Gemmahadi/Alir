# Alir — fork of Fulguris

**Alir** is an Android web browser by GEMMA WORKS, built from the open-source
[Fulguris](https://github.com/Slion/Fulguris) browser by Stéphane Lenclud.
Package: `com.alir.browser`, version 1.0.10 (code 11). Debug builds install as
`com.alir.browser.debug` (`.debug` suffix from the debug build type).

## What changed from Fulguris (v1 rebrand)

- App name: Fulguris → **Alir** (all locales, `app_name` + user-visible strings).
- Application ID: `net.slions.fulguris` → `com.alir.browser` (slions flavor,
  `.full` and `.fdroid` suffixes removed, so the fdroid release installs clean).
- Version: 1.0.9 / code 10 (v1.0.0 / code 1 was the first CI-built APK).
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
- Toolbar (v1.0.1): always-visible Media button with a badge (hides at zero),
  next to the menu button; the menu bolt is tinted Alir cyan.
  Badge (v1.0.2): counts only the current tab's page host, refreshed on
  sniff, page start, tab change and resume; the hub itself keeps every item.
  (v1.0.1 badge was the session total, which piled up across sites.)
- Mini-player (v1.0.1): navy bar inside the toolbar layout (follows the
  toolbar top/bottom) while audio plays — title + play/pause + close, driven
  by `NowPlaying` from `AudioService`. Audio only, not video.
- Audio hardening (v1.0.1): the service goes foreground immediately with a
  loading notification (Android 12+ timeout), a failed load stops it cleanly
  instead of crashing, and a missing URL stops it at once.
- Audio fixes (v1.0.2): every new item rebuilds the player AND session, so
  the previous site's Referer/UA/cookies can never leak into the next site's
  requests (v1.0.1 reused the first player — real cross-site header leak);
  mini-player actions use a plain start (not foreground start) and a stale
  toggle with no session stops at once (Android 12+ timeout crash edge);
  a failed load toasts "Gagal memutar audio" instead of dying silently;
  the audio player disables video tracks, so "listen only" on HLS/DASH does
  not download video segments (progressive mp4 still downloads in full).
- Menus (v1.0.1): Sessions and Incognito start hidden (`HiddenMenu`,
  restorable from menu settings) so first-run menus stay focused.
- Brand colors (v1.0.1): `alir_navy` #041c51 + `alir_cyan` #00d5ff for the new
  surfaces (badge, mini-player, toolbar accents). The Material theme itself
  is untouched — a full theme recolor needs on-device visual checks.
- Still NOT tested on a real phone (Oct 2026): CI compiles it, but playback,
  PiP, badge and mini-player behavior need on-device testing.
- Deferred: snackbar on first detection per page.

## Build history (GitHub Actions, repo Gemmahadi/Alir)

- v1.0.0 debug (run 40631335804, 5th run): first success. Earlier runs failed
  on: `yes | sdkmanager` under pipefail (SIGPIPE), `setup-android@v3`,
  AAPT `show_controller` (no such PlayerView attr), Kotlin `when` missing the
  Media branch. Fixes are in `.github/workflows/build-alir.yml` and the code.
- v1.0.1 debug (run 40636230118): success. Batch: headers, listen-only,
  audio hardening, toolbar button + badge, mini-player, declutter, brand
  colors.
- v1.0.2 debug (run 40640637236): success. Batch: header-leak fix (rebuild
  player per item), mini-player action crash edge, play-failure toast,
  audio-only tracks, per-host badge.
- v1.0.3: track-end stop (no stale notification/mini-player), track title
  in MediaItem metadata (media3 notification/lock-screen title), this
  build-history record.
- v1.0.4: LeakCanary removed from debug builds (its launcher alias showed
  a second "Leaks" icon next to Alir — confusing for users). No app code
  called LeakCanary APIs, so removal is dependency-only; the dead
  "LeakCanary" toggle in Developer settings and its strings remain (no-op).
- v1.0.5: diagnostic crash reporter (open lightning-button crash, cause
  unknown from static analysis). `CrashReporter` catches uncaught
  exceptions, saves the stack trace to internal storage, chains to the
  previous handler (system FC dialog still shows), and on the next launch
  shows the report in a dialog with Salin/Tutup (either deletes the file).
  Native/WebView crashes produce NO report — that absence is diagnostic.
- v1.0.6: the v1.0.5 dialog died with the activity — the app crashed AGAIN
  during startup before the user could tap Salin, so the report could never
  be copied. New `CrashReportActivity`: a minimal activity (no WebView, no
  tabs, no session restore) launched from `SplashActivity` instead of the
  browser whenever a report exists, with Salin/Lanjut buttons; either button
  deletes the file and continues to the browser. The dialog path was removed.
  Note: the lightning crash's own report was overwritten by the startup
  crash, so the next report copied will be the startup crash — fix that
  first, then re-reproduce the lightning crash.
- v1.0.7: ROOT CAUSE FOUND (from the 1.0.6 crash report, Xiaomi/Android 13):
  the sniffer hook in `WebPageClient.shouldInterceptRequest` called
  `view.settings.userAgentString` on Chromium's IO thread, where WebView
  methods throw (`checkThread` RuntimeException). That single line was the
  only `view.*` call in the whole interceptor. Fix: UA is now cached on the
  UI thread in `onPageStarted` (after desktop mode may change it) and the
  interceptor uses the cache, falling back to the static
  `WebSettings.getDefaultUserAgent` before any page starts. This explains
  both the lightning-tap crash (a page was loading) and the startup crash
  (restored tab loading) — same code path.
- v1.0.8: default bookmarks are Alir's, not Fulguris's. The slions-brand
  seed (`app/src/slions/res/raw/default_bookmarks.dat`, the one our flavor
  actually uses) and the main seed were both replaced with an "Alir"
  folder: DuckDuckGo, Wikipedia (id), YouTube, Google. Plus a one-time
  migration in `App.onCreate` (flag `alir_bookmarks_migrated`): existing
  installs keep their own bookmarks, but the exact Fulguris default URLs
  (folder "Fulguris" or titled "Fulguris home") are deleted and the Alir
  set is imported if the Alir folder does not exist yet. The seed alone
  would never have changed existing installs (it only runs when count==0).
  Still Fulguris-linked (not changed — no Alir site exists to point to):
  About rows for home page / terms / privacy (`url_app_home_page`,
  `url_terms_and_conditions`, `url_privacy_policy` in donottranslate.xml).
  Build history: v1.0.8 = CI run 40675022543, commit d903c3b, APK
  24,768,777 B, SHA-256 0f2e17fc…12087, verified (badging code 9 / 1.0.8,
  apksigner OK, 1 LAUNCHER, 0 leak, new seed + migration flag in dex).
- v1.0.9: legacy launcher icons replaced. The adaptive icon was already
  the Alir logo, but all 20 legacy mipmap .webp files (ic_launcher,
  ic_launcher_round, ic_launcher_incognito, ic_launcher_incognito_round ×
  mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) still held the old Fulguris bolt, and
  MIUI renders the legacy path — so the home screen showed the bolt.
  Regenerated from the 1600px Alir tile (script
  ~/workspace/gen_alir_legacy_icons.py): square = scaled tile, round =
  circle crop. Verified visually at xxhdpi. Note: after updating, MIUI
  may still show the cached old icon — if so, reboot or remove + re-add
  the home icon.

## Licenses

Files under Stéphane Lenclud's sole copyright: CPAL-1.0. All other files:
MPL-2.0. See LICENSE-CPAL-1.0 and LICENSE-MPL-2.0.
