import { deviceConnection } from './todayView';
import type { DeviceRecord, WorkState } from './types';

const BROWSER_CONTEXT_FRESH_WINDOW_MS = 2 * 60 * 1000;

export type DeviceCategory = 'phone' | 'laptop' | 'extension';

export interface DeviceCardView {
  category: DeviceCategory;
  label: string;
  connected: boolean;
  /** Only set when a device of this category was seen before but has since gone stale — e.g. "5m ago". Never set for a category with no device registered at all. */
  lastSeenLabel: string | null;
  /** e.g. "Working" / "Focus" / "Active" — only ever set alongside connected: true, never fabricated. */
  sessionNote: string | null;
}

const PHONE_TYPES = ['android', 'ios'];
const EXTENSION_TYPES = ['browser'];

function mostRecent(devices: DeviceRecord[]): DeviceRecord | null {
  if (!devices.length) return null;
  return [...devices].sort((a, b) => b.last_seen.localeCompare(a.last_seen))[0];
}

/**
 * This app and the browser extension are the only clients that currently register a device (see
 * `/api/devices`). Nothing in this integration registers a distinct "laptop/website" device yet, so
 * that category will honestly read Offline until a client does — never faked as Connected.
 */
function findByCategory(devices: DeviceRecord[], category: DeviceCategory): DeviceRecord | null {
  if (category === 'phone') return mostRecent(devices.filter(d => PHONE_TYPES.includes(d.device_type)));
  if (category === 'extension') return mostRecent(devices.filter(d => EXTENSION_TYPES.includes(d.device_type)));
  return mostRecent(devices.filter(d => !PHONE_TYPES.includes(d.device_type) && !EXTENSION_TYPES.includes(d.device_type)));
}

export interface BuildDeviceViewsInput {
  devices: DeviceRecord[];
  workState: WorkState | null;
  /** Whether Focus Bridge is genuinely active on THIS phone right now — computed by the caller from the real native permission state, not assumed here. */
  phoneFocusActive: boolean;
  now?: number;
}

export function buildDeviceViews({ devices, workState, phoneFocusActive, now = Date.now() }: BuildDeviceViewsInput): DeviceCardView[] {
  const sessionActive = workState?.status === 'ACTIVE';
  const browserContextFresh = sessionActive && !!workState?.browser_context && now - new Date(workState.browser_context.captured_at).getTime() < BROWSER_CONTEXT_FRESH_WINDOW_MS;

  const build = (category: DeviceCategory, label: string, sessionNoteWhenConnected: string | null): DeviceCardView => {
    const device = findByCategory(devices, category);
    if (!device) return { category, label, connected: false, lastSeenLabel: null, sessionNote: null };
    const connection = deviceConnection(device, now);
    return {
      category,
      label,
      connected: connection.online,
      lastSeenLabel: connection.online ? null : connection.label,
      sessionNote: connection.online ? sessionNoteWhenConnected : null,
    };
  };

  return [
    build('phone', 'Phone', sessionActive && phoneFocusActive ? 'Focus' : null),
    build('laptop', 'Laptop', sessionActive ? 'Working' : null),
    build('extension', 'WEFT Extension', browserContextFresh ? 'Active' : null),
  ];
}
