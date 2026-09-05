import { useState } from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';
import { api } from '@/api';
import { useWorkspace } from '@/workspace';
import { Button, Card, DeadlinePicker, Field, ScreenTitle, Toggle, s } from '@/ui';
import { colors } from '@/theme';
import { createTaskAndMaybeStart, generateTaskWorkflow as generateWorkflowPlan, saveTaskWorkflow as persistTaskWorkflow } from '@/createTaskFlow';
import type { WorkflowPlan } from '@/types';

type Mode = 'task' | 'goal' | 'habit' | 'workflow';
export default function Create() {
  const { data, refresh, saveWorkState } = useWorkspace();
  const [mode, setMode] = useState<Mode>('task');
  const [title, setTitle] = useState('');
  const [detail, setDetail] = useState('');
  const [deadline, setDeadline] = useState<string | null>(null);
  const [startImmediately, setStartImmediately] = useState(false);
  const [busy, setBusy] = useState(false);
  const [plan, setPlan] = useState<WorkflowPlan | null>(null);

  const resetTaskFields = () => { setTitle(''); setDetail(''); setDeadline(null); setStartImmediately(false); setPlan(null); };

  const createTask = async () => {
    if (!title.trim()) return;
    setBusy(true);
    try {
      const created = await createTaskAndMaybeStart({ api, saveWorkState }, { task_name: title, next_micro_step: detail, deadline }, startImmediately, data.workflows, data.workState);
      resetTaskFields();
      await refresh();
      if (startImmediately) router.push({ pathname: '/focus', params: { taskId: String(created.id) } });
      else Alert.alert('Task created', 'Available on every device.');
    } catch (e) { Alert.alert('Could not create task', e instanceof Error ? e.message : 'Try again.'); }
    finally { setBusy(false); }
  };

  const generateTaskWorkflow = async () => {
    if (!title.trim()) return;
    setBusy(true);
    try { setPlan(await generateWorkflowPlan({ api }, title)); }
    catch (e) { Alert.alert('Could not generate workflow', e instanceof Error ? e.message : 'Try again.'); }
    finally { setBusy(false); }
  };

  const saveTaskWorkflow = async () => {
    if (!plan) return;
    setBusy(true);
    try {
      await persistTaskWorkflow({ api }, plan, title);
      setPlan(null);
      await refresh();
      Alert.alert('Workflow created', `${plan.steps.length} step${plan.steps.length === 1 ? '' : 's'} added as tasks.`);
    } catch (e) { Alert.alert('Could not save workflow', e instanceof Error ? e.message : 'Try again.'); }
    finally { setBusy(false); }
  };

  const submit = async () => { if (!title.trim() && mode !== 'workflow') return; setBusy(true); try { if (mode === 'goal') await api.createGoal({ title: title.trim(), description: detail.trim() }); if (mode === 'habit') await api.createHabit(title.trim()); if (mode === 'workflow') { if (!plan) { setPlan(await api.generateWorkflow(detail.trim())); return; } await api.createWorkflow({ ...plan, sop_text: detail.trim() }); setPlan(null); } setTitle(''); setDetail(''); await refresh(); Alert.alert('Saved', `Your ${mode} is available on every device.`); } catch (e) { Alert.alert('Could not save', e instanceof Error ? e.message : 'Try again.'); } finally { setBusy(false); } };

  return <ScrollView style={s.page} contentContainerStyle={s.content}><ScreenTitle eyebrow="Turn intent into action" title="Create" /><ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.row}>{(['task', 'goal', 'habit', 'workflow'] as Mode[]).map(x => <Pressable key={x} onPress={() => { setMode(x); setPlan(null); }} style={{ minWidth: 88, padding: 11, backgroundColor: mode === x ? colors.blue : colors.card, borderWidth: 1.5, borderColor: colors.ink }}><Text style={{ textAlign: 'center', color: mode === x ? colors.white : colors.ink, fontWeight: '900', textTransform: 'capitalize' }}>{x}</Text></Pressable>)}</ScrollView>

    {mode === 'task' ? <Card>
      <Text style={s.h2}>Capture a task</Text>
      <Field placeholder="What do you need to do? e.g. Finish my CN assignment and practice 5 CNF problems." multiline value={title} onChangeText={setTitle} />
      <Field placeholder="Next concrete action (optional)" value={detail} onChangeText={setDetail} />
      <Text style={s.muted}>Deadline (optional)</Text>
      <DeadlinePicker value={deadline} onChange={setDeadline} />
      <Toggle label="Start immediately" value={startImmediately} onChange={setStartImmediately} />
      <Button busy={busy} disabled={!title.trim()} onPress={createTask}>Create task</Button>
      <View style={s.divider} />
      <Text style={s.h3}>Need a plan for this?</Text>
      <Text style={s.body}>WEFT AI can break this down into a step-by-step workflow you can run as tasks.</Text>
      {plan && <View style={{ gap: 8 }}><Text style={s.h3}>{plan.name}</Text>{plan.steps.map((step, i) => <Text style={s.body} key={i}>{i + 1}. {step.task_name} · {step.estimated_minutes}m</Text>)}</View>}
      <Button variant="light" busy={busy} disabled={!title.trim()} onPress={plan ? saveTaskWorkflow : generateTaskWorkflow}>{plan ? 'Save workflow' : 'Generate step-by-step workflow'}</Button>
      {plan && <Button variant="light" onPress={() => setPlan(null)}>Discard</Button>}
    </Card> : <Card><Text style={s.h2}>{mode === 'workflow' ? 'Describe your process' : mode === 'goal' ? 'Set a goal' : 'Build a habit'}</Text>{mode !== 'workflow' && <Field placeholder={mode === 'goal' ? 'Goal title' : 'Habit name'} value={title} onChangeText={setTitle} />}{mode !== 'habit' && <Field multiline placeholder={mode === 'workflow' ? 'Paste or describe the process. WEFT AI will turn it into reusable steps.' : 'What does success look like?'} value={detail} onChangeText={setDetail} />}{plan && <View style={{ gap: 8 }}><Text style={s.h3}>{plan.name}</Text>{plan.steps.map((step, i) => <Text style={s.body} key={i}>{i + 1}. {step.task_name} · {step.estimated_minutes}m</Text>)}</View>}<Button busy={busy} disabled={mode === 'workflow' ? !detail.trim() : !title.trim()} onPress={submit}>{mode === 'workflow' ? plan ? 'Save workflow' : 'Generate with AI' : `Create ${mode}`}</Button>{plan && <Button variant="light" onPress={() => setPlan(null)}>Regenerate</Button>}</Card>}
  </ScrollView>;
}
