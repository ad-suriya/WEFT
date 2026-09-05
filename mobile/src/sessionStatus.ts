import { deviceConnection } from './todayView';
import type { BrowserContext, DeviceRecord } from './types';

export type ConnectionState = 'connected' | 'disconnected' | 'unavailable';
export interface ConnectionStatusView { state: ConnectionState; label: string; }

const BROWSER_FRESH_WINDOW_MS = 2 * 60 * 1000;

/** The most recently-seen non-mobile device, standing in for "the laptop" — this app and the browser extension are the only registered device types, so anything that isn't this phone counts. */
export function findLaptopDevice(devices: DeviceRecord[]): DeviceRecord | null {
  const candidates = devices.filter(d => d.device_type !== 'android' && d.device_type !== 'ios');
  if (!candidates.length) return null;
  return [...candidates].sort((a, b) => b.last_seen.localeCompare(a.last_seen))[0];
}

/** Real, backend-reported laptop/desktop connectivity — never fabricated. No registered device at all reads as "unavailable", not "disconnected". */
export function laptopStatus(devices: DeviceRecord[], now: number = Date.now()): ConnectionStatusView {
  const device = findLaptopDevice(devices);
  if (!device) return { state: 'unavailable', label: 'Unavailable' };
  return deviceConnection(device, now).online ? { state: 'connected', label: 'Connected' } : { state: 'disconnected', label: 'Disconnected' };
}

/** Whether the browser extension is actively pushing page context for the current work session (it only does so while ACTIVE/PAUSED). No context ever received reads as "unavailable"; context that's gone stale reads as "disconnected". */
export function browserStatus(browserContext: BrowserContext | null | undefined, now: number = Date.now()): ConnectionStatusView {
  if (!browserContext) return { state: 'unavailable', label: 'Unavailable' };
  const age = now - new Date(browserContext.captured_at).getTime();
  return age < BROWSER_FRESH_WINDOW_MS ? { state: 'connected', label: 'Connected' } : { state: 'disconnected', label: 'Disconnected' };
}

export type FocusState = 'checking' | 'unavailable' | 'access_needed' | 'off' | 'on';
export interface FocusStatusView { state: FocusState; label: string; }
export interface FocusStatusInput { isAvailable: boolean | null; hasAccess: boolean | null; enabled: boolean | null; }

/** Focus Bridge (Android DND silencing) status, built only from what the native module actually reports — never assumed on. */
export function deriveFocusStatus({ isAvailable, hasAccess, enabled }: FocusStatusInput): FocusStatusView {
  if (isAvailable === null || hasAccess === null) return { state: 'checking', label: 'Checking…' };
  if (!isAvailable) return { state: 'unavailable', label: 'Unavailable' };
  if (!hasAccess) return { state: 'access_needed', label: 'Access needed' };
  return enabled ? { state: 'on', label: 'On' } : { state: 'off', label: 'Off' };
}

/**
 * The two-part rule Focus activation must always satisfy: the user has turned the Study Focus
 * preference on, AND the Android notification-policy permission has actually been granted.
 * Availability of the native module itself is a further prerequisite — without it neither matters.
 */
export function shouldActivateFocus(studyFocusPref: boolean | undefined, isAvailable: boolean, hasAccess: boolean): boolean {
  return !!studyFocusPref && isAvailable && hasAccess;
}

/**
 * Whether this app instance should still consider itself responsible for turning Focus back off,
 * right after mounting — e.g. the app was closed and reopened mid-session. Inferred only from real,
 * already-synced state (this task is the one being tracked, its session is ACTIVE, and the
 * preference that would have caused us to enable it in the first place is still on) — never assumed.
 */
export function inferFocusEnabledOnMount(trackingThisTask: boolean, workStateStatus: 'ACTIVE' | 'PAUSED' | 'IDLE' | undefined, studyFocusPref: boolean | undefined): boolean {
  return trackingThisTask && workStateStatus === 'ACTIVE' && !!studyFocusPref;
}
