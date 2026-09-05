# WEFT Mobile

Expo + React Native + TypeScript Android client for the existing WEFT backend.

## Run

1. Install Node 20.19+ and run `pnpm install` in this directory.
2. Android OAuth is configured for package `com.weft.mobile` and client
   `611514829994-fuh73027tm7ru5npv3a84mfq3afkm345.apps.googleusercontent.com`.
   The current debug signing SHA-1 is
   `5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:F6:25`;
   that exact package/fingerprint pair must be present on the Android OAuth
   client in Google Cloud.
3. Put the real Firebase Android file at
   `WEFT/mobile/google-services.json`. It must be the Android app config for
   Firebase project `task-weave-93efd` and package `com.weft.mobile`, not a
   Firebase Admin/service-account key. The file is gitignored and
   `app.config.js` enables it automatically when present.
4. For Expo Go UI development, run `pnpm start`.
5. For notifications and Focus Bridge, run
   `EXPO_NO_TELEMETRY=1 ./node_modules/.bin/expo prebuild --clean`, then
   `pnpm android`.

The backend URL and existing web OAuth audience are in `app.json`. The app sends the Google ID token as the same Bearer credential used by the website and polls shared state while foregrounded. Local encrypted/cache storage only provides launch and offline continuity; the backend remains the source of truth.

## Integration levels

- Existing backend: authentication, tasks, goals, workflows, AI generation, scheduling, focus sessions and reminders work immediately.
- With `../integration/backend/weft_sync.py` installed in the website backend: exact Resume state, activity, devices, heartbeat, references and browser context become cross-device resources.
- Focus Bridge never silently changes DND. The user first grants Android notification-policy access from Settings.
