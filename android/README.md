# Task Weave — Android

A native Android client for **Task Weave**. It is a *thin client*: every
task / AI / scheduling decision is made by the existing FastAPI backend and
Firestore. This app calls the same REST API the web dashboard and Chrome
extension use, so a user signed in with the same Google account sees identical
data everywhere — and it adds the mobile-only pieces (push, share-sheet capture,
voice capture, and later on-device focus blocking + a Glance widget).

- **Base URL:** `https://task-weave-backend-57923630274.asia-south1.run.app`
- **Auth:** `Authorization: Bearer <Google ID token>`; the backend verifies the
  token server-side with audience = the existing **Web** OAuth client id.

---

## Status — Phase 1 (this scaffold)

| # | MVP item | State |
|---|----------|-------|
| 1 | Google sign-in → ID token → authenticated API calls | ✅ implemented |
| 2 | Task list: view / add / edit / complete / delete, pull-to-refresh, optimistic updates, offline cache + replay queue | ✅ implemented |
| 3 | AI chat screen → `/api/chat`, quick-reply chips, agentic starter-content card with copy, voice dictation (`SpeechRecognizer`) | ✅ implemented |
| 4 | Today view: tasks scheduled today, "next micro-step" surfaced, button to run `/api/schedule` | ✅ implemented |
| 5 | Sign-in requests the Google Calendar scope so server-side sync keeps working | ✅ consent flow wired (see *Calendar scope* below) |

Phase 2 hooks that are already stubbed so they compile and can be finished
without restructuring: FCM messaging service + `/api/devices` registration,
`ACTION_SEND` share-sheet capture, `taskweave://task/{id}` notification deep
links, `POST_NOTIFICATIONS` runtime permission surface, periodic `/api/status`
sync via WorkManager. Focus mode / widget / QS tile are **not** started.

---

## Architecture

MVVM + unidirectional data flow. Room is a **cache only** — Firestore via the API
is the source of truth (see *Non-goals* in the brief).

```
Compose UI  ──>  ViewModel  ──>  Repository  ──┬──>  Retrofit (TaskWeaveApi) ── OkHttp ── backend
   ▲                                           └──>  Room (TaskDao, PendingOpDao)
   └────────────  StateFlow  ◀───────────────────────  Room Flow (single source of truth for the UI)

Writes: optimistic upsert to Room → push to API → on network failure, enqueue in
`pending_ops` and let PendingOpWorker replay on reconnect.
```

Key packages under `app/src/main/java/com/taskweave/android/`:

| Package | What's in it |
|---|---|
| `auth/` | `TokenStore` (EncryptedSharedPreferences), `AuthManager` (Credential Manager + Sign in with Google, silent refresh), `AuthInterceptor` + `TokenAuthenticator` (adds bearer, refreshes on 401), `CalendarAuthorizer` |
| `data/remote/` | `TaskWeaveApi` (Retrofit), `dto/` (kotlinx.serialization) |
| `data/local/` | `TaskWeaveDatabase`, `TaskEntity` + `PendingOpEntity`, DAOs |
| `data/repository/` | `TaskRepository` (offline-first), `ChatRepository`, `ScheduleRepository`, `StatusRepository`, `DeviceRepository`, `Mappers` |
| `sync/` | `SyncScheduler`, `StatusSyncWorker` (periodic `/api/status`), `PendingOpWorker` (replay queue) |
| `fcm/` | `TaskWeaveMessagingService`, `FcmRegistrar` |
| `notifications/` | notification builder + deep-link intent, `StatusNotifier` |
| `ui/` | `theme/`, `navigation/`, `signin/`, `tasks/`, `chat/`, `today/`, `components/`, plus `RootViewModel` + `TaskWeaveRoot` |
| `di/` | `NetworkModule`, `DatabaseModule` (Hilt) |

**Stack:** Kotlin 2.0, Jetpack Compose + Material 3 (Material You dynamic color),
Hilt, Retrofit/OkHttp + kotlinx.serialization, Room, WorkManager, Credential
Manager, Firebase Cloud Messaging. `minSdk 26`, `compileSdk`/`targetSdk 35`.

---

## Prerequisites

- Android Studio Ladybug or newer (JDK 17 bundled).
- The **wrapper jar is not committed** (binary). Android Studio downloads it on
  first sync; from the CLI run once: `gradle wrapper --gradle-version 8.11.1`.

---

## Configuration

Nothing secret is committed. Three things must be provided locally.

### 1. `android/local.properties` (git-ignored)

```properties
sdk.dir=/Users/you/Library/Android/sdk

# Backend
taskweave.baseUrl=https://task-weave-backend-57923630274.asia-south1.run.app/

# OAuth: the backend verifies the ID token against the existing *Web* client id,
# so the Android app must send THAT as the server client id (not an Android id).
taskweave.serverClientId=611514829994-jeeb1eg42ghcrmicruek2nbhmmf9fgo5.apps.googleusercontent.com
```

