import { useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { api } from '@/api';
import { useWorkspace } from '@/workspace';
import { Badge, Button, Card, DeadlinePicker, Empty, Field, ScreenTitle, s } from '@/ui';
import { colors } from '@/theme';
import type { Task } from '@/types';
import { buildTaskPayload, deadlineLabel, emptyDraft, groupTasks, isOverdue, nextStatusOnToggleComplete, type TaskDraft } from '@/workTasks';

type Segment = 'tasks' | 'goals' | 'habits' | 'workflows';

function TaskForm({ initial, busy, onCancel, onSubmit }: { initial?: Partial<TaskDraft>; busy: boolean; onCancel: () => void; onSubmit: (draft: TaskDraft) => void }) {
  const [draft, setDraft] = useState<TaskDraft>({ ...emptyDraft, ...initial });
  return <Card accent={colors.blue}>
    <Field placeholder="Task name" value={draft.task_name} onChangeText={v => setDraft(d => ({ ...d, task_name: v }))} />
    <Field placeholder="Next micro step (optional)" value={draft.next_micro_step} onChangeText={v => setDraft(d => ({ ...d, next_micro_step: v }))} />
    <Text style={s.muted}>Deadline</Text>
    <DeadlinePicker value={draft.deadline} onChange={v => setDraft(d => ({ ...d, deadline: v }))} />
    <View style={s.row}>
      <Button style={{ flex: 1 }} busy={busy} disabled={!draft.task_name.trim()} onPress={() => onSubmit(draft)}>Save</Button>
      <Button variant="light" style={{ flex: 1 }} onPress={onCancel} disabled={busy}>Cancel</Button>
    </View>
  </Card>;
}

function TaskCard({ task, onStart, onToggleComplete, onEdit, onDelete }: { task: Task; onStart: () => void; onToggleComplete: () => void; onEdit: () => void; onDelete: () => void }) {
  const deadline = deadlineLabel(task.deadline);
  return <Card>
    <View style={s.between}>
      <Badge color={task.status === 'COMPLETED' ? colors.green : task.status === 'IN_PROGRESS' ? colors.blue : colors.muted}>{task.status.replace('_', ' ')}</Badge>
      {task.risk && <Text style={[s.muted, task.risk.risk_level === 'high' && { color: colors.red }]}>{task.risk.risk_percent}% risk</Text>}
    </View>
    <Text style={[s.h3, task.status === 'COMPLETED' && { textDecorationLine: 'line-through', color: colors.muted }]}>{task.task_name}</Text>
    {task.next_micro_step && <Text style={s.body}>Next: {task.next_micro_step}</Text>}
    {deadline && <Text style={[s.muted, isOverdue(task) && { color: colors.red, fontWeight: '700' }]}>{deadline}</Text>}
    <View style={s.row}>
      {task.status !== 'COMPLETED' && <Button style={{ flex: 1 }} onPress={onStart}>{task.status === 'IN_PROGRESS' ? 'Resume' : 'Start'}</Button>}
      <Button variant="light" style={{ flex: 1 }} onPress={onToggleComplete}>{task.status === 'COMPLETED' ? 'Reopen' : 'Done'}</Button>
    </View>
    <View style={s.row}>
      <Button variant="light" style={{ flex: 1 }} onPress={onEdit}>Edit</Button>
      <Button variant="danger" style={{ flex: 1 }} onPress={onDelete}>Delete</Button>
    </View>
  </Card>;
}

export default function Work() {
  const { data, refreshing, refresh } = useWorkspace();
  const [segment, setSegment] = useState<Segment>('tasks');
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);

  const addTask = async (draft: TaskDraft) => {
    setBusy(true);
    try { await api.createTask(buildTaskPayload(draft)); setAdding(false); await refresh(); }
    catch (e) { Alert.alert('Could not create task', e instanceof Error ? e.message : 'Try again.'); }
    finally { setBusy(false); }
  };
  const editTask = async (id: number, draft: TaskDraft) => {
    setBusy(true);
    try { await api.patchTask(id, buildTaskPayload(draft)); setEditingId(null); await refresh(); }
    catch (e) { Alert.alert('Could not save task', e instanceof Error ? e.message : 'Try again.'); }
    finally { setBusy(false); }
  };
  const toggleComplete = async (task: Task) => { await api.patchTask(task.id, { status: nextStatusOnToggleComplete(task.status) }); await refresh(); };
  const removeTask = (task: Task) => Alert.alert('Delete task', `Delete "${task.task_name}"? This can't be undone.`, [
    { text: 'Cancel', style: 'cancel' },
    { text: 'Delete', style: 'destructive', onPress: async () => { try { await api.deleteTask(task.id); await refresh(); } catch (e) { Alert.alert('Could not delete task', e instanceof Error ? e.message : 'Try again.'); } } },
  ]);

  const { visible, active, upcoming, completed } = groupTasks(data.tasks);

  const renderTask = (task: Task) => editingId === task.id
    ? <TaskForm key={task.id} initial={{ task_name: task.task_name, next_micro_step: task.next_micro_step, deadline: task.deadline }} busy={busy} onCancel={() => setEditingId(null)} onSubmit={draft => editTask(task.id, draft)} />
    : <TaskCard key={task.id} task={task} onStart={() => router.push({ pathname: '/focus', params: { taskId: String(task.id) } })} onToggleComplete={() => toggleComplete(task)} onEdit={() => setEditingId(task.id)} onDelete={() => removeTask(task)} />;

  return <ScrollView style={s.page} contentContainerStyle={s.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}><ScreenTitle eyebrow="Shared across devices" title="My Work" action={<Pressable onPress={() => router.push('/history')} hitSlop={10}><Ionicons name="pulse-outline" size={24} color={colors.ink} /></Pressable>} />
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.row}>{(['tasks', 'goals', 'habits', 'workflows'] as Segment[]).map(x => <Pressable key={x} onPress={() => setSegment(x)} style={{ minWidth: 92, padding: 11, backgroundColor: segment === x ? colors.ink : colors.card, borderWidth: 1.5, borderColor: colors.ink }}><Text style={{ textAlign: 'center', color: segment === x ? colors.white : colors.ink, fontWeight: '800', textTransform: 'capitalize' }}>{x}</Text></Pressable>)}</ScrollView>

    {segment === 'tasks' && <>
      <View style={s.between}><Text style={s.h2}>Tasks</Text><Button variant="light" style={{ minHeight: 40, paddingVertical: 8 }} onPress={() => { setEditingId(null); setAdding(a => !a); }}>{adding ? 'Close' : '+ Add task'}</Button></View>
      {adding && <TaskForm busy={busy} onCancel={() => setAdding(false)} onSubmit={async draft => { await addTask(draft); }} />}
      {!visible.length && <Empty>No tasks yet.</Empty>}
      {!!active.length && <><Text style={s.eyebrow}>Active · {active.length}</Text>{active.map(renderTask)}</>}
      {!!upcoming.length && <><Text style={s.eyebrow}>Upcoming · {upcoming.length}</Text>{upcoming.map(renderTask)}</>}
      {!!completed.length && <><Text style={s.eyebrow}>Completed · {completed.length}</Text>{completed.map(renderTask)}</>}
    </>}
    {segment === 'goals' && (data.goals.length ? data.goals.map(goal => <Card key={goal.id}><Text style={s.h3}>{goal.title}</Text><Text style={s.body}>{goal.description}</Text><Text style={s.muted}>{goal.current_value} / {goal.target_value} {goal.metric} · {goal.linked_done}/{goal.linked_total} tasks done</Text><View style={{ height: 8, backgroundColor: colors.line }}><View style={{ height: 8, width: `${Math.min(100, goal.target_value ? goal.current_value / goal.target_value * 100 : 0)}%`, backgroundColor: colors.blue }} /></View><Button variant="light" onPress={async () => { await api.incrementGoal(goal.id); await refresh(); }}>Add progress</Button></Card>) : <Empty>No goals yet.</Empty>)}
    {segment === 'habits' && (data.habits.length ? data.habits.map(habit => <Card key={habit.id}><View style={s.between}><Text style={s.h3}>{habit.name}</Text><Badge color={habit.done_today ? colors.green : colors.muted}>{habit.done_today ? 'Done' : habit.cadence}</Badge></View><Text style={s.muted}>{habit.streak} streak · {habit.total_done} total check-ins</Text><Button variant={habit.done_today ? 'light' : 'dark'} onPress={async () => { await api.checkHabit(habit.id); await refresh(); }}>{habit.done_today ? 'Undo today' : 'Check in'}</Button></Card>) : <Empty>No habits yet.</Empty>)}
    {segment === 'workflows' && (data.workflows.length ? data.workflows.map(flow => <Card key={flow.id}><View style={s.between}><Badge color={flow.active ? colors.green : colors.muted}>{flow.active ? 'Active' : 'Paused'}</Badge><Text style={s.muted}>{flow.steps.length} steps</Text></View><Text style={s.h3}>{flow.name}</Text>{flow.steps.slice(0, 3).map((step, i) => <Text key={i} style={s.body}>{i + 1}. {step.task_name}</Text>)}<Button onPress={async () => { await api.runWorkflow(flow.id); await refresh(); }}>Run workflow</Button></Card>) : <Empty>No workflows yet.</Empty>)}
  </ScrollView>;
}
