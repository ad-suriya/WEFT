# WEFT

This workspace contains the Android clients and the cross-device integration layer for the existing Vibe2Ship/WEFT website.

- [`mobile/`](mobile/) — primary React Native + Expo + TypeScript app.
- [`integration/backend/`](integration/backend/) — durable Resume, devices, heartbeat, activity, browser-context, mobile OAuth-audience, and FCM test-push API extension. It deliberately reuses the latest website's existing references and Focus-preference routes.
- [`integration/browser/`](integration/browser/) — privacy-limited active-work context sync for the Chrome extension.
- [`android/`](android/) — earlier Kotlin/Compose prototype retained as a reference; it is not the primary client.

Start with [`mobile/README.md`](mobile/README.md). The website remains the source of truth and has not been modified in place.
