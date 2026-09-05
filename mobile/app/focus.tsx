import { useEffect, useMemo, useState } from 'react';
import { Alert, AppState, SafeAreaView, ScrollView, Text, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { api } from '@/api';
import { useWorkspace } from '@/workspace';
import { Badge, Button, Card, Field, s } from '@/ui';
import { colors } from '@/theme';
import { FocusBridge } from '@/focusBridge';
import { storage } from '@/storage';
import { useAuth } from '@/auth';
import { buildAdvanceStepPatch, buildWorkflowProgress, findActiveWorkflow, resolveWorkflowLink, stepNameForLink, taskForStepName } from '@/workflowProgress';
import { browserStatus, deriveFocusStatus, inferFocusEnabledOnMount, laptopStatus, shouldActivateFocus, type ConnectionState, type FocusState } from '@/sessionStatus';
import { inferBlockingEnabledOnMount, shouldActivateBlocking } from '@/appBlocking';

const elapsed = (start: string) => Math.max(0, Math.floor((Date.now() - new Date(start).getTime()) / 1000));
const statusColor = (state: ConnectionState | FocusState) => state === 'connected' || state === 'on' ? colors.green : state === 'access_needed' ? colors.amber : colors.muted;

export default function Focus() {
  const params = useLocalSearchParams<{ taskId?: string }>();
  const { profile } = useAuth();
  const { data, currentTask, activeSession, refresh, saveWorkState } = useWorkspace();
  const chosen = data.tasks.find(t => t.id === Number(params.taskId)) || currentTask;
  const trackingThisTask = data.workState?.task_id === chosen?.id;
  const [tick, setTick] = useState(0);
  const [activity, setActivity] = useState('');
  const [next, setNext] = useState(trackingThisTask ? data.workState?.next_action || '' : chosen?.next_micro_step || '');
  const [busy, setBusy] = useState(false);
  const [focusAvailable, setFocusAvailable] = useState<boolean | null>(null);
  const [focusAccess, setFocusAccess] = useState<boolean | null>(null);
  const [focusEnabled, setFocusEnabled] = useState(() => inferFocusEnabledOnMount(trackingThisTask, data.workState?.status, profile?.focus_prefs?.study_focus));
  const [blockingPref, setBlockingPref] = useState<boolean | null>(null);
  const [blockingAvailable, setBlockingAvailable] = useState<boolean | null>(null);
  const [blockingAccess, setBlockingAccess] = useState<boolean | null>(null);
  const [blockingActive, setBlockingActive] = useState(() => inferBlockingEnabledOnMount(trackingThisTask, data.workState?.status, false));
  useEffect(() => { const timer = setInterval(() => setTick(x => x + 1), 1000); return () => clearInterval(timer); }, []);
  useEffect(() => { setNext(trackingThisTask ? data.workState?.next_action || '' : chosen?.next_micro_step || ''); }, [chosen?.id]);
  useEffect(() => { let alive = true; (async () => { const available = await FocusBridge.isAvailable(); const access = available ? await FocusBridge.hasAccess() : false; if (alive) { setFocusAvailable(available); setFocusAccess(access); } })(); return () => { alive = false; }; }, []);
  useEffect(() => { let alive = true; (async () => { const pref = await storage.blockingEnabled(); const available = await FocusBridge.isAvailable(); const access = available ? await FocusBridge.hasBlockingAccess() : false; if (alive) { setBlockingPref(pref); setBlockingAvailable(available); setBlockingAccess(access); setBlockingActive(a => a || inferBlockingEnabledOnMount(trackingThisTask, data.workState?.status, pref)); } })(); return () => { alive = false; }; }, []);

  const seconds = useMemo(() => activeSession ? elapsed(activeSession.start_time) : 0, [activeSession, tick]);
  const clock = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  const activeWorkflow = findActiveWorkflow(data.workflows, data.workState, chosen?.id);
  const progress = activeWorkflow ? buildWorkflowProgress(activeWorkflow, data.workState, data.tasks) : null;
  const laptop = laptopStatus(data.devices);
  const browser = browserStatus(data.workState?.browser_context);
  const focus = deriveFocusStatus({ isAvailable: focusAvailable, hasAccess: focusAccess, enabled: focusEnabled });
  const blocking = deriveFocusStatus({ isAvailable: blockingAvailable, hasAccess: blockingAccess, enabled: blockingActive });

  // Focus only ever turns ON when the preference is set AND the permission is actually granted (never assumed).
  // It turns OFF whenever we're the one who turned it on this session, regardless of whether the preference
  // has since changed — so we never leave the phone silently stuck in Do Not Disturb.
  const applyFocusBridge = async (enabled: boolean) => {
    if (enabled) {
      if (!shouldActivateFocus(profile?.focus_prefs?.study_focus, await FocusBridge.isAvailable(), await FocusBridge.hasAccess())) { setFocusEnabled(false); return; }
      const ok = await FocusBridge.setEnabled(true);
      setFocusEnabled(ok);
      return;
    }
    if (!focusEnabled) return;
    await FocusBridge.setEnabled(false);
    setFocusEnabled(false);
  };

  // Same rule as Focus/DND: activates only when the user turned App Blocking on AND the
  // Accessibility permission is actually granted; deactivates whenever we're the one who turned it
  // on, regardless of preference changes since — so a blocked app is never left blocked forever.
  const applyAppBlocking = async (enabled: boolean) => {
    if (enabled) {
      const pref = await storage.blockingEnabled();
      if (!shouldActivateBlocking(pref, await FocusBridge.hasBlockingAccess())) { setBlockingActive(false); return; }
      const ok = await FocusBridge.setBlockingEnabled(true);
      setBlockingActive(ok);
      return;
    }
    if (!blockingActive) return;
    await FocusBridge.setBlockingEnabled(false);
    setBlockingActive(false);
  };

  // Completing a workflow step must also mark its underlying task done (so My Work/Activity agree
  // with the checklist), and move "current work" on to whichever task the next step actually is
  // (so this screen doesn't keep showing the task that was just finished as if it were still current).
  const advanceStep = async () => {
    if (!progress) return;
    const patch = buildAdvanceStepPatch(progress);
    if (!patch) return;
    const completingTask = taskForStepName(data.tasks, progress.currentStepName);
    if (completingTask && completingTask.status !== 'COMPLETED') await api.patchTask(completingTask.id, { status: 'COMPLETED' });
    await saveWorkState(patch);
    await refresh();
    const nextTask = patch.status !== 'IDLE' ? taskForStepName(data.tasks, patch.current_step) : null;
    if (nextTask && nextTask.id !== chosen?.id) router.replace({ pathname: '/focus', params: { taskId: String(nextTask.id) } });
  };

  const start = async () => {
    if (!chosen) return;
    setBusy(true);
    try {
      if (chosen.status !== 'IN_PROGRESS') await api.patchTask(chosen.id, { status: 'IN_PROGRESS' });
      const sameTaskSession = activeSession?.task_id === chosen.id ? activeSession : null;
      const session = sameTaskSession || await api.startSession(chosen.task_name, chosen.estimated_minutes || 25, chosen.id);
      const link = resolveWorkflowLink(data.workflows, data.workState, chosen.id, chosen.task_name);
      const stepName = stepNameForLink(data.workflows, link, chosen.task_name);
      await saveWorkState({ task_id: chosen.id, workflow_id: link.workflow_id, current_step_index: link.current_step_index, current_step: stepName, next_action: next || chosen.next_micro_step || 'Continue', last_activity: sameTaskSession ? 'Resumed work' : 'Started work', status: 'ACTIVE', active_session_id: session.id, started_at: session.start_time });
      await applyFocusBridge(true);
      await applyAppBlocking(true);
      await refresh();
    } catch (e) { Alert.alert('Could not start', e instanceof Error ? e.message : 'Try again.'); }
    finally { setBusy(false); }
  };
  const pause = async () => { if (!activeSession || !chosen) return; await api.patchSession(activeSession.id, { is_paused: true }); await saveWorkState({ status: 'PAUSED', last_activity: activity.trim() || 'Paused work', next_action: next.trim() || chosen.next_micro_step }); await applyFocusBridge(false); await applyAppBlocking(false); await refresh(); };
  const resume = async () => { if (!activeSession || !chosen) return start(); await api.patchSession(activeSession.id, { is_paused: false }); await saveWorkState({ status: 'ACTIVE', last_activity: 'Resumed work', next_action: next.trim() || chosen.next_micro_step }); await applyFocusBridge(true); await applyAppBlocking(true); await refresh(); };
  const stop = async (complete = false) => {
    if (!chosen) return;
    if (activeSession) await api.patchSession(activeSession.id, { end_time: new Date().toISOString(), is_paused: false });
    await api.patchTask(chosen.id, { status: complete ? 'COMPLETED' : 'TODO', next_micro_step: next.trim() });
    await saveWorkState({ task_id: complete ? null : chosen.id, last_activity: activity.trim() || (complete ? 'Completed work' : 'Stopped work'), next_action: complete ? 'Choose the next task' : next.trim() || 'Continue where you stopped', status: 'IDLE', active_session_id: null });
    await applyFocusBridge(false);
    await applyAppBlocking(false);
    await refresh();
    if (complete) router.back();
  };

  return <SafeAreaView style={s.page}><ScrollView contentContainerStyle={s.content}>
    <View style={s.between}><Button variant="light" onPress={() => router.back()}>Back</Button><Badge color={activeSession?.is_paused ? colors.amber : activeSession ? colors.green : colors.ink}>{activeSession?.is_paused ? 'Paused' : activeSession ? 'Working' : 'Ready'}</Badge></View>
    {chosen ? <>
      <Card accent={colors.blue}>
        <Text style={s.eyebrow}>Current work</Text>
        <Text style={s.h2}>{chosen.task_name}</Text>
        <Text style={s.muted}>Current step</Text>
        <Text style={s.h3}>{data.workState?.current_step || chosen.task_name}</Text>
        <Text style={s.muted}>Last action</Text>
        <Text style={s.body}>{(trackingThisTask && data.workState?.last_activity) || 'No activity recorded yet.'}</Text>
        <Text style={s.muted}>Next action</Text>
        <Text style={s.body}>{next || 'Set the next concrete action below.'}</Text>
      </Card>

      {progress && <Card accent={colors.blue}>
        <View style={s.between}><Text style={s.eyebrow}>Workflow</Text><Badge color={colors.blue}>{progress.progressLabel}</Badge></View>
        <Text style={s.h3}>{progress.workflow.name}</Text>
        <View style={{ gap: 6 }}>{progress.steps.map(step => <View key={step.index}>
          <View style={s.row}>
            <Text style={{ width: 20, fontSize: 15, fontWeight: '900', color: step.state === 'done' ? colors.green : step.state === 'current' ? colors.blue : colors.muted }}>{step.state === 'done' ? '✓' : step.state === 'current' ? '→' : '○'}</Text>
            <Text style={[s.body, step.state === 'done' && { color: colors.muted, textDecorationLine: 'line-through' }, step.state === 'current' && { fontWeight: '800' }]}>{step.taskName}</Text>
          </View>
          {step.dependsOn.length > 0 && <Text style={[s.muted, { marginLeft: 30 }]}>Depends on: {step.dependsOn.join(', ')}</Text>}
        </View>)}</View>
        <Text style={s.muted}>Next</Text>
        <Text style={s.body}>{progress.isComplete ? 'Workflow complete' : progress.nextStepName || 'This is the last step'}</Text>
        {!progress.isComplete && <Button onPress={advanceStep}>{`Complete "${progress.currentStepName}"`}</Button>}
      </Card>}

      <Card>
        <Text style={{ fontFamily: 'monospace', fontSize: 56, textAlign: 'center', fontWeight: '800', color: colors.ink }}>{clock}</Text>
        {!activeSession && <Button busy={busy} onPress={start}>Start work</Button>}
        {activeSession?.is_paused && <Button onPress={resume}>Resume work</Button>}
        {activeSession && !activeSession.is_paused && <Button variant="light" onPress={pause}>Pause</Button>}
      </Card>

      <Card>
        <Text style={s.h3}>Connections</Text>
        <View style={s.between}><Text style={s.body}>Laptop</Text><Badge color={statusColor(laptop.state)}>{laptop.label}</Badge></View>
        <View style={s.between}><Text style={s.body}>Browser</Text><Badge color={statusColor(browser.state)}>{browser.label}</Badge></View>
        <View style={s.between}><Text style={s.body}>Focus</Text><Badge color={statusColor(focus.state)}>{focus.label}</Badge></View>
        <View style={s.between}><Text style={s.body}>App Blocking</Text><Badge color={statusColor(blocking.state)}>{blocking.label}</Badge></View>
      </Card>

      <Card>
        <Text style={s.h3}>Leave a precise handoff</Text>
        <Field placeholder="Last activity — e.g. Solved Example 1" value={activity} onChangeText={setActivity} />
        <Field placeholder="Next — e.g. Complete Example 2" value={next} onChangeText={setNext} />
        <Button variant="light" onPress={() => stop(false)}>Stop</Button>
        <Button onPress={() => stop(true)}>Complete</Button>
      </Card>

      {data.workState?.browser_context && <Card><Text style={s.eyebrow}>Browser context</Text><Text style={s.h3}>{data.workState.browser_context.title}</Text><Text style={s.muted} numberOfLines={2}>{data.workState.browser_context.url}</Text></Card>}
    </> : <Card><Text style={s.h2}>Nothing selected</Text><Text style={s.body}>Choose a task from Today or My Work.</Text><Button onPress={() => router.replace('/(tabs)/work')}>Choose work</Button></Card>}
  </ScrollView></SafeAreaView>;
}
