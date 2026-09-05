import { useCallback, useEffect, useState } from 'react';
import { Alert, AppState, Linking, Platform, Pressable, ScrollView, Share, Text, View } from 'react-native';
import * as Notifications from 'expo-notifications';
import { router, useFocusEffect } from 'expo-router';
import { api } from '@/api';
import { useAuth } from '@/auth';
import { useWorkspace } from '@/workspace';
import { Badge, Button, Card, ScreenTitle, Toggle, s } from '@/ui';
import { colors } from '@/theme';
import { FocusBridge, type InstalledApp } from '@/focusBridge';
import { storage } from '@/storage';
import { deriveFocusStatus } from '@/sessionStatus';
import { availableCategories, toggleCategory } from '@/focusPreferences';
import { shouldActivateBlocking } from '@/appBlocking';

export default function Settings() {
  const { profile, signOut, updateFocusPrefs } = useAuth();
  const { data, registerDevice, refresh } = useWorkspace();
  const [focusAvailable, setFocusAvailable] = useState<boolean | null>(null);
  const [focusAccess, setFocusAccess] = useState<boolean | null>(null);
  const [awaitingGrant, setAwaitingGrant] = useState(false);

  const checkFocusPermission = useCallback(async () => {
    const available = Platform.OS === 'android' && await FocusBridge.isAvailable();
    const access = available ? await FocusBridge.hasAccess() : false;
    setFocusAvailable(available);
    setFocusAccess(access);
    return access;
  }, []);
  useEffect(() => { checkFocusPermission(); }, [checkFocusPermission]);
  useEffect(() => {
    if (!awaitingGrant) return;
    const subscription = AppState.addEventListener('change', async state => {
      if (state !== 'active') return;
      const granted = await checkFocusPermission();
      if (granted) { setAwaitingGrant(false); await updateFocusPrefs({ study_focus: true }); }
    });
    return () => subscription.remove();
  }, [awaitingGrant, checkFocusPermission, updateFocusPrefs]);

  const focus = deriveFocusStatus({ isAvailable: focusAvailable, hasAccess: focusAccess, enabled: !!profile?.focus_prefs?.study_focus });
  const permissionLabel = focusAvailable === null ? 'Checking…' : !focusAvailable ? 'Unavailable' : focusAccess ? 'Granted' : 'Not granted';
  const permissionColor = focusAvailable === null ? colors.muted : !focusAvailable ? colors.muted : focusAccess ? colors.green : colors.amber;

  // Turning Study Focus ON requires the permission to already be granted, or the user to grant it
  // themselves in Android's settings — WEFT never flips it on its own. Turning it OFF is immediate.
  const toggleStudyFocus = async (value: boolean) => {
    if (!value) { setAwaitingGrant(false); await updateFocusPrefs({ study_focus: false }); await FocusBridge.setEnabled(false).catch(() => undefined); return; }
    if (Platform.OS !== 'android' || !(await FocusBridge.isAvailable())) { Alert.alert('Development build required', 'Focus Bridge needs the WEFT Expo development build. The rest of the app works in Expo Go.'); return; }
    if (await FocusBridge.hasAccess()) { await updateFocusPrefs({ study_focus: true }); return; }
    setAwaitingGrant(true);
    await FocusBridge.requestAccess();
    Alert.alert('Grant access in Android Settings', 'Turn on notification access for WEFT, then come back to this screen — Study Focus will switch on automatically once it\'s granted.');
  };

  const categories = availableCategories(data.reminders);
  const allowList = profile?.focus_prefs?.allow_list || [];

  const [blockingPref, setBlockingPref] = useState(false);
  const [blockingAccess, setBlockingAccess] = useState<boolean | null>(null);
  const [awaitingBlockGrant, setAwaitingBlockGrant] = useState(false);
  const [blockedApps, setBlockedApps] = useState<InstalledApp[]>([]);

  const checkBlockingPermission = useCallback(async () => {
    const available = Platform.OS === 'android' && await FocusBridge.isAvailable();
    const access = available ? await FocusBridge.hasBlockingAccess() : false;
    setBlockingAccess(access);
    return access;
  }, []);
  useEffect(() => { storage.blockingEnabled().then(setBlockingPref); checkBlockingPermission(); }, [checkBlockingPermission]);
  useFocusEffect(useCallback(() => { storage.blockedApps().then(setBlockedApps); }, []));
  useEffect(() => {
    if (!awaitingBlockGrant) return;
    const subscription = AppState.addEventListener('change', async state => {
      if (state !== 'active') return;
      const granted = await checkBlockingPermission();
      if (granted) { setAwaitingBlockGrant(false); setBlockingPref(true); await storage.saveBlockingEnabled(true); }
    });
    return () => subscription.remove();
  }, [awaitingBlockGrant, checkBlockingPermission]);

  const blockingActive = shouldActivateBlocking(blockingPref, !!blockingAccess) && data.workState?.status === 'ACTIVE';
  const blockingPermissionLabel = blockingAccess === null ? 'Checking…' : Platform.OS !== 'android' ? 'Unavailable' : blockingAccess ? 'Granted' : 'Not granted';
  const blockingPermissionColor = blockingAccess === null ? colors.muted : Platform.OS !== 'android' ? colors.muted : blockingAccess ? colors.green : colors.amber;

  // Same shape as toggleStudyFocus: turning ON only ever takes effect once the Accessibility
  // permission is actually granted (never assumed); turning OFF is immediate and also stops any
  // blocking that's live right now — this IS the "explicit way to end blocking" from WEFT.
  const toggleBlocking = async (value: boolean) => {
    if (!value) { setAwaitingBlockGrant(false); setBlockingPref(false); await storage.saveBlockingEnabled(false); await FocusBridge.setBlockingEnabled(false).catch(() => undefined); return; }
    if (Platform.OS !== 'android' || !(await FocusBridge.isAvailable())) { Alert.alert('Development build required', 'App blocking needs the WEFT Expo development build. The rest of the app works in Expo Go.'); return; }
    if (!blockedApps.length) { Alert.alert('Choose apps first', 'Select at least one app to block below before turning this on.'); return; }
    if (await FocusBridge.hasBlockingAccess()) { setBlockingPref(true); await storage.saveBlockingEnabled(true); return; }
    setAwaitingBlockGrant(true);
    await FocusBridge.requestBlockingAccess();
    Alert.alert('Grant access in Android Settings', 'Turn on the "WEFT Focus Blocking" accessibility service, then come back — App Blocking will switch on automatically once it\'s granted.');
  };

  const enableNotifications = async () => { const result = await Notifications.requestPermissionsAsync(); if (!result.granted) Alert.alert('Notifications are off', 'You can enable them later in Android settings.'); else { await registerDevice(); try { const sent = await api.testPush(); Alert.alert('Notification test sent', `${sent.sent} device${sent.sent === 1 ? '' : 's'} reached.`); } catch { Alert.alert('Notifications enabled', 'This phone is registered. A test push will work after the shared backend integration is deployed.'); } } };

  const exportMyData = async () => {
    try { const exported = await api.exportMyData(); await Share.share({ title: 'WEFT data export', message: JSON.stringify(exported, null, 2) }); }
    catch (e) { Alert.alert('Could not export data', e instanceof Error ? e.message : 'Try again.'); }
  };
  const deleteMyData = () => Alert.alert(
    'Delete all WEFT data',
    'This permanently deletes your tasks, workflows, sessions, goals, habits, reminders and saved references. Your account sign-in stays valid. This cannot be undone.',
    [{ text: 'Cancel', style: 'cancel' }, { text: 'Delete everything', style: 'destructive', onPress: async () => {
      try { await api.deleteMyData(); await refresh(); Alert.alert('Data deleted', 'Your WEFT data has been removed.'); }
      catch (e) { Alert.alert('Could not delete data', e instanceof Error ? e.message : 'Try again.'); }
    } }],
  );

  return <ScrollView style={s.page} contentContainerStyle={s.content}>
    <Button variant="light" onPress={() => router.back()}>Back</Button>
    <ScreenTitle eyebrow="Make WEFT yours" title="Settings" />

    <Card>
      <Text style={s.h2}>Account</Text>
      <Text style={s.h3}>{profile?.name || 'WEFT account'}</Text>
      <Text style={s.muted}>{profile?.email}</Text>
      <Text style={s.body}>The same account is used by your phone, website, and browser extension.</Text>
      <Button variant="light" onPress={() => Linking.openURL('https://task-weave-57923630274.asia-south1.run.app')}>Open WEFT website</Button>
    </Card>

    <Card accent={colors.blue}>
      <View style={s.between}><Text style={s.h2}>Focus preferences</Text><Badge color={statusColorFor(focus.state)}>{focus.label}</Badge></View>
      <Text style={s.body}>When you start active work, WEFT can ask Android to silence interruptions. Android requires you to grant notification-policy access first; WEFT never changes it silently.</Text>
      <View style={s.between}><Text style={s.body}>Android permission</Text><Badge color={permissionColor}>{permissionLabel}</Badge></View>
      <Toggle label="Study Focus" value={!!profile?.focus_prefs?.study_focus} onChange={toggleStudyFocus} />
      {awaitingGrant && <Text style={s.muted}>Waiting for you to grant access in Android Settings…</Text>}
      <Toggle label="Hold WEFT notifications while focused" value={!!profile?.focus_prefs?.hold_notifications} onChange={v => updateFocusPrefs({ hold_notifications: v })} />
      {categories.length > 0 ? <View style={{ gap: 6 }}>
        <Text style={s.muted}>Always allowed through, even while focused</Text>
        <View style={[s.row, { flexWrap: 'wrap' }]}>{categories.map(kind => { const active = allowList.includes(kind); return <Pressable key={kind} onPress={() => updateFocusPrefs({ allow_list: toggleCategory(allowList, kind) })} style={{ paddingHorizontal: 12, paddingVertical: 7, borderWidth: 1.5, borderColor: colors.ink, backgroundColor: active ? colors.blue : colors.card }}><Text style={{ color: active ? colors.white : colors.ink, fontWeight: '800', fontSize: 12 }}>{kind}</Text></Pressable>; })}</View>
      </View> : <Text style={s.muted}>Reminder categories will appear here once you have some, so you can choose which stay on during Focus.</Text>}

      <View style={s.divider} />
      <View style={s.between}><Text style={s.h3}>App Blocking</Text><Badge color={blockingActive ? colors.green : colors.muted}>{blockingActive ? 'ACTIVE' : 'OFF'}</Badge></View>
      <Text style={s.body}>Sends you home instead of opening apps you've chosen to block, for as long as an active WEFT work session is running. Requires turning on the "WEFT Focus Blocking" accessibility service — WEFT never enables it silently.</Text>
      <View style={s.between}><Text style={s.body}>Accessibility permission</Text><Badge color={blockingPermissionColor}>{blockingPermissionLabel}</Badge></View>
      <Toggle label="Block distracting apps" value={blockingPref} onChange={toggleBlocking} />
      {awaitingBlockGrant && <Text style={s.muted}>Waiting for you to grant access in Android Settings…</Text>}
      <Button variant="light" onPress={() => router.push('/block-apps')}>Select apps to block</Button>
      <View style={{ gap: 4 }}>
        <Text style={s.muted}>Blocked apps</Text>
        {blockedApps.length ? blockedApps.map(app => <Text key={app.packageName} style={s.body}>{app.appName}</Text>) : <Text style={s.muted}>None selected yet.</Text>}
      </View>
    </Card>

    <Card><Text style={s.h2}>Notifications</Text><Text style={s.body}>Deadline, focus-start and custom reminders. Remote push requires an Expo development build and Firebase configuration.</Text><Button variant="light" onPress={enableNotifications}>Enable and test notifications</Button></Card>
    <Card><Text style={s.h2}>Calendar integration</Text><Text style={s.body}>Pull external commitments into WEFT and push scheduled work blocks using the website’s existing Google Calendar connection.</Text><Button variant="light" onPress={async () => { try { const status = await api.calendarStatus(); if (!status.connected) return Alert.alert('Connect on the website', 'Open WEFT Website below and connect Google Calendar once.'); await api.calendarSync(); Alert.alert('Calendar synced'); } catch (e) { Alert.alert('Calendar sync failed', e instanceof Error ? e.message : 'Try again.'); } }}>Sync calendar</Button></Card>

    <Card>
      <Text style={s.h2}>Privacy & data</Text>
      <Text style={s.body}>Export everything WEFT stores for your account, or delete it. Your sign-in stays valid either way.</Text>
      <Button variant="light" onPress={exportMyData}>Export my data</Button>
      <Button variant="danger" onPress={deleteMyData}>Delete my data</Button>
    </Card>

    <Button variant="danger" onPress={() => signOut()}>Sign out</Button>
  </ScrollView>;
}

function statusColorFor(state: ReturnType<typeof deriveFocusStatus>['state']) {
  if (state === 'on') return colors.green;
  if (state === 'access_needed') return colors.amber;
  return colors.muted;
}
