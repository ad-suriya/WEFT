import { useState } from 'react';
import { RefreshControl, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';
import { api } from '@/api';
import { useWorkspace } from '@/workspace';
import { Badge, Button, Card, Empty, ScreenTitle, s } from '@/ui';
import { colors } from '@/theme';

const sameDay = (value: string | null) => value && new Date(value).toDateString() === new Date().toDateString();
export default function Today() {
  const { data, refreshing, refresh, currentTask, activeSession } = useWorkspace(); const [scheduling, setScheduling] = useState(false);
  const today = data.tasks.filter(t => sameDay(t.scheduled_start) || (t.deadline && sameDay(t.deadline))).filter(t => !['COMPLETED', 'ARCHIVED'].includes(t.status));
  const resume = data.workState;
  const schedule = async () => { setScheduling(true); try { await api.schedule(); await refresh(); } finally { setScheduling(false); } };
  return <ScrollView style={s.page} contentContainerStyle={s.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}><ScreenTitle eyebrow={new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })} title="Today" action={<Badge color={activeSession ? colors.green : colors.ink}>{activeSession ? activeSession.is_paused ? 'Paused' : 'Live' : 'Synced'}</Badge>} />
    {data.reminders.filter(r => r.due).map(reminder => <Card key={`reminder-${reminder.id}`} accent={colors.amber}><Text style={s.eyebrow}>Reminder · {reminder.kind}</Text><Text style={s.body}>{reminder.message}</Text><Button variant="light" onPress={async () => { await api.ackReminder(reminder.id); await refresh(); }}>Dismiss</Button></Card>)}
    {(resume || currentTask) && <Card accent={colors.blue}><Text style={s.eyebrow}>{resume?.status === 'ACTIVE' ? 'Current work' : 'Continue your thread'}</Text><Text style={s.h2}>{currentTask?.task_name || resume?.current_step}</Text><View style={s.divider} /><Text style={s.muted}>You stopped at</Text><Text style={s.h3}>{resume?.current_step || currentTask?.task_name}</Text><Text style={s.muted}>Last activity</Text><Text style={s.body}>{resume?.last_activity || 'Ready to continue'}</Text><Text style={s.muted}>Next</Text><Text style={s.body}>{resume?.next_action || currentTask?.next_micro_step || 'Continue the task'}</Text>{resume?.browser_context && <View><Text style={s.muted}>Relevant browser context</Text><Text style={s.body} numberOfLines={2}>{resume.browser_context.title}</Text></View>}<Button onPress={() => router.push('/focus')}>{activeSession && !activeSession.is_paused ? 'Open current work' : 'Resume work'}</Button></Card>}
    <View style={s.between}><Text style={s.h2}>Your plan</Text><Button variant="light" busy={scheduling} onPress={schedule} style={{ minHeight: 40, paddingVertical: 8 }}>Replan</Button></View>{today.length ? today.map(task => <Card key={task.id}><View style={s.between}><Badge color={task.urgency === 'HIGH' ? colors.red : task.urgency === 'MEDIUM' ? colors.amber : colors.green}>{task.urgency}</Badge><Text style={s.muted}>{task.estimated_minutes} min</Text></View><Text style={s.h3}>{task.task_name}</Text>{task.next_micro_step && <Text style={s.body}>→ {task.next_micro_step}</Text>}<Button variant="light" onPress={() => router.push({ pathname: '/focus', params: { taskId: String(task.id) } })}>Start work</Button></Card>) : <Empty>No scheduled work yet. Create a goal, add a task, or generate today’s plan.</Empty>}
  </ScrollView>;
}
