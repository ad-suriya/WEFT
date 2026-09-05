import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme';

const icon = (name: keyof typeof Ionicons.glyphMap) => ({ color, size }: { color: any; size: number }) => <Ionicons name={name} size={size} color={color} />;
export default function TabsLayout() { return <Tabs screenOptions={{ headerShown: false, tabBarActiveTintColor: colors.blue, tabBarInactiveTintColor: colors.muted, tabBarStyle: { backgroundColor: colors.card, borderTopColor: colors.ink, borderTopWidth: 1.5, height: 68, paddingTop: 7, paddingBottom: 8 }, tabBarLabelStyle: { fontSize: 10, fontWeight: '800', textTransform: 'uppercase' } }}><Tabs.Screen name="today" options={{ title: 'Today', tabBarIcon: icon('sunny-outline') }} /><Tabs.Screen name="work" options={{ title: 'My Work', tabBarIcon: icon('checkbox-outline') }} /><Tabs.Screen name="create" options={{ title: 'Create', tabBarIcon: icon('add-circle-outline') }} /><Tabs.Screen name="history" options={{ title: 'Activity', tabBarIcon: icon('pulse-outline') }} /><Tabs.Screen name="settings" options={{ title: 'Settings', tabBarIcon: icon('options-outline') }} /></Tabs> }
