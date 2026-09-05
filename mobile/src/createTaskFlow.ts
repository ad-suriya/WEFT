import type { Task, WorkflowPlan, WorkState, Workflow } from './types';
import { buildTaskPayload, type TaskDraft } from './workTasks';
import { resolveWorkflowLink, stepNameForLink } from './workflowProgress';

export interface TaskFlowApi {
  createTask: (body: { task_name: string; next_micro_step: string; deadline: string | null }) => Promise<Task>;
  patchTask: (id: number, body: Partial<Task>) => Promise<Task>;
  startSession: (description: string, duration_minutes: number, task_id?: number) => Promise<{ id: number; start_time: string }>;
  generateWorkflow: (sop_text: string) => Promise<WorkflowPlan>;
  createWorkflow: (plan: WorkflowPlan & { sop_text: string }) => Promise<{ id: number }>;
  runWorkflow: (id: number) => Promise<unknown>;
}

export interface TaskFlowDeps {
  api: TaskFlowApi;
  saveWorkState: (patch: Partial<WorkState>) => Promise<unknown>;
}

/**
 * Quick text task creation, using the existing `/api/tasks` endpoint. When `startImmediately`
 * is set, also flips the task to IN_PROGRESS, opens a session, and updates shared work state —
 * the same three backend calls the Focus screen's own "Start work" already makes.
 */
export async function createTaskAndMaybeStart(deps: TaskFlowDeps, draft: TaskDraft, startImmediately: boolean, workflows: Workflow[] = [], priorWorkState: WorkState | null = null): Promise<Task> {
  const created = await deps.api.createTask(buildTaskPayload(draft));
  if (startImmediately) {
    await deps.api.patchTask(created.id, { status: 'IN_PROGRESS' });
    const session = await deps.api.startSession(created.task_name, created.estimated_minutes || 25, created.id);
    // A brand-new task never matches the workflow the shared state was previously tracking — resolve
    // its link fresh (same rule Focus's own start() uses) so a stale workflow_id from whatever task
    // was last active can't silently attach itself to this one.
    const link = resolveWorkflowLink(workflows, priorWorkState, created.id, created.task_name);
    const stepName = stepNameForLink(workflows, link, created.task_name);
    await deps.saveWorkState({
      task_id: created.id,
      workflow_id: link.workflow_id,
      current_step_index: link.current_step_index,
      current_step: stepName,
      next_action: created.next_micro_step || 'Continue',
      last_activity: 'Started work',
      status: 'ACTIVE',
      active_session_id: session.id,
      started_at: session.start_time,
    });
  }
  return created;
}

/** Asks the existing AI workflow-generation endpoint to turn free text into a step-by-step plan. */
export function generateTaskWorkflow(deps: Pick<TaskFlowDeps, 'api'>, sopText: string): Promise<WorkflowPlan> {
  return deps.api.generateWorkflow(sopText.trim());
}

/** Persists a generated plan as a real workflow and immediately runs it, turning its steps into tasks. */
export async function saveTaskWorkflow(deps: Pick<TaskFlowDeps, 'api'>, plan: WorkflowPlan, sopText: string): Promise<void> {
  const workflow = await deps.api.createWorkflow({ ...plan, sop_text: sopText.trim() });
  await deps.api.runWorkflow(workflow.id);
}
