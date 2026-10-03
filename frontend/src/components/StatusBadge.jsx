const LABELS = { QUEUED: 'Queued', RUNNING: 'Running', DONE: 'Done', FAILED: 'Failed' };

/** A coloured pill with the job status in words, so colour is never the only signal. */
export default function StatusBadge({ status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status] ?? status}</span>;
}
