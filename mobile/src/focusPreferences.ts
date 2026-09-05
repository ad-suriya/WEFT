import type { SessionStatus } from './todayView';
import type { FocusPrefs, Reminder } from './types';

/**
 * Whether a reminder of this kind should be held back right now, per the user's Focus preferences.
 * Only ever holds anything while a session is actually ACTIVE, and never holds a category the
 * user explicitly allowed through — "muted unless allowed", not the reverse.
 */
export function shouldHoldNotification(kind: string, prefs: FocusPrefs | undefined, sessionStatus: SessionStatus): boolean {
  if (sessionStatus !== 'ACTIVE') return false;
  if (!prefs?.hold_notifications) return false;
  return !prefs.allow_list.includes(kind);
}

/** The real reminder categories this account actually has — never a fabricated fixed list, so "unsupported" (none yet) renders as nothing rather than fake options. */
export function availableCategories(reminders: Reminder[]): string[] {
  return [...new Set(reminders.map(r => r.kind))].sort();
}

export function toggleCategory(allowList: string[], kind: string): string[] {
  return allowList.includes(kind) ? allowList.filter(k => k !== kind) : [...allowList, kind];
}
