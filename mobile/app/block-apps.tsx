import { useEffect, useState } from 'react';
import { Alert, FlatList, SafeAreaView, View } from 'react-native';
import * as Application from 'expo-application';
import { router } from 'expo-router';
import { FocusBridge, type InstalledApp } from '@/focusBridge';
import { storage } from '@/storage';
import { isAppSelected, selectableApps, toggleBlockedApp } from '@/appBlocking';
import { Button, Empty, ScreenTitle, Toggle, s } from '@/ui';

export default function BlockApps() {
  const [apps, setApps] = useState<InstalledApp[] | null>(null);
  const [selected, setSelected] = useState<InstalledApp[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      const [installed, current] = await Promise.all([FocusBridge.listInstalledApps(), storage.blockedApps()]);
      setApps(selectableApps(installed, Application.applicationId || ''));
      setSelected(current);
    })();
  }, []);

  const save = async () => {
    setSaving(true);
    try {
      await storage.saveBlockedApps(selected);
      await FocusBridge.setBlockedApps(selected.map(a => a.packageName));
      router.back();
    } catch (e) { Alert.alert('Could not save', e instanceof Error ? e.message : 'Try again.'); }
    finally { setSaving(false); }
  };

  return <SafeAreaView style={s.page}>
    <FlatList
      data={apps || []}
      keyExtractor={item => item.packageName}
      contentContainerStyle={s.content}
      ListHeaderComponent={<ScreenTitle eyebrow={apps ? `${selected.length} selected` : 'Loading installed apps…'} title="Block apps" />}
      ListEmptyComponent={apps !== null ? <Empty>No blockable apps were found on this device.</Empty> : null}
      renderItem={({ item }) => <Toggle label={item.appName} value={isAppSelected(selected, item.packageName)} onChange={() => setSelected(sel => toggleBlockedApp(sel, item))} />}
      ItemSeparatorComponent={() => <View style={{ height: 8 }} />}
      ListFooterComponent={<View style={{ paddingTop: 12, gap: 10 }}>
        <Button busy={saving} onPress={save}>Save blocked apps</Button>
        <Button variant="light" onPress={() => router.back()}>Cancel</Button>
      </View>}
    />
  </SafeAreaView>;
}
