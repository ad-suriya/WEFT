import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import * as Crypto from 'expo-crypto';
import type { InstalledApp } from './focusBridge';
import type { WorkState, WorkspaceSnapshot } from './types';

const TOKEN = 'weft.googleIdToken';
const DEVICE = 'weft.deviceId';
const SNAPSHOT = 'weft.workspaceSnapshot';
const WORK_STATE = 'weft.workState';
const CONSENT_USER = 'weft.consentUserId';
const BLOCKED_APPS = 'weft.blockedApps';
const BLOCKING_ENABLED = 'weft.blockingEnabled';

export const storage = {
  getToken: () => SecureStore.getItemAsync(TOKEN),
  setToken: (value: string) => SecureStore.setItemAsync(TOKEN, value),
  clearToken: () => SecureStore.deleteItemAsync(TOKEN),
  getConsentUserId: () => AsyncStorage.getItem(CONSENT_USER),
  setConsentUserId: (userId: string) => AsyncStorage.setItem(CONSENT_USER, userId),
  async deviceId() { let id = await AsyncStorage.getItem(DEVICE); if (!id) { id = Crypto.randomUUID(); await AsyncStorage.setItem(DEVICE, id); } return id; },
  async snapshot(): Promise<WorkspaceSnapshot | null> { const raw = await AsyncStorage.getItem(SNAPSHOT); return raw ? JSON.parse(raw) : null; },
  saveSnapshot: (value: WorkspaceSnapshot) => AsyncStorage.setItem(SNAPSHOT, JSON.stringify(value)),
  async workState(): Promise<WorkState | null> { const raw = await AsyncStorage.getItem(WORK_STATE); return raw ? JSON.parse(raw) : null; },
  saveWorkState: (value: WorkState) => AsyncStorage.setItem(WORK_STATE, JSON.stringify(value)),
  async blockedApps(): Promise<InstalledApp[]> { const raw = await AsyncStorage.getItem(BLOCKED_APPS); return raw ? JSON.parse(raw) : []; },
  saveBlockedApps: (apps: InstalledApp[]) => AsyncStorage.setItem(BLOCKED_APPS, JSON.stringify(apps)),
  async blockingEnabled(): Promise<boolean> { return (await AsyncStorage.getItem(BLOCKING_ENABLED)) === 'true'; },
  saveBlockingEnabled: (enabled: boolean) => AsyncStorage.setItem(BLOCKING_ENABLED, enabled ? 'true' : 'false'),
  clearAll: () => Promise.all([SecureStore.deleteItemAsync(TOKEN), AsyncStorage.multiRemove([SNAPSHOT, WORK_STATE, CONSENT_USER, BLOCKED_APPS, BLOCKING_ENABLED])]),
};
