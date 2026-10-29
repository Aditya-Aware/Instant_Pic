# Shared Memory Photo Network

Temporary, event-scoped photo network: create an event, invite friends, everyone enrolls a
face profile, everyone takes photos normally, and the app automatically detects who's in each
photo and delivers it to the right people based on configurable sharing rules.

See the full product concept in the source doc this was built from
(`Shared_Memory_Photo_Network_Product_Documentation.docx`).

**For full current status, what's built, what's left, and known gotchas, see [`PROGRESS.md`](./PROGRESS.md).**

## Stack

- **Backend**: Java 21 + Spring Boot 4.1.x, Postgres 16 (Flyway migrations), Spring Security (JWT),
  Drools (sharing rules engine), ONNX Runtime Java (face detection/embedding, no cloud API).
- **Mobile**: React Native (Expo SDK 57), `expo-router`, TanStack Query, `expo-image-picker`.

## Architecture principle

Face matching (`com.sharedmemory.face`) never decides *who gets a photo* — it only outputs
detected participant matches (`face.api.PhotoMatchResult`). A separate Drools-based rules engine
(`com.sharedmemory.rules`) consumes those matches and decides delivery + gallery visibility
(`rules.api.SharingDecision` / `VisibilityDecision`). Only
`com.sharedmemory.photoprocessing.PhotoProcessingOrchestrator` is allowed to depend on both
`face.api` and `rules.api` — enforced by an ArchUnit test
(`backend/src/test/java/com/sharedmemory/architecture/ArchitectureTest.java`).

## Local setup

### Prerequisites
- JDK 21, Maven 3.9+
- Docker (for Postgres)
- Node 20+ / npm (for the frontend)

### Backend

```bash
docker compose up -d          # starts Postgres on :5432
cd backend
./mvnw spring-boot:run        # boots the app on :8080, Flyway migrates on startup
curl localhost:8080/actuator/health
```

### Face ML models

Required for face detection/matching to work. Not committed to git (`backend/models/`,
gitignored — binaries, tens of MB — **already downloaded** in this checkout). Download manually
if setting up fresh elsewhere:

1. **Embedding**: `arcfaceresnet100-8.onnx` from the ONNX Model Zoo
   (`github.com/onnx/models`, `vision/body_analysis/arcface`) — Apache-2.0, commercially usable.
   Do **not** substitute InsightFace's `buffalo_l`/`w600k_r50` weights — those pretrained weights
   require a separate commercial license from InsightFace despite the InsightFace code being MIT.
2. **Detection**: "Ultra-Light-Fast-Generic-Face-Detector-1MB" (`version-RFB-320.onnx`) — MIT.

Place both under `backend/models/` and point `app.face.detector-model-path` /
`app.face.embedding-model-path` in `application.yml` (or the matching env vars) at them.

### Mobile

```bash
cd mobile
npm install
npx expo start                 # scan the QR code with the Expo Go app on your phone
```

Requires the backend running (above) and both devices on the same Wi-Fi network — a phone can't
reach `localhost` on the dev machine, so `mobile/src/api/config.ts` hardcodes this machine's LAN
IP. Update it if that IP changes (`ipconfig getifaddr en0` on macOS).

## Status

See [`PROGRESS.md`](./PROGRESS.md) for the full breakdown. Short version: backend is fully built
and tested (Phases 1–4); the mobile app is fully written but not yet tested on a real device.
# Instant_Pic
