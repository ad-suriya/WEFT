import { requireOptionalNativeModule } from 'expo-modules-core';
import { Platform } from 'react-native';

export interface InstalledApp { packageName: string; appName: string; }

const native = requireOptionalNativeModule<{
  hasPolicyAccess(): Promise<boolean>;
  requestPolicyAccess(): Promise<boolean>;
  setFocusEnabled(enabled: boolean): Promise<boolean>;
  hasAccessibilityAccess(): Promise<boolean>;
  requestAccessibilityAccess(): Promise<boolean>;
  listInstalledApps(): Promise<InstalledApp[]>;
  setBlockedPackages(packages: string[]): Promise<boolean>;
  setBlockingEnabled(enabled: boolean): Promise<boolean>;
}>('WeftFocusBridge');

export const FocusBridge = {
  isAvailable: async () => Platform.OS === 'android' && Boolean(native),
  hasAccess: async () => native ? Boolean(await native.hasPolicyAccess()) : false,
  requestAccess: async () => native ? native.requestPolicyAccess() : false,
  setEnabled: async (enabled: boolean) => native ? native.setFocusEnabled(enabled) : false,

  // App blocking, via the Android AccessibilityService declared in this same native module.
  hasBlockingAccess: async () => native ? Boolean(await native.hasAccessibilityAccess()) : false,
  requestBlockingAccess: async () => native ? native.requestAccessibilityAccess() : false,
  listInstalledApps: async (): Promise<InstalledApp[]> => native ? native.listInstalledApps() : [],
  setBlockedApps: async (packages: string[]) => native ? native.setBlockedPackages(packages) : false,
  setBlockingEnabled: async (enabled: boolean) => native ? native.setBlockingEnabled(enabled) : false,
};
