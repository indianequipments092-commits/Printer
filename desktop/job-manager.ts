export type DesktopJobKind = 'scan' | 'print';
export type DesktopJobState = 'queued' | 'preparing' | 'running' | 'completed' | 'failed' | 'cancelled';

export interface DesktopJob<TPayload = unknown, TResult = unknown> {
  id: string;
  kind: DesktopJobKind;
  state: DesktopJobState;
  payload: TPayload;
  result?: TResult;
  error?: string;
  progress: number;
  createdAt: string;
  updatedAt: string;
}

const TERMINAL: ReadonlySet<DesktopJobState> = new Set(['completed', 'failed', 'cancelled']);
const ALLOWED: Record<DesktopJobState, ReadonlySet<DesktopJobState>> = {
  queued: new Set(['preparing', 'cancelled', 'failed']),
  preparing: new Set(['running', 'cancelled', 'failed']),
  running: new Set(['completed', 'cancelled', 'failed']),
  completed: new Set(),
  failed: new Set(),
  cancelled: new Set(),
};

export class DesktopJobManager<TPayload = unknown, TResult = unknown> {
  private readonly jobs = new Map<string, DesktopJob<TPayload, TResult>>();

  create(kind: DesktopJobKind, payload: TPayload): DesktopJob<TPayload, TResult> {
    const now = new Date().toISOString();
    const job: DesktopJob<TPayload, TResult> = {
      id: globalThis.crypto?.randomUUID?.() ?? `job-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      kind,
      state: 'queued',
      payload,
      progress: 0,
      createdAt: now,
      updatedAt: now,
    };
    this.jobs.set(job.id, job);
    return { ...job };
  }

  get(id: string): DesktopJob<TPayload, TResult> | undefined {
    const job = this.jobs.get(id);
    return job ? { ...job } : undefined;
  }

  list(): DesktopJob<TPayload, TResult>[] {
    return [...this.jobs.values()].map((job) => ({ ...job }));
  }

  transition(id: string, next: DesktopJobState, patch: Partial<Pick<DesktopJob<TPayload, TResult>, 'result' | 'error' | 'progress'>> = {}): DesktopJob<TPayload, TResult> {
    const job = this.jobs.get(id);
    if (!job) throw new Error(`Unknown job: ${id}`);
    if (TERMINAL.has(job.state)) throw new Error(`Job ${id} is already terminal (${job.state})`);
    if (!ALLOWED[job.state].has(next)) throw new Error(`Invalid job transition: ${job.state} -> ${next}`);

    const updated: DesktopJob<TPayload, TResult> = {
      ...job,
      ...patch,
      state: next,
      progress: Math.max(0, Math.min(100, patch.progress ?? job.progress)),
      updatedAt: new Date().toISOString(),
    };
    this.jobs.set(id, updated);
    return { ...updated };
  }

  cancel(id: string): DesktopJob<TPayload, TResult> {
    return this.transition(id, 'cancelled', { progress: this.jobs.get(id)?.progress ?? 0 });
  }
}
