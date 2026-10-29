# Shared Memory Photo Network — Progress & Handoff

Last updated: 2026-08-25

This document exists so work can be picked back up in a fresh session without re-deriving
context. Read this before making changes.

## 1. What this is

Event-scoped photo network: create an event, invite friends, everyone enrolls a face profile,
everyone takes photos normally, the app detects who's in each photo and automatically delivers it
to the right people based on configurable sharing rules. Full concept in
`Shared_Memory_Photo_Network_Product_Documentation.docx` (in Downloads, not in this repo) and the
implementation plan that was approved at `~/.claude/plans/eventual-gliding-wand.md`.

## 2. Status at a glance

| Phase | What it covers | Status |
|---|---|---|
| 1 | Backend scaffold (Spring Boot, Postgres, Flyway) | ✅ Done |
| 2 | Auth, Event, Membership, Invitations | ✅ Done |
| 3 | Face detection/embedding/matching pipeline (ONNX) | ✅ Done |
| 4 | Drools sharing-rules engine, Delivery, photo processing orchestrator | ✅ Done |
| 5 | Privacy controls beyond what already exists, retry/offline queue | ❌ Not started |
| 6 | Mobile app (React Native / Expo) | 🟡 Built, **not yet tested on a real device** |

Backend: **fully built and tested** (15 automated tests, all passing, plus repeated manual
end-to-end verification with real photos). Mobile: **all screens written, typechecks clean, bundles
successfully** — but nobody has opened it on an actual phone/simulator yet. That's the very next
step.

## 3. Tech stack (and why)

- **Backend**: Java 21, **Spring Boot 4.1.1** (not 3.x — the whole 3.x line hit Spring's
  end-of-open-source-support on 2026-06-30, so a new project today has to start on 4.x), Postgres
  16 (Flyway-migrated), Spring Security (JWT), **Drools 10.2.0** (sharing rules engine),
  **ONNX Runtime Java 1.26.0** (face detection + embedding — no Python, no cloud API).
- **Mobile**: **Expo SDK 57** (React Native 0.86, React 19.2), `expo-router` (file-based routing),
  TanStack Query + axios, `expo-image-picker`, `expo-secure-store`.
  - This was originally going to be a React **web** app; the user redirected mid-session to a
    real mobile app instead, so that's what got built. See §6 for what that trade-off means.

## 4. How to run everything

### Backend
```bash
cd ~/shared-memory-photo-network
docker compose up -d                      # Postgres — already running as of this writing
cd backend
./mvnw spring-boot:run                    # boots on :8080, Flyway migrates automatically
curl localhost:8080/actuator/health       # should return {"status":"UP"}
```
Face ML models must exist at `backend/models/arcfaceresnet100-8.onnx` and
`backend/models/version-RFB-320.onnx` — **already downloaded**, gitignored, ~262MB total. See
`backend/README.md` if they ever need re-downloading.

Run the backend test suite: `cd backend && ./mvnw test` (15 tests, ArchUnit + Drools + two
Testcontainers-backed full-stack integration tests using real face photos).

### Mobile (this is the part that needs YOUR device to actually test)
```bash
cd ~/shared-memory-photo-network/mobile
npx expo start
```
This prints a QR code. Scan it with the **Expo Go** app on your phone (same Wi-Fi network as this
machine). The app will load over the network.

**Before it'll work, check `mobile/src/api/config.ts`** — it hardcodes this dev machine's LAN IP
(`192.168.28.56`) as the API base URL, because a phone can't reach `localhost` on the dev machine.
If that IP has changed (new network, restarted router, etc.), get the current one with
`ipconfig getifaddr en0` and update that file.

The backend must be running (see above) for the app to do anything beyond the login screen.

## 5. What's built

### Backend — full detail in `backend/README.md` and inline code comments

- **identity**: register/login, JWT auth (`jjwt` 0.13.0)
- **event / membership**: create event, QR-code-ready invite codes, join, leave, remove, roles
- **face**: `FaceDetector` (Ultra-Light face detector) + `FaceEmbedder` (ArcFace) — pure ONNX,
  detection→crop→embed→cosine-similarity matching against enrolled `FaceProfile`s. Enrollment
  endpoint (3–5 selfies → centroid embedding; raw selfies never persisted).
- **rules**: Drools engine with 3 real rules (default self-delivery, private-photo-hides-gallery,
  group-photo-shows-gallery ≥3 people) wired via `sharing-rules.drl`. 4 more rule types exist in
  the schema (pair/subgroup/photographer/approval) but have no DRL yet — toggleable in the API but
  inert, and the mobile UI is honest about this (shows them as "Coming soon").
  Default rules auto-seeded when an event is created.
- **media**: AES-256-GCM-encrypted local photo storage, upload, gallery, per-photo image
  streaming (auth-gated).
- **delivery**: per-recipient Delivery records, My Moments feed.
- **photoprocessing**: `PhotoProcessingOrchestrator` — the one class allowed to depend on both
  the face and rules facades (enforced by an ArchUnit test); wires detect→match→rules→deliver.

