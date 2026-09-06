# WEFT

**One Goal. Every Device. One Continuous Workflow.**

Cross-device work execution that remembers where you stopped.

WEFT keeps a single **work state** — your goal, its workflow, the current step,
the context around it, and your progress — connected across every surface you
work on. Pause on one device, resume on another at the exact step you left.

---

## The problem

- Constant interruptions break the flow of work.
- Lost context forces you to reconstruct what you were doing.
- Re-seating takes time — finding the task, files, tabs, and next step again.

**The focus cost:**

| | |
|---|---|
| **68%** | of people lack uninterrupted focus |
| **every 2 min** | a digital interruption arrives |
| **92%** | say losing focus hurts their productivity |

---

## The model

```
Goal ──> Workflow ──> Work ──> Context ──> State ──> Resume ──┐
  ▲                                                            │
  └────────────────────────────────────────────────────────────┘
```

Turn an intention into work that can be paused and resumed. Every step writes
back to the shared WEFT work state, so the loop closes wherever you pick it up
next.

---

## One workflow, three connected surfaces

| Surface | Role | Where it lives |
|---|---|---|
| **Android** | Capture · Control · Resume | [`mobile/`](mobile/) — this repo |
| **Laptop** | Execute · Work | the existing Vibe2Ship/WEFT website (source of truth, unchanged) |
| **Browser** | Context · Evidence | Chrome extension + [`integration/browser/`](integration/browser/) |

All three read and write the same work state through the WEFT backend:

```
        Android  ─┐
        Website  ─┼──►  WEFT backend  ──►  tasks · goals · workflows · sessions · context · progress
        Browser  ─┘
```

---

## This repository

WEFT is built on an existing FastAPI + Firestore backend and website. This repo
adds the mobile surface and the cross-device integration layer around it — the
website is the source of truth and is **not** modified in place.

| Path | What it is |
|---|---|
| [`mobile/`](mobile/) | **Primary client.** Expo + React Native + TypeScript Android app (package `com.weft.mobile`). Capture tasks, run focused work sessions, block distracting apps via the Focus Bridge module, resume at the exact step. See [`mobile/README.md`](mobile/README.md). |
| [`integration/backend/`](integration/backend/) | FastAPI resource module (`weft_sync.py`, based on Vibe2Ship commit `43ca62c`) that adds durable Resume state, devices, heartbeat, activity, browser context, the mobile OAuth audience, and FCM test push — reusing the website's existing references and Focus-preference routes. See [`integration/backend/INSTALL.md`](integration/backend/INSTALL.md). |
| [`integration/browser/`](integration/browser/) | Privacy-limited active-work context sync for the Chrome extension (`weft-context-sync.ts`): registers the browser as a device, sends a heartbeat, and reports only the active tab's title/URL while work state is ACTIVE. See [`integration/browser/INSTALL.md`](integration/browser/INSTALL.md). |
| [`android/`](android/) | Earlier Kotlin/Compose prototype, retained as a reference. **Not the primary client.** |

### System layers

| Layer | Responsibility |
|---|---|
| **AI** | Understand goals, generate structured workflows and next steps |
| **WEFT state** | Tasks + goals + workflows + sessions + context + progress — the single source of truth, owned by the backend |
| **Devices** | Android + website + browser surfaces, each syncing to WEFT state |

---

## Quick start (mobile)

```bash
cd mobile
pnpm install                     # Node 20.19+
pnpm start                       # Expo Go, for UI development

# For notifications + Focus Bridge (native modules):
EXPO_NO_TELEMETRY=1 ./node_modules/.bin/expo prebuild --clean
pnpm android                     # to a connected device / emulator
pnpm typecheck
```

Local configuration — Android OAuth client (package `com.weft.mobile` + debug
SHA-1) and `mobile/google-services.json` for the Firebase project
`task-weave-93efd` — is described in [`mobile/README.md`](mobile/README.md). The
backend URL and web OAuth audience live in `mobile/app.json`.

### Integration levels

- **Existing backend only:** auth, tasks, goals, workflows, AI generation,
  scheduling, focus sessions, and reminders all work immediately.
- **With `integration/backend/weft_sync.py` installed:** exact Resume state,
  activity, devices, heartbeat, references, and browser context become
  cross-device resources.
- Focus Bridge never silently changes Do Not Disturb — the user grants Android
  notification-policy access first.
