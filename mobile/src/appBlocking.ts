import type { InstalledApp } from './focusBridge';

/**
 * JS-side mirror of the native NEVER_BLOCK denylist (WeftBlockAccessibilityService.NEVER_BLOCK),
 * applied when rendering the "choose apps to block" picker — so the user can never even select
 * Settings, System UI, or phone/emergency apps in the first place. The native service enforces the
 * same exclusion independently at block-time; this is defense in depth, not the only safeguard.
 */
export const NEVER_BLOCK = new Set([
  'com.android.systemui',
  'com.android.settings',
  'com.android.phone',
  'com.android.server.telecom',
  'com.android.dialer',
  'com.google.android.dialer',
  'com.google.android.permissioncontroller',
  'com.android.permissioncontroller',
  'com.android.emergency',
]);

/** Apps the user is actually allowed to choose from — WEFT itself and safety-critical system apps are never selectable. */
export function selectableApps(apps: InstalledApp[], ownPackage: string): InstalledApp[] {
  return apps.filter(app => app.packageName !== ownPackage && !NEVER_BLOCK.has(app.packageName));
}

export function isAppSelected(blocked: InstalledApp[], packageName: string): boolean {
  return blocked.some(app => app.packageName === packageName);
}

export function toggleBlockedApp(blocked: InstalledApp[], app: InstalledApp): InstalledApp[] {
  return isAppSelected(blocked, app.packageName) ? blocked.filter(a => a.packageName !== app.packageName) : [...blocked, app];
}

/** Blocking must activate only when the user has turned it on AND the Accessibility permission is actually granted. */
export function shouldActivateBlocking(blockingPref: boolean, hasAccessibilityAccess: boolean): boolean {
  return blockingPref && hasAccessibilityAccess;
}

/** Same app-restart-survival inference used for DND Focus: only claim responsibility for turning blocking back off if this task's session is really ACTIVE and the preference is still on. */
export function inferBlockingEnabledOnMount(trackingThisTask: boolean, workStateStatus: 'ACTIVE' | 'PAUSED' | 'IDLE' | undefined, blockingPref: boolean): boolean {
  return trackingThisTask && workStateStatus === 'ACTIVE' && blockingPref;
}
