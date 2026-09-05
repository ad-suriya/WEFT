import type { Status, Task } from './types';

export type TaskDraft = { task_name: string; next_micro_step: string; deadline: string | null };
export const emptyDraft: TaskDraft = { task_name: '', next_micro_step: '', deadline: null };

export const deadlineChoices: { label: string; days: number | null }[] = [
  { label: 'None', days: null },
  { label: 'Today', days: 0 },
  { label: 'Tomorrow', days: 1 },
  { label: '3 days', days: 3 },
  { label: '1 week', days: 7 },
];

/** End-of-day ISO timestamp `days` from `now` (or null for "no deadline"). */
export function deadlineFromDays(days: number | null, now: Date = new Date()): string | null {
  if (days === null) return null;
  const d = new Date(now); d.setDate(d.getDate() + days); d.setHours(23, 59, 0, 0);
  return d.toISOString();
}

/** Whole calendar-day difference between two dates, ignoring time-of-day (so an end-of-day deadline still reads as "today"). */
function calendarDaysBetween(a: Date, b: Date): number {
  const utcA = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate());
  const utcB = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate());
  return Math.round((utcA - utcB) / 86400000);
}

export function deadlineLabel(iso: string | null, now: number = Date.now()): string | null {
  if (!iso) return null;
  const days = calendarDaysBetween(new Date(iso), new Date(now));
  const date = new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  if (days < 0) return `Overdue · ${date}`;
  if (days === 0) return `Due today · ${date}`;
  if (days === 1) return `Due tomorrow · ${date}`;
  return `Due ${date}`;
}

export function isOverdue(task: Task, now: number = Date.now()): boolean {
  return !!task.deadline && task.status !== 'COMPLETED' && new Date(task.deadline).getTime() < now;
}

export interface TaskGroups { visible: Task[]; active: Task[]; upcoming: Task[]; completed: Task[]; }
/** Splits the shared task list into the My Work sections: active, upcoming, completed. Archived tasks are hidden. */
export function groupTasks(tasks: Task[]): TaskGroups {
  const visible = tasks.filter(t => t.status !== 'ARCHIVED');
  const active = visible.filter(t => t.status === 'IN_PROGRESS');
  const upcoming = visible.filter(t => t.status === 'TODO').sort((a, b) => (a.deadline || 'z').localeCompare(b.deadline || 'z'));
  const completed = visible.filter(t => t.status === 'COMPLETED').sort((a, b) => b.updated_at.localeCompare(a.updated_at));
  return { visible, active, upcoming, completed };
}

export function buildTaskPayload(draft: TaskDraft): { task_name: string; next_micro_step: string; deadline: string | null } {
  return { task_name: draft.task_name.trim(), next_micro_step: draft.next_micro_step.trim(), deadline: draft.deadline };
}

export function nextStatusOnToggleComplete(status: Status): Status {
  return status === 'COMPLETED' ? 'TODO' : 'COMPLETED';
}
