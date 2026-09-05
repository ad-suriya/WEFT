import type { Task, WorkState, Workflow } from './types';

export type StepState = 'done' | 'current' | 'upcoming';

export interface WorkflowStepView {
  index: number;
  taskName: string;
  state: StepState;
  dependsOn: string[];
}

export interface WorkflowProgressView {
  workflow: Workflow;
  stepIndex: number;
  total: number;
  progressLabel: string;
  steps: WorkflowStepView[];
  currentStepName: string | null;
  nextStepName: string | null;
  isComplete: boolean;
}

export interface AdvanceStepPatch {
  workflow_id: number;
  current_step_index: number;
  current_step: string;
  next_action: string;
  last_activity: string;
  status: WorkState['status'];
}

/** The shared work state only names a workflow while it's tracking work on that exact task. */
export function findActiveWorkflow(workflows: Workflow[], workState: WorkState | null, taskId: number | undefined | null): Workflow | null {
  if (!workState?.workflow_id || workState.task_id !== taskId) return null;
  return workflows.find(w => w.id === workState.workflow_id) || null;
}

/** Best-effort match from a task's name to the workflow step it came from — the only link available, since tasks don't carry a workflow_id of their own. */
export function matchWorkflowStep(workflows: Workflow[], taskName: string): { workflowId: number; stepIndex: number } | null {
  for (const workflow of workflows) {
    const stepIndex = workflow.steps.findIndex(step => step.task_name === taskName);
    if (stepIndex !== -1) return { workflowId: workflow.id, stepIndex };
  }
  return null;
}

/**
 * The workflow_id/current_step_index a session should start with for `taskId`/`taskName`.
 * If the shared work state is already tracking this exact task, its existing step position is kept as-is.
 * Otherwise (a different task, or no state yet) it is resolved fresh from the task's name — never inherited
 * from whatever task the shared state previously pointed at, which would silently attach the wrong workflow.
 */
export function resolveWorkflowLink(workflows: Workflow[], workState: WorkState | null, taskId: number, taskName: string): { workflow_id: number | null; current_step_index: number } {
  if (workState?.task_id === taskId && workState.workflow_id != null) {
    return { workflow_id: workState.workflow_id, current_step_index: workState.current_step_index };
  }
  const matched = matchWorkflowStep(workflows, taskName);
  return matched ? { workflow_id: matched.workflowId, current_step_index: matched.stepIndex } : { workflow_id: null, current_step_index: 0 };
}

/** The real step text for a resolved workflow link, so `current_step` reflects the actual step (e.g. "CNF Conversion") instead of being overwritten with the task's own name. */
export function stepNameForLink(workflows: Workflow[], link: { workflow_id: number | null; current_step_index: number }, fallback: string): string {
  const workflow = link.workflow_id != null ? workflows.find(w => w.id === link.workflow_id) : null;
  return workflow?.steps[link.current_step_index]?.task_name || fallback;
}

/** Best-effort link from a workflow step to the real task it was run into (matched by name — the only link there is). */
export function taskForStepName(tasks: Task[], stepName: string | null): Task | null {
  if (!stepName) return null;
  return tasks.find(t => t.task_name === stepName) || null;
}

function dependencyNames(stepTaskName: string, tasks: Task[]): string[] {
  const match = taskForStepName(tasks, stepTaskName);
  if (!match?.dependencies?.length) return [];
  return match.dependencies
    .map(id => tasks.find(t => t.id === id)?.task_name)
    .filter((name): name is string => !!name);
}

/** Pure view-model for a workflow's step checklist, derived entirely from the shared work state's current_step_index. */
export function buildWorkflowProgress(workflow: Workflow, workState: WorkState | null, tasks: Task[]): WorkflowProgressView {
  const total = workflow.steps.length;
  const rawIndex = workState?.current_step_index ?? 0;
  const stepIndex = Math.min(Math.max(rawIndex, 0), total);
  const isComplete = total > 0 && stepIndex >= total;
  const steps: WorkflowStepView[] = workflow.steps.map((step, index) => ({
    index,
    taskName: step.task_name,
    state: index < stepIndex ? 'done' : index === stepIndex ? 'current' : 'upcoming',
    dependsOn: dependencyNames(step.task_name, tasks),
  }));
  const progressLabel = `${isComplete ? total : Math.min(stepIndex + 1, total)}/${total}`;
  const currentStepName = isComplete ? null : workflow.steps[stepIndex]?.task_name ?? null;
  const nextStepName = !isComplete && stepIndex + 1 < total ? workflow.steps[stepIndex + 1].task_name : null;
  return { workflow, stepIndex, total, progressLabel, steps, currentStepName, nextStepName, isComplete };
}

/** The shared-work-state patch that advances a workflow by one step (or marks it complete on the last step). Returns null once already complete. */
export function buildAdvanceStepPatch(progress: WorkflowProgressView): AdvanceStepPatch | null {
  if (progress.isComplete || !progress.currentStepName) return null;
  const completedStepName = progress.currentStepName;
  const nextIndex = progress.stepIndex + 1;
  const willBeComplete = nextIndex >= progress.total;
  return {
    workflow_id: progress.workflow.id,
    current_step_index: nextIndex,
    current_step: willBeComplete ? completedStepName : progress.workflow.steps[nextIndex].task_name,
    next_action: willBeComplete ? 'Workflow complete' : progress.workflow.steps[nextIndex + 1]?.task_name ? `Then: ${progress.workflow.steps[nextIndex + 1].task_name}` : `Finish "${progress.workflow.steps[nextIndex].task_name}"`,
    last_activity: `Completed ${completedStepName}`,
    status: willBeComplete ? 'IDLE' : 'ACTIVE',
  };
}
