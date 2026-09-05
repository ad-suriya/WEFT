import { requireOptionalNativeModule } from 'expo-modules-core';
import { Platform } from 'react-native';

const native = requireOptionalNativeModule<{ hasPolicyAccess(): Promise<boolean>; requestPolicyAccess(): Promise<boolean>; setFocusEnabled(enabled: boolean): Promise<boolean> }>('WeftFocusBridge');
export const FocusBridge = {
  isAvailable: async () => Platform.OS === 'android' && Boolean(native),
  hasAccess: async () => native ? Boolean(await native.hasPolicyAccess()) : false,
  requestAccess: async () => native ? native.requestPolicyAccess() : false,
  setEnabled: async (enabled: boolean) => native ? native.setFocusEnabled(enabled) : false,
};