These are read in `app/build.gradle.kts` and emitted as `BuildConfig` fields.
The values above are also the built-in defaults, so the app compiles and runs
without this file — but keep real values here rather than editing Gradle.

### 2. Google OAuth clients (Google Cloud console → *APIs & Services → Credentials*, project `task-weave-93efd`)

Sign in with Google on Android needs **two** client IDs that already exist for
this project, plus one new Android client:

| Client | Used for | Notes |
|---|---|---|
| **Web** `611514829994-jeeb1eg42…apps.googleusercontent.com` | `serverClientId` passed to Credential Manager; audience the backend verifies | already exists — do not change |
| **Android** (new) | lets Google Play Services vouch for this app's signing identity | create one per signing cert |

Create the Android client:

1. *Create credentials → OAuth client ID → Android*.
2. Package name: `com.taskweave.android` (debug builds use
   `com.taskweave.android.debug` — add a second Android client for it, or add the
   debug SHA-1 to the same one; Google allows multiple certs per package only via
   multiple clients, so typically: one client for the debug keystore, one for
   release).
3. SHA-1 of the signing certificate:

   ```bash
   # debug keystore (created automatically by Android Studio)
   keytool -list -v -alias androiddebugkey -storepass android -keypass android \
     -keystore ~/.android/debug.keystore | grep 'SHA1:'

   # release keystore
   keytool -list -v -alias <your-alias> -keystore <your-release.jks> | grep 'SHA1:'
   ```

No client secret or JSON is needed for the Android client — Play Services matches
package + SHA-1 at runtime. The **only** id the code references is the Web one.

> If sign-in returns `NoCredentialException` / "no credentials available":
> the device has no Google account, or the SHA-1 / package don't match a
> configured Android client, or Play Services is out of date.

### 3. `app/google-services.json` (git-ignored) — for FCM

1. Firebase console → project **`task-weave-93efd`** (project number `57923630274`,
   the same google-services project the backend's `firebase-admin` uses).
2. *Add app → Android*: package name `com.taskweave.android` (add
   `com.taskweave.android.debug` as a second Android app in the same Firebase
   project so debug builds get a token too).
3. Download `google-services.json` into `android/app/`.

The `com.google.gms.google-services` Gradle plugin is applied **only when that
file is present** (see the bottom of `app/build.gradle.kts`), so `assembleDebug`
stays green on CI before Firebase is wired. Without the file the app still runs;
push is simply inert.

**Device registration contract** (already implemented client-side):
- on sign-in and on FCM token refresh → `POST /api/devices { token }`
- on sign-out → `DELETE /api/devices/{token}` then delete the local token
- incoming `data.type ∈ {DEADLINE, FOCUS_START, CUSTOM, …}`; `task_id`
  deep-links via `taskweave://task/{id}`; `reminder_id` → call
  `POST /api/reminders/{id}/ack` when the user opens/dismisses.

### Calendar scope

Credential Manager's ID-token flow does **not** grant API scopes. `CalendarAuthorizer`
uses the Google **Authorization API** (`Identity.getAuthorizationClient`) to
request `https://www.googleapis.com/auth/calendar` with **offline access**
(a server auth code) right after the first sign-in. To finish server-side sync,
send `AuthorizationResult.getServerAuthCode()` to the backend endpoint that
exchanges it for a refresh token (wire it in `RootViewModel.onSignedIn()` — one
call). Until then the consent screen still appears so the grant is recorded on
the account.

---

## Build & run

```bash
cd android
gradle wrapper --gradle-version 8.11.1   # first time only (creates gradlew + jar)
./gradlew assembleDebug                  # APK at app/build/outputs/apk/debug/
./gradlew installDebug                   # to a connected device/emulator
./gradlew testDebugUnitTest lint
```

Or just open `android/` in Android Studio and Run.

---

## CI

`.github/workflows/android-ci.yml` runs on every PR and on `main`: sets up JDK 17
+ Gradle 8.11.1, generates the wrapper, then
`./gradlew assembleDebug testDebugUnitTest lint` and uploads `app-debug.apk`.

The workflow assumes **`android/` is the repository root**. If you nest it in a
monorepo, move the workflow to the repo-root `.github/workflows/` and add
`paths:`/`working-directory:` filters for `android/`.

---

## Backend additions required for push (not in this repo)

Phase 2 item 7 needs a small backend change — a `devices` Firestore collection
and `POST/DELETE /api/devices`, a `push.py` that fans out due reminders via
`firebase-admin` `messaging.send_each_for_multicast`, a one-line piggyback in the
existing `/api/status` handler, and a Cloud Scheduler job hitting a protected
`/api/cron/push` every 5 minutes. The full diff is specified in the project
brief; reuse the existing `task-weave-93efd` firebase-admin credentials.

---

## Non-goals (kept intentionally)

- No AI/scheduling logic in the app — the backend owns it.
- No separate database — Room is a read cache + write queue only.
