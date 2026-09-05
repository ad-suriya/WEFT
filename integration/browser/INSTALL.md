# Browser integration

Copy `weft-context-sync.ts` into the extension source and fix its relative `backend-client` import. Call `startWorkContextSync()` from the background service worker and call `saveCurrentReference(taskId)` only from the existing Save Reference click handler.

Add `"alarms"` to the extension manifest permissions. The module registers the browser as an account device and sends a five-minute heartbeat. Keep the existing explicit-only content capture. This module reads only the active HTTP(S) tab title and URL, and only while shared work state is ACTIVE. Its relevance flag is a local keyword-overlap heuristic; no page text is uploaded.
