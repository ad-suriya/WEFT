import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors } from '@/theme';

const icon = (name: keyof typeof Ionicons.glyphMap) => ({ color, size }: { color: any; size: number }) => <Ionicons name={name} size={size} color={color} />;
export default function TabsLayout() {
  const insets = useSafeAreaInsets();
  return <Tabs screenOptions={{ headerShown: false, tabBarActiveTintColor: colors.blue, tabBarInactiveTintColor: colors.muted, tabBarStyle: { backgroundColor: colors.card, borderTopColor: colors.ink, borderTopWidth: 1.5, height: 60 + insets.bottom, paddingTop: 7, paddingBottom: 8 + insets.bottom }, tabBarLabelStyle: { fontSize: 10, fontWeight: '800', textTransform: 'uppercase' } }}><Tabs.Screen name="today" options={{ title: 'Today', tabBarIcon: icon('sunny-outline') }} /><Tabs.Screen name="work" options={{ title: 'My Work', tabBarIcon: icon('checkbox-outline') }} /><Tabs.Screen name="create" options={{ title: '+', tabBarIcon: icon('add-circle-outline') }} /><Tabs.Screen name="devices" options={{ title: 'Devices', tabBarIcon: icon('hardware-chip-outline') }} /></Tabs>;
}
