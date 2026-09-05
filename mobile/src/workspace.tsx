import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react';
import { AppState } from 'react-native';
import * as Device from 'expo-device';
import * as Application from 'expo-application';
import { getNativePushToken } from './notifications';
import { api, ApiError } from './api';
import { storage } from './storage';
import { useAuth } from './auth';
import type { Session, Task, WorkState, WorkspaceSnapshot } from './types';

const empty: WorkspaceSnapshot = { tasks: [], goals: [], habits: [], sessions: [], workflows: [], reminders: [], activities: [], references: [], devices: [], workState: null };
type WorkspaceValue = { data: WorkspaceSnapshot; loading: boolean; refreshing: boolean; error: string | null; refresh: () => Promise<void>; mutate: (fn: (data: WorkspaceSnapshot) => WorkspaceSnapshot) => void; saveWorkState: (patch: Partial<WorkState>) => Promise<WorkState>; registerDevice: () => Promise<void>; activeSession: Session | null; currentTask: Task | null; };
const WorkspaceContext = createContext<WorkspaceValue | null>(null);

export function deriveCurrentTask(tasks: Task[], workState: WorkState | null): Task | null {
  if (workState?.task_id) return tasks.find(t => t.id === workState.task_id) || null;
  return tasks.find(t => t.status === 'IN_PROGRESS') || tasks.filter(t => !['COMPLETED', 'ARCHIVED'].includes(t.status)).sort((a, b) => (a.scheduled_start || 'z').localeCompare(b.scheduled_start || 'z'))[0] || null;
}
export function deriveWorkState(tasks: Task[], sessions: Session[], remote: WorkState | null, local: WorkState | null): WorkState | null {
  if (remote) return remote;
  if (local) return local;
  const task = deriveCurrentTask(tasks, null); if (!task) return null;
  const session = sessions.filter(s => !s.end_time).sort((a, b) => b.id - a.id)[0];
  return { task_id: task.id, workflow_id: null, current_step_index: 0, current_step: task.task_name, last_activity: task.status === 'IN_PROGRESS' ? 'Work in progress' : 'Task ready', next_action: task.next_micro_step || 'Continue this task', status: session?.is_paused ? 'PAUSED' : session ? 'ACTIVE' : 'IDLE', active_session_id: session?.id || null, started_at: session?.start_time || null, updated_at: task.updated_at };
}

export function WorkspaceProvider({ children }: PropsWithChildren) {
  const { profile, signOut } = useAuth(); const [data, setData] = useState(empty); const [loading, setLoading] = useState(true); const [refreshing, setRefreshing] = useState(false); const [error, setError] = useState<string | null>(null); const mounted = useRef(true);
  const refresh = useCallback(async () => { if (!profile) return; setRefreshing(true); try { const [fresh, local] = await Promise.all([api.loadWorkspace(), storage.workState()]); fresh.workState = deriveWorkState(fresh.tasks, fresh.sessions, fresh.workState, local); if (fresh.workState) await storage.saveWorkState(fresh.workState); if (mounted.current) setData(fresh); await storage.saveSnapshot(fresh); setError(null); } catch (e) { if (e instanceof ApiError && e.status === 401) await signOut(); const cached = await storage.snapshot(); if (cached && mounted.current) setData(cached); setError(e instanceof Error ? e.message : 'Could not sync'); } finally { if (mounted.current) { setLoading(false); setRefreshing(false); } } }, [profile, signOut]);
  const registerDevice = useCallback(async () => { if (!profile) return; const deviceId = await storage.deviceId(); const pushToken = await getNativePushToken(); try { await api.registerDevice({ device_id: deviceId, name: Device.deviceName || Application.applicationName || 'Android phone', device_type: 'android', capabilities: ['push', 'focus_bridge', 'resume', 'share_reference'], push_token: pushToken }); } catch (e) { if (!(e instanceof ApiError && e.status === 404)) throw e; } }, [profile]);
  useEffect(() => { mounted.current = true; storage.snapshot().then(x => x && setData(x)); refresh(); registerDevice(); const interval = setInterval(() => { if (AppState.currentState === 'active') { refresh(); storage.deviceId().then(id => api.heartbeat(id).catch(() => undefined)); } }, 10000); const subscription = AppState.addEventListener('change', state => state === 'active' && refresh()); return () => { mounted.current = false; clearInterval(interval); subscription.remove(); }; }, [refresh, registerDevice]);
  const saveWorkState = useCallback(async (patch: Partial<WorkState>) => { const previous = data.workState; const next: WorkState = { task_id: null, workflow_id: null, current_step_index: 0, current_step: '', last_activity: '', next_action: '', status: 'IDLE', active_session_id: null, started_at: null, updated_at: new Date().toISOString(), ...previous, ...patch }; const device_id = await storage.deviceId(); const local = { ...next, device_id, updated_at: new Date().toISOString() }; setData(d => ({ ...d, workState: local })); await storage.saveWorkState(local); try { const saved = await api.saveWorkState(local); setData(d => ({ ...d, workState: saved })); await storage.saveWorkState(saved); return saved; } catch (e) { if (e instanceof ApiError && (e.status === 404 || e.status === 405)) return local; throw e; } }, [data.workState]);
  const value = useMemo<WorkspaceValue>(() => ({ data, loading, refreshing, error, refresh, mutate: setData, saveWorkState, registerDevice, activeSession: data.sessions.filter(s => !s.end_time).sort((a, b) => b.id - a.id)[0] || null, currentTask: deriveCurrentTask(data.tasks, data.workState) }), [data, loading, refreshing, error, refresh, saveWorkState, registerDevice]);
  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}
export function useWorkspace() { const value = useContext(WorkspaceContext); if (!value) throw new Error('WorkspaceProvider missing'); return value; }
