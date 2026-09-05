import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider } from '@/auth';
import { WorkspaceProvider } from '@/workspace';
import { useNotificationRouting } from '@/notifications';
import { colors } from '@/theme';

function Navigation() { useNotificationRouting(); return <WorkspaceProvider><StatusBar style="dark" /><Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.paper } }} /></WorkspaceProvider>; }
export default function RootLayout() {
  return <AuthProvider><Navigation /></AuthProvider>;
}
