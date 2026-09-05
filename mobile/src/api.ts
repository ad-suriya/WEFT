import Constants from 'expo-constants';
import type { Activity, DeviceRecord, FocusPrefs, Goal, Habit, Profile, Reference, Reminder, Session, Task, WorkState, Workflow, WorkflowPlan, WorkspaceSnapshot } from './types';

const baseUrl = String(Constants.expoConfig?.extra?.apiUrl || '').replace(/\/$/, '');
let tokenProvider: () => Promise<string | null> = async () => null;
export const setTokenProvider = (provider: typeof tokenProvider) => { tokenProvider = provider; };

export class ApiError extends Error { constructor(public status: number, message: string) { super(message); } }
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await tokenProvider();
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { Accept: 'application/json', ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...init.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(response.status, body.detail || `Request failed (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
async function optional<T>(path: string, fallback: T): Promise<T> {
  try { return await request<T>(path); } catch (error) { if (error instanceof ApiError && error.status === 404) return fallback; throw error; }
}

export const api = {
  profile: () => request<Profile>('/api/me'),
  upsertProfile: () => request<Profile>('/api/me', { method: 'POST' }),
  acceptConsent: () => request('/api/me/consent', { method: 'POST' }),
  patchFocusPrefs: (body: Partial<FocusPrefs>) => request<FocusPrefs>('/api/me/focus', { method: 'PATCH', body: JSON.stringify(body) }),
  exportMyData: () => request<Record<string, unknown>>('/api/me/data/export'),
  deleteMyData: () => request<{ deleted: Record<string, number> }>('/api/me/data', { method: 'DELETE' }),
  tasks: () => request<Task[]>('/api/tasks'),
  createTask: (body: Partial<Task> & { task_name: string }) => request<Task>('/api/tasks', { method: 'POST', body: JSON.stringify(body) }),
  patchTask: (id: number, body: Partial<Task>) => request<Task>(`/api/tasks/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  deleteTask: (id: number) => request(`/api/tasks/${id}`, { method: 'DELETE' }),
  goals: () => request<Goal[]>('/api/goals'),
  createGoal: (body: { title: string; description?: string; metric?: string; target_value?: number; deadline?: string | null }) => request<Goal>('/api/goals', { method: 'POST', body: JSON.stringify(body) }),
  incrementGoal: (id: number, delta = 1) => request<Goal>(`/api/goals/${id}/increment`, { method: 'POST', body: JSON.stringify({ delta }) }),
  habits: () => request<Habit[]>('/api/habits'),
  createHabit: (name: string, cadence: 'DAILY' | 'WEEKLY' = 'DAILY') => request<Habit>('/api/habits', { method: 'POST', body: JSON.stringify({ name, cadence }) }),
  checkHabit: (id: number) => request<Habit>(`/api/habits/${id}/check`, { method: 'POST' }),
  workflows: () => request<Workflow[]>('/api/workflows'),
  generateWorkflow: (sop_text: string) => request<WorkflowPlan>('/api/workflows/generate', { method: 'POST', body: JSON.stringify({ sop_text }) }),
  createWorkflow: (plan: WorkflowPlan & { sop_text: string }) => request<Workflow>('/api/workflows', { method: 'POST', body: JSON.stringify({ ...plan, active: true }) }),
  runWorkflow: (id: number) => request<{ created: Task[] }>(`/api/workflows/${id}/run`, { method: 'POST' }),
  sessions: () => request<Session[]>('/api/sessions'),
  startSession: (description: string, duration_minutes: number, task_id?: number) => request<Session>('/api/sessions', { method: 'POST', body: JSON.stringify({ description, duration_minutes, task_id }) }),
  patchSession: (id: number, body: Partial<Session>) => request<Session>(`/api/sessions/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  reminders: () => request<Reminder[]>('/api/reminders'),
  ackReminder: (id: number) => request(`/api/reminders/${id}/ack`, { method: 'POST' }),
  calendarStatus: () => request<{ connected: boolean }>('/api/calendar/status'),
  calendarSync: () => request('/api/calendar/sync', { method: 'POST' }),
  schedule: () => request('/api/schedule', { method: 'POST' }),
  chat: (message: string, history: { role: string; text: string }[] = []) => request<{ chat_ui: { agent_message: string }; tasks: Task[] }>('/api/chat', { method: 'POST', body: JSON.stringify({ message, history }) }),
  workState: () => optional<WorkState | null>('/api/work-state', null),
  saveWorkState: (body: Partial<WorkState>) => request<WorkState>('/api/work-state', { method: 'PUT', body: JSON.stringify(body) }),
  activities: () => optional<Activity[]>('/api/activity', []),
  references: () => optional<Reference[]>('/api/references', []),
  devices: () => optional<DeviceRecord[]>('/api/devices', []),
  registerDevice: (body: Omit<DeviceRecord, 'last_seen'> & { push_token?: string | null }) => request<DeviceRecord>('/api/devices', { method: 'POST', body: JSON.stringify(body) }),
  heartbeat: (deviceId: string) => request<DeviceRecord>(`/api/devices/${encodeURIComponent(deviceId)}/heartbeat`, { method: 'POST' }),
  testPush: () => request<{ sent: number; failed: number }>('/api/devices/push/test', { method: 'POST' }),
  loadWorkspace: async (): Promise<WorkspaceSnapshot> => {
    const [tasks, goals, habits, sessions, workflows, reminders, activities, references, devices, workState] = await Promise.all([
      api.tasks(), api.goals(), api.habits(), api.sessions(), api.workflows(), api.reminders(), api.activities(), api.references(), api.devices(), api.workState(),
    ]);
    return { tasks, goals, habits, sessions, workflows, reminders, activities, references, devices, workState };
  },
};
