import { useEffect } from 'react';
import { Platform } from 'react-native';
import { router } from 'expo-router';
import * as Notifications from 'expo-notifications';
import { colors } from './theme';

Notifications.setNotificationHandler({ handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: false, shouldSetBadge: false }) });

export async function getNativePushToken(): Promise<string | null> {
  if (Platform.OS !== 'android') return null;
  try {
    await Notifications.setNotificationChannelAsync('work', { name: 'Work and reminders', importance: Notifications.AndroidImportance.HIGH, vibrationPattern: [0, 200, 100, 200], lightColor: colors.blue });
    const permission = await Notifications.getPermissionsAsync();
    if (!permission.granted) return null;
    return (await Notifications.getDevicePushTokenAsync()).data;
  } catch { return null; }
}

export function useNotificationRouting() {
  useEffect(() => {
    const open = (response: Notifications.NotificationResponse | null) => {
      const taskId = response?.notification.request.content.data?.task_id;
      if (taskId != null) router.push({ pathname: '/focus', params: { taskId: String(taskId) } });
    };
    Notifications.getLastNotificationResponseAsync().then(open);
    const subscription = Notifications.addNotificationResponseReceivedListener(open);
    return () => subscription.remove();
  }, []);
}
