import { useEffect, useState } from 'react';
import { RefreshControl, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useAuth } from '@/auth';
import { useWorkspace } from '@/workspace';
import { FocusBridge } from '@/focusBridge';
import { shouldActivateFocus } from '@/sessionStatus';
import { buildDeviceViews, type DeviceCardView } from '@/devicesView';
import { Badge, Button, Card, ScreenTitle, s } from '@/ui';
import { colors } from '@/theme';

function DeviceCard({ view }: { view: DeviceCardView }) {
  return <Card accent={view.connected ? colors.green : undefined}>
    <View style={s.between}>
      <Text style={s.eyebrow}>{view.label}</Text>
      <Badge color={view.connected ? colors.green : colors.muted}>{view.connected ? 'Connected' : 'Offline'}</Badge>
    </View>
    {view.sessionNote && <Text style={s.body}>{view.label} — {view.sessionNote}</Text>}
    {!view.connected && <Text style={s.muted}>{view.lastSeenLabel ? `Last seen ${view.lastSeenLabel}` : 'Never registered on this account yet.'}</Text>}
  </Card>;
}

export default function Devices() {
  const { profile } = useAuth();
  const { data, refreshing, refresh } = useWorkspace();
  const [phoneFocusActive, setPhoneFocusActive] = useState(false);

  useEffect(() => {
    let alive = true;
    (async () => {
      const available = await FocusBridge.isAvailable();
      const access = available ? await FocusBridge.hasAccess() : false;
      if (alive) setPhoneFocusActive(shouldActivateFocus(profile?.focus_prefs?.study_focus, available, access));
    })();
    return () => { alive = false; };
  }, [profile?.focus_prefs?.study_focus]);

  const views = buildDeviceViews({ devices: data.devices, workState: data.workState, phoneFocusActive });

  return <ScrollView style={s.page} contentContainerStyle={s.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}>
    <ScreenTitle eyebrow="Your WEFT ecosystem" title="Devices" />
    <Text style={s.body}>Connected status comes from each device's own heartbeat — nothing here is assumed. Pairing is automatic: sign in with the same account on your phone, the WEFT website, and the browser extension.</Text>
    {views.map(view => <DeviceCard key={view.category} view={view} />)}
    <Button variant="light" onPress={() => router.push('/settings')}>Settings</Button>
  </ScrollView>;
}
