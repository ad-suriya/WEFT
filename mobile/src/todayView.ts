import { colors } from './theme';
import type { DeviceRecord, Session, Task, WorkspaceSnapshot } from './types';

export const ONLINE_WINDOW_MS = 10 * 60 * 1000;

export type DeviceIconName = 'globe-outline' | 'phone-portrait-outline' | 'laptop-outline';
export function deviceIcon(type: string): DeviceIconName {
  if (type === 'browser') return 'globe-outline';
  if (type === 'android' || type === 'ios') return 'phone-portrait-outline';
  return 'laptop-outline';
}

export function relativeTime(iso: string, now: number = Date.now()): string {
  const mins = Math.max(0, Math.round((now - new Date(iso).getTime()) / 60000));
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.round(hours / 24)}d ago`;
}

export function deviceConnection(device: DeviceRecord, now: number = Date.now()): { online: boolean; label: string } {
  const online = now - new Date(device.last_seen).getTime() < ONLINE_WINDOW_MS;
  return { online, label: online ? 'Connected' : relativeTime(device.last_seen, now) };
}

export type SessionStatus = 'ACTIVE' | 'PAUSED' | 'IDLE';
export function deriveSessionStatus(workState: WorkspaceSnapshot['workState'], activeSession: Session | null): SessionStatus {
  return workState?.status ?? (activeSession ? (activeSession.is_paused ? 'PAUSED' : 'ACTIVE') : 'IDLE');
}

/** Minutes actually worked in the most recently-ended session for a task, from the existing shared session history — not a new field, no estimate. */
export function lastSessionMinutes(sessions: Session[], taskId: number | null): number | null {
  if (taskId == null) return null;
  const ended = sessions
    .filter((s): s is Session & { end_time: string } => s.task_id === taskId && !!s.end_time)
    .sort((a, b) => b.end_time.localeCompare(a.end_time));
  const last = ended[0];
  if (!last) return null;
  const minutes = Math.round((new Date(last.end_time).getTime() - new Date(last.start_time).getTime()) / 60000);
  return minutes >= 0 ? minutes : null;
}

export interface ResumeView {
  workTitle: string;
  stepName: string | null;
  stoppedAtLabel: string | null;
  progressLabel: string | null;
  lastSessionMinutes: number | null;
  nextAction: string | null;
  sessionStatus: SessionStatus;
  statusColor: string;
  statusLabel: string;
  eyebrow: string;
  buttonLabel: string;
  isWelcomeBack: boolean;
}

/**
 * Pure view-model for Today's "resume" card — WEFT's Exact Resume experience. Built entirely from the
 * shared work state, workflow, and session history already synced from the backend (same data the
 * website reads), so resuming continues the actual previous work state rather than just reopening a task.
 */
export function deriveResumeView(data: WorkspaceSnapshot, currentTask: Task | null, activeSession: Session | null): ResumeView {
  const resume = data.workState;
  const workflow = resume?.workflow_id ? data.workflows.find(w => w.id === resume.workflow_id) || null : null;
  const stepIndex = resume?.current_step_index ?? 0;
  const stepName = workflow?.steps[stepIndex]?.task_name || resume?.current_step || null;
  const progressLabel = workflow && workflow.steps.length ? `${Math.min(stepIndex + 1, workflow.steps.length)}/${workflow.steps.length}` : null;
  const workTitle = currentTask?.task_name || workflow?.name || resume?.current_step || 'Untitled work';
  const nextAction = resume?.next_action || currentTask?.next_micro_step || null;
  const stoppedAtLabel = stepName ? (nextAction ? `${stepName} → ${nextAction}` : stepName) : null;

  const sessionStatus = deriveSessionStatus(resume, activeSession);
  const hasResumeData = !!(resume || currentTask);
  const isWelcomeBack = sessionStatus === 'IDLE' && hasResumeData;
  const taskIdForHistory = currentTask?.id ?? resume?.task_id ?? null;
  const lastMinutes = isWelcomeBack ? lastSessionMinutes(data.sessions, taskIdForHistory) : null;

  const statusColor = sessionStatus === 'ACTIVE' ? colors.green : sessionStatus === 'PAUSED' ? colors.amber : colors.ink;
  const statusLabel = sessionStatus === 'ACTIVE' ? 'Live' : sessionStatus === 'PAUSED' ? 'Paused' : 'Synced';
  const eyebrow = isWelcomeBack ? 'Welcome back' : sessionStatus === 'ACTIVE' ? 'Current work' : sessionStatus === 'PAUSED' ? 'Paused work' : 'Continue your thread';
  const buttonLabel = sessionStatus === 'ACTIVE' ? 'Continue Work' : 'Resume Work';

  return { workTitle, stepName, stoppedAtLabel, progressLabel, lastSessionMinutes: lastMinutes, nextAction, sessionStatus, statusColor, statusLabel, eyebrow, buttonLabel, isWelcomeBack };
}
