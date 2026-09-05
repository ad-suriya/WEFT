import { colors } from './theme';
import type { Activity, Reference, Session, Task, WorkspaceSnapshot } from './types';

export type ActivityRowKind = 'session' | 'task_completed' | 'reference' | 'log';

export interface ActivityRow {
  key: string;
  kind: ActivityRowKind;
  label: string;
  message: string;
  timestamp: string;
  color: string;
}

/** One row per ended work session, with its real clocked duration — not shown until it actually ends. */
function sessionRows(sessions: Session[]): ActivityRow[] {
  return sessions
    .filter((s): s is Session & { end_time: string } => !!s.end_time)
    .map(s => {
      const minutes = Math.max(0, Math.round((new Date(s.end_time).getTime() - new Date(s.start_time).getTime()) / 60000));
      return { key: `session-${s.id}`, kind: 'session', label: 'Work session', message: `${s.description || 'Work session'} — ${minutes} min`, timestamp: s.end_time, color: colors.blue };
    });
}

function taskCompletedRows(tasks: Task[]): ActivityRow[] {
  return tasks
    .filter(t => t.status === 'COMPLETED')
    .map(t => ({ key: `task-${t.id}`, kind: 'task_completed', label: 'Task completed', message: t.task_name, timestamp: t.updated_at, color: colors.green }));
}

function referenceRows(references: Reference[]): ActivityRow[] {
  return references.map(r => ({ key: `reference-${r.id}`, kind: 'reference', label: 'Saved reference', message: r.title || r.url, timestamp: r.created_at, color: colors.amber }));
}

/** Starts, pauses, resumes, stops, and completed workflow steps — all already logged with plain, real messages (e.g. "Completed CNF Conversion", "Paused work"). */
function logRows(activities: Activity[]): ActivityRow[] {
  return activities.map(a => ({ key: `activity-${a.id}`, kind: 'log', label: a.kind.replaceAll('_', ' '), message: a.message, timestamp: a.created_at, color: a.kind.includes('COMPLETE') ? colors.green : colors.blue }));
}

/** Chronological merge of every real WEFT history source — no derived stats, no scores, just what actually happened, newest first. */
export function buildActivityTimeline(data: Pick<WorkspaceSnapshot, 'sessions' | 'tasks' | 'references' | 'activities'>): ActivityRow[] {
  return [
    ...sessionRows(data.sessions),
    ...taskCompletedRows(data.tasks),
    ...referenceRows(data.references),
    ...logRows(data.activities),
  ].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
}
