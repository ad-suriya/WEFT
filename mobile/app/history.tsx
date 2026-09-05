import { RefreshControl, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useWorkspace } from '@/workspace';
import { buildActivityTimeline } from '@/activityView';
import { Badge, Button, Card, Empty, ScreenTitle, s } from '@/ui';

export default function History() {
  const { data, refreshing, refresh } = useWorkspace();
  const rows = buildActivityTimeline(data);
  return <ScrollView style={s.page} contentContainerStyle={s.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}>
    <Button variant="light" onPress={() => router.back()}>Back</Button>
    <ScreenTitle eyebrow="Your continuous thread" title="Activity" />
    {rows.length ? rows.map(row => <Card key={row.key}><View style={s.between}><Badge color={row.color}>{row.label}</Badge><Text style={s.muted}>{new Date(row.timestamp).toLocaleString()}</Text></View><Text style={s.body}>{row.message}</Text></Card>) : <Empty>Your work sessions, pauses, resumes, completed steps, completed tasks, and saved references will appear here.</Empty>}
  </ScrollView>;
}