### Mobile — `mobile/app/` (routes) + `mobile/src/` (everything else)

Screens: login, register, events list, create event, join-by-code (works pre-login too, shows
event preview), event dashboard (tabs: My Moments / Gallery / Members / Rules / Invite), face
enrollment (camera or library, multi-photo), photo upload (camera or library), privacy (delete
face data, leave event).

Not built on mobile: QR code *image* rendering for invites (share sheet with the raw code works;
no `react-native-qrcode-svg`-style visual code — scope cut for time, see §7), push notifications,
offline queue UI.

## 6. Known limitations / deliberate scope cuts (read before assuming something's a bug)

- **No background camera-roll monitoring.** The product doc describes automatically detecting new
  photos in the background — this is a genuinely hard native engineering problem (the doc itself
  lists it as a key technical challenge) and was explicitly scoped OUT for this MVP. The mobile
  app uses manual "take/choose a photo" instead, same as the web version would have.
- **Rule toggles**: only `GROUP_PHOTO` and `PRIVATE_PHOTO` actually do anything when toggled.
  `DEFAULT_SELF` is always effectively on regardless of its toggle (the DRL doesn't gate it on the
  config flag — a known gap, not a mobile bug if you notice it).
- **No QR code image** in the Invite tab, just a shareable text code — see above.
- **Phase 5 (privacy/retry) wasn't built.** What privacy controls exist (delete face data, leave
  event) came from earlier phases, not a dedicated Phase 5 pass. No offline sync queue, no
  granular per-category opt-outs beyond "leave the whole event."

## 7. What's left to do, roughly in priority order

1. **Test the mobile app on a real device via Expo Go** — this has not happened yet. Start here.
   Expect to find real bugs; nothing beyond `tsc --noEmit` + a successful Metro bundle has been
   verified for the mobile app.
2. Fix whatever device testing turns up.
3. Decide whether to build Phase 5 (retry/offline queue, granular privacy/opt-out) or treat
   current privacy controls as sufficient for MVP.
4. Optional polish: QR code image for invites, push notifications, thumbnail generation (photos
   currently serve full-size images), pull-to-refresh polish, loading/error state pass.
5. If ever moving toward production: real secrets (`app.jwt.secret` / `app.media.master-key-base64`
   in `application.yml` are dev-only placeholders), TLS, revisit the ONNX model licensing note in
   `backend/README.md` before any commercial use.

## 8. Gotchas hit during the build (useful if something looks newly broken)

These were all real bugs found and fixed during development — listed here so they're not
re-debugged from scratch if they resurface after a dependency bump or environment change:

- **Spring Boot 4 modularized autoconfiguration.** Raw `flyway-core` on the classpath is *not*
  enough anymore — needed the actual `spring-boot-starter-flyway` artifact, or Flyway silently
  never runs (no error, just no migrations).
- **Jackson dependency scope matters.** A `test`-scoped explicit Jackson dependency shadowed the
  `runtime`-scoped one `jjwt-jackson` needs at actual app runtime (Maven nearest-wins mediation) —
  broke JWT generation with no obvious error until traced to `ClassNotFoundException`. Fixed by
  using default (`compile`) scope instead.
- **ArcFace preprocessing**: the "standard" `(pixel-127.5)/128` normalization is WRONG for the
  `arcfaceresnet100-8.onnx` model used here — empirically this model wants raw, unnormalized 0–255
  pixel values. Verified against real photos (see `FaceRecognitionPipelineTest`). If anyone
  "fixes" this back to the textbook normalization, matching will silently break (same/different
  identity scores both land in a useless ~0.94–0.99 band).
- **Drools**: needs both `drools-engine` AND `drools-mvel` (the latter is nominally deprecated but
  still required for runtime `.drl` text compilation via `KieFileSystem`/`KieBuilder`).
- **Testcontainers + Spring context caching**: a container shared across test classes via a base
  class must use the "singleton container" pattern (`static { CONTAINER.start(); }`, no
  `@Container`/`@Testcontainers` lifecycle annotations) — otherwise the per-class stop/restart
  cycle desyncs from Spring's ApplicationContext cache and every test class after the first hangs
  on dead DB connections until a 30s HikariCP timeout.
- **macOS Java Dock icon**: the face pipeline's `BufferedImage`/`Graphics2D` usage triggers AWT's
  GUI registration on macOS unless `java.awt.headless=true` is set explicitly in `main()`.
- **npm peer dependency conflicts in the Expo project**: `expo-router`'s own transitive web
  dependencies (`@expo/ui`, `radix-ui`, `vaul`) conflict under npm's strict resolver. Fixed with
  `legacy-peer-deps=true` in `mobile/.npmrc` — standard practice in the RN/Expo ecosystem, not a
  sign of a broken install.
- **`babel-preset-expo` wasn't hoisted** to the mobile project's top-level `node_modules` by
  default (nested under `expo/node_modules/` instead), which would have broken `babel.config.js`
  resolution. Fixed by adding it as an explicit devDependency.
