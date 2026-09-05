export type Status = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED';
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Task {
  id: number; task_name: string; status: Status; urgency: Urgency;
  estimated_minutes: number; completed_minutes?: number; deadline: string | null;
  next_micro_step: string; scheduled_start: string | null; scheduled_end: string | null;
  goal_id: number | null; created_at: string; updated_at: string;
  dependencies?: number[]; tags?: string[]; url?: string | null; selected_text?: string | null;
  risk?: { risk_percent: number; risk_level: 'safe' | 'medium' | 'high'; reason: string } | null;
}
export interface Goal { id: number; title: string; description: string; metric: string; target_value: number; current_value: number; deadline: string | null; linked_total: number; linked_done: number; created_at: string; updated_at: string; }
export interface Session { id: number; description: string; project_id: number | null; task_id?: number | null; start_time: string; end_time: string | null; duration_minutes: number; is_paused: boolean; created_at: string; updated_at: string; }
export interface WorkflowStep { task_name: string; urgency: Urgency; estimated_minutes: number; tags: string[]; }
export interface WorkflowPlan { name: string; trigger_type: 'DAILY' | 'WEEKLY' | 'ON_TASK_COMPLETE' | 'MANUAL'; trigger_match: string; steps: WorkflowStep[]; }
export interface Workflow extends WorkflowPlan { id: number; sop_text: string; active: boolean; last_run: string | null; created_at: string; updated_at: string; }
export interface Reminder { id: number; task_id: number | null; message: string; remind_at: string; kind: string; acknowledged: number; due: boolean; }
export interface Habit { id: number; name: string; cadence: 'DAILY' | 'WEEKLY'; streak: number; done_today: boolean; total_done: number; created_at: string; }
export interface Activity { id: number; kind: string; message: string; task_id?: number | null; created_at: string; device_id?: string | null; }
export interface BrowserContext { title: string; url: string; relevant: boolean | null; captured_at: string; device_id?: string; }
export interface WorkState {
  task_id: number | null; workflow_id: number | null; current_step_index: number;
  current_step: string; last_activity: string; next_action: string;
  status: 'IDLE' | 'ACTIVE' | 'PAUSED'; active_session_id: number | null;
  started_at: string | null; updated_at: string; device_id?: string | null;
  browser_context?: BrowserContext | null;
}
export interface DeviceRecord { device_id: string; name: string; device_type: string; last_seen: string; capabilities: string[]; push_token?: string | null; }
export interface FocusPrefs { study_focus: boolean; hold_notifications: boolean; allow_list: string[]; }
export interface Profile { id: string; email?: string; name?: string; picture?: string; consent_accepted_at?: string; focus_prefs?: FocusPrefs; }
export interface WorkspaceSnapshot { tasks: Task[]; goals: Goal[]; habits: Habit[]; sessions: Session[]; workflows: Workflow[]; reminders: Reminder[]; activities: Activity[]; devices: DeviceRecord[]; workState: WorkState | null; }
