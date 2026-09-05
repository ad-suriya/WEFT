import { useEffect, useState } from 'react';
import { BackHandler, SafeAreaView, ScrollView, Text } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useWorkspace } from '@/workspace';
import { FocusBridge } from '@/focusBridge';
import { storage } from '@/storage';
import { randomBlockQuote } from '@/blockQuotes';
import { Button, Card, ScreenTitle, s } from '@/ui';
import { colors } from '@/theme';

export default function Blocked() {
  const params = useLocalSearchParams<{ app?: string }>();
  const { currentTask } = useWorkspace();
  const [appName, setAppName] = useState(params.app || 'This app');
  const [quote] = useState(randomBlockQuote);
  useEffect(() => { storage.blockedApps().then(apps => { const match = apps.find(a => a.packageName === params.app); if (match) setAppName(match.appName); }); }, [params.app]);

  // This screen only ever leaves via the explicit Exit button below — the hardware back button
  // must not be a silent escape hatch, or "blocked until you exit" isn't actually true.
  useEffect(() => { const sub = BackHandler.addEventListener('hardwareBackPress', () => true); return () => sub.remove(); }, []);

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
    <Card accent={colors.blue}>
      <Text style={s.h3}>{quote}</Text>
    </Card>
    <Button onPress={() => router.replace('/(tabs)/today')}>Exit — back to WEFT</Button>
    <Button variant="danger" onPress={endBlocking}>End blocking for this session</Button>
  </ScrollView></SafeAreaView>;
}
