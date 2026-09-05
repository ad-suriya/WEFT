import { useEffect, useState } from 'react';
import { SafeAreaView, ScrollView, Text } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useWorkspace } from '@/workspace';
import { FocusBridge } from '@/focusBridge';
import { storage } from '@/storage';
import { Button, Card, ScreenTitle, s } from '@/ui';
import { colors } from '@/theme';

export default function Blocked() {
  const params = useLocalSearchParams<{ app?: string }>();
  const { currentTask } = useWorkspace();
  const [appName, setAppName] = useState(params.app || 'This app');
  useEffect(() => { storage.blockedApps().then(apps => { const match = apps.find(a => a.packageName === params.app); if (match) setAppName(match.appName); }); }, [params.app]);

  const endBlocking = async () => {
    await FocusBridge.setBlockingEnabled(false);
    await storage.saveBlockingEnabled(false);
    router.replace('/settings');
  };

  return <SafeAreaView style={s.page}><ScrollView contentContainerStyle={s.content}>
    <ScreenTitle eyebrow="App blocking is active" title="Blocked" />
    <Card accent={colors.red}>
      <Text style={s.h2}>{appName} is blocked</Text>
      <Text style={s.body}>You chose to block this app during your current WEFT work session{currentTask ? ` on “${currentTask.task_name}”` : ''}. It will be available again once you pause, stop, or complete the session.</Text>
    </Card>
    <Button onPress={() => router.replace('/(tabs)/today')}>Back to WEFT</Button>
    <Button variant="danger" onPress={endBlocking}>End blocking for this session</Button>
  </ScrollView></SafeAreaView>;
}
