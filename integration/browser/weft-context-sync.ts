/**
 * Background-service integration for the WEFT Chrome extension.
 *
 * Call `startWorkContextSync()` once from the extension background worker.
 * Context is collected only while the authenticated account has ACTIVE work,
 * and is limited to the active tab's title + sanitized URL. Save Reference is
 * exposed as a separate explicit action.
 */
import { request } from '../../packages/storage/lib/impl/backend-client.js';

type WorkState = { status: 'IDLE' | 'ACTIVE' | 'PAUSED'; task_id: number | null; current_step: string; next_action: string };
type PageContext = { title: string; url: string; relevant: boolean; captured_at: string; device_id: string };

const DEVICE_KEY = 'weft-browser-device-id';
const SENSITIVE_QUERY = /token|password|secret|auth|session|code|key/i;
const words = (value: string) => new Set(value.toLowerCase().match(/[a-z0-9]{3,}/g) || []);
function safeUrl(value: string): string {
  const parsed = new URL(value);
  parsed.username = '';
  parsed.password = '';
  parsed.hash = '';
  [...parsed.searchParams.keys()].filter(key => SENSITIVE_QUERY.test(key)).forEach(key => parsed.searchParams.delete(key));
  return parsed.toString();
}
export function pageRelevance(title: string, url: string, state: WorkState): boolean {
  const work = words(`${state.current_step} ${state.next_action}`);
  const page = words(`${title} ${new URL(url).hostname}`);
  return [...work].some(word => page.has(word));
}
async function deviceId(): Promise<string> {
  const saved = await chrome.storage.local.get(DEVICE_KEY);
  if (saved[DEVICE_KEY]) return saved[DEVICE_KEY];
  const id = `browser-${crypto.randomUUID()}`;
  await chrome.storage.local.set({ [DEVICE_KEY]: id });
  return id;
}
async function activeMetadata(): Promise<{ title: string; url: string } | null> {
  const [tab] = await chrome.tabs.query({ active: true, lastFocusedWindow: true });
  if (!tab?.url || !/^https?:/.test(tab.url)) return null;
  return { title: (tab.title || new URL(tab.url).hostname).slice(0, 300), url: safeUrl(tab.url) };
}
async function syncOnce(): Promise<void> {
  const state = await request<WorkState | null>('/work-state');
  if (!state || state.status !== 'ACTIVE') return;
  const page = await activeMetadata(); if (!page) return;
  const context: PageContext = { ...page, relevant: pageRelevance(page.title, page.url, state), captured_at: new Date().toISOString(), device_id: await deviceId() };
  await request('/browser-context', { method: 'PUT', body: JSON.stringify(context) });
}
async function registerBrowser(): Promise<void> {
  const device_id = await deviceId();
  await request('/devices', {
    method: 'POST',
    body: JSON.stringify({
      device_id,
      name: `${chrome.runtime.getManifest().name} browser extension`,
      device_type: 'browser',
      capabilities: ['page_context', 'save_reference', 'context_switch_detection'],
    }),
  });
}
export function startWorkContextSync(): void {
  void registerBrowser().catch(() => undefined);
  chrome.alarms.create('weft-work-context', { periodInMinutes: 0.5 });
  chrome.alarms.create('weft-device-heartbeat', { periodInMinutes: 5 });
  chrome.alarms.onAlarm.addListener(alarm => {
    if (alarm.name === 'weft-work-context') void syncOnce().catch(() => undefined);
    if (alarm.name === 'weft-device-heartbeat') void deviceId().then(id => request(`/devices/${encodeURIComponent(id)}/heartbeat`, { method: 'POST' })).catch(() => registerBrowser());
  });
}
export async function saveCurrentReference(taskId?: number): Promise<void> {
  const page = await activeMetadata(); if (!page) return;
  await request('/references', { method: 'POST', body: JSON.stringify({ title: page.title, url: page.url, task_id: taskId }) });
}
