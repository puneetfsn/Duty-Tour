# Duty Tour — Hendrickson

A private, on-device **duty tour log** for one traveller at Hendrickson.

Each duty tour is one sitting: route, dates, what was spent (travel / hotel / other), the total,
and what work was done. Export it as a printable PDF to send to accounts.

This is not SAP and not a team app. It is a tour file that happens to fit in a phone.

- **Android app** — Kotlin, Jetpack Compose Material 3, Room, DataStore. Package `com.puneetfsn.dutytour`, minSdk 26.
- **Web app** — the same log in plain HTML/CSS/JS, for a laptop or a borrowed browser. No build step, no dependencies, no network.

## Hard rules this repo keeps

| Rule | How it is kept |
| --- | --- |
| No account, no cloud, no analytics, no ads | Nothing to sign into; no SDKs of that kind anywhere |
| No `INTERNET` permission | The Android manifest declares none. The web app makes no requests |
| On-device storage | Room (Android), `localStorage` (web) |
| `allowBackup="false"` | Set in the manifest, with cloud backup and device transfer excluded |
| Never asks for secrets | Amounts and place names only — no bank passwords, UPI PINs, card CVV, or OTPs |
| Debug APK for sideload | Built by GitHub Actions, uploaded as a workflow artifact |

The watermark is a style choice. It is **not** verification that anything is official.

## Android app

### Screens

1. **Tours** — route, dates, total. Quiet list, one row per tour.
2. **Add / edit tour** — totals update live as amounts are typed.
3. **Tour detail** — the full record.
4. **Export** — single tour, all tours, or by date range. *(next step)*
5. **Settings** — traveller name, company, default city, currency ₹, watermark on/off.

### Data model

`Tour` (Room, table `tours`):

| Field | Type | Notes |
| --- | --- | --- |
| `startLocation`, `destination` | `String` | Route |
| `startDate`, `endDate` | `Long` (`LocalDate.toEpochDay()`) | End date optional |
| `travelMode` | enum | Train / Bus / Cab / Flight / Other |
| `travelAmountPaise`, `travelNote` | `Int`, `String` | Money in **paise** |
| `hotelName`, `hotelNights`, `hotelAmountPaise` | | All optional |
| `otherAmountPaise`, `otherNote` | | Optional |
| `workTitle`, `workDetails` | `String` | What was done |
| `purposeLine` | `String` | Client / office / purpose, optional |

**Total is never stored.** `Tour.totalPaise` is computed as travel + hotel + other, so it can't drift.

Money is an `Int` of paise end to end; `util/Money.kt` is the only place that formats or parses it.
Dates are stored as epoch days so range filters are exact; `startDate`/`endDate` queries drive exports.

### Build

```bash
./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # money + total tests
```

Requires JDK 17 and the Android SDK (compileSdk 35).

### Sideload from CI

Every push builds a debug APK on GitHub Actions. Open the latest **Android debug APK** run,
download the `duty-tour-debug-apk` artifact, then install it on the phone
(`adb install app-debug.apk`, or copy it across and tap it).

## Web app

Open `web/index.html` in a browser, or serve the folder:

```bash
cd web && python3 -m http.server 8000
```

Run the smoke test (jsdom; walks every screen, submits the form, and renders the
single / all / date-range sheets):

```bash
cd web && npm install && npm test
```

### One file, no server

`web/duty-tour.html` is a single self-contained build — CSS and JS inlined, no
external requests. Copy it to a laptop or phone and open it directly (`file://`);
it keeps working with no server and no internet. Data stays in that browser's
`localStorage`, and printing to PDF works the same.

Regenerate it after editing the sources:

```bash
cd web && npm run build:single
```

CI fails if the committed file drifts from the sources.

### What it does

- Same fields as the Android app, with a live total.
- **Your name is entered once and saved** (`Settings`). It prints on every PDF and pre-fills each new
  entry; any entry can override it, so a shared browser still produces correct sheets.
- Multiple tours, kept in `localStorage` under `duty-tour:v1:*`.
- **Export / print**: single tour, all tours (one sheet per tour, oldest first), or a date range —
  with an optional summary sheet carrying the grand total. A4, `DUTY TOUR` heading, `Hendrickson`
  company line, spend table, work details, the footer *Personal duty-tour record. Not a GST invoice.*,
  and a light diagonal `Hendrickson` watermark (Settings → PDF → Watermark).
- Use the browser's print dialog → **Save as PDF**. The PDF is built by the browser on the device;
  there is no PDF library and nothing is uploaded.
- **Backup**: download a plain JSON file, restore it later. That is the only way data moves between
  devices, and you control the file.

### Notes

- Clearing site data or switching browsers starts empty — take a backup first.
- Tabs are hash routes (`#/tours`, `#/tour/<id>`, `#/export`, `#/settings`), so the browser back button works.

## Make the web app live

The site is plain static files, so it can be hosted anywhere that serves files.
`web/` is the site; `web/duty-tour.html` is the same app in one file.

### GitHub Pages (one-time switch)

1. Repo **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. Push to `main` (or run **Deploy web app to GitHub Pages** from the Actions tab).

The site then lives at `https://puneetfsn.github.io/Duty-Tour/`, and every push
redeploys it. Note: Pages on a **private** repository needs a paid GitHub plan; on a
free plan the Pages settings ask you to make the repo public first.

### Any other host

Upload the contents of `web/` (or just drag `duty-tour.html`) to Netlify Drop,
Cloudflare Pages, or any static host. Nothing server-side is required, and no data
is ever sent anywhere — the app is client-side only.

## Status

Done: project scaffold, Room `Tour` entity + DAO, list / add-edit / detail / settings screens,
live totals, CI debug APK, and the web app.

**Next, after your confirmation:** native `PdfDocument` export on Android — single tour, all tours,
and date range — with the diagonal `Hendrickson` watermark and `FileProvider` sharing. The detail
screen has the button placement reserved and disabled until then.

## Layout

```
app/src/main/java/com/puneetfsn/dutytour/
  data/            Tour entity, DAO, database, repositories (Room + DataStore)
  ui/list/         tour list
  ui/edit/         add / edit form + live totals
  ui/detail/       tour detail
  ui/settings/     traveller, company, city, watermark
  ui/common/       shared Compose pieces
  util/            Money (paise), Dates
web/               static web app: index.html, styles.css, app.js
web/duty-tour.html single-file build (open with no server)
web/build-single.mjs, web/tests/   build script + jsdom smoke test
.github/workflows/android-debug.yml
```
