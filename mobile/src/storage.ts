import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import * as Crypto from 'expo-crypto';
import type { WorkState, WorkspaceSnapshot } from './types';

const TOKEN = 'weft.googleIdToken';
const DEVICE = 'weft.deviceId';
const SNAPSHOT = 'weft.workspaceSnapshot';
const WORK_STATE = 'weft.workState';

export const storage = {
  getToken: () => SecureStore.getItemAsync(TOKEN),
  setToken: (value: string) => SecureStore.setItemAsync(TOKEN, value),
  clearToken: () => SecureStore.deleteItemAsync(TOKEN),
  async deviceId() { let id = await AsyncStorage.getItem(DEVICE); if (!id) { id = Crypto.randomUUID(); await AsyncStorage.setItem(DEVICE, id); } return id; },
  async snapshot(): Promise<WorkspaceSnapshot | null> { const raw = await AsyncStorage.getItem(SNAPSHOT); return raw ? JSON.parse(raw) : null; },
  saveSnapshot: (value: WorkspaceSnapshot) => AsyncStorage.setItem(SNAPSHOT, JSON.stringify(value)),
  async workState(): Promise<WorkState | null> { const raw = await AsyncStorage.getItem(WORK_STATE); return raw ? JSON.parse(raw) : null; },
  saveWorkState: (value: WorkState) => AsyncStorage.setItem(WORK_STATE, JSON.stringify(value)),
  clearAll: () => Promise.all([SecureStore.deleteItemAsync(TOKEN), AsyncStorage.multiRemove([SNAPSHOT, WORK_STATE])]),
};
