import { useState } from 'react';
import { getJob } from '../api/client.js';
import { usePolled } from '../hooks/usePolled.js';
import { PERCENTILE_KEYS, formatDateTime, formatDuration, formatNumber, isActive } from '../lib/format.js';
import Histogram from './Histogram.jsx';
import StatusBadge from './StatusBadge.jsx';

function Stat({ label, value, unit, note }) {
  return (
    <div className="stat">
      <div className="stat-label">{label}</div>
      <div className="stat-value">
        {value} {unit && <span className="stat-unit">{unit}</span>}
      </div>
      {note && <div className="hint">{note}</div>}
    </div>
  );
}

function CopyButton({ text }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard access can be refused by the browser; the seed stays visible to copy by hand.
    }
  }
  return (
    <button type="button" className="link-button" onClick={copy}>
      {copied ? 'Copied' : 'Copy seed'}
    </button>
  );
}

/** Shows one job: its status, then (when finished) the statistics and the histogram. */
export default function JobDetail({ jobId }) {
  const { data: job, error, loading } = usePolled(() => getJob(jobId), [jobId], (j) => isActive(j.status));

  if (loading && !job) return <section className="card"><p>Loading job...</p></section>;
  if (error && !job) {
    return (
      <section className="card">
        <div className="notice notice-error" role="alert">{error.message}</div>
      </section>
    );
  }

  const result = job.result;
  return (
    <section className="card" aria-label="Job result">
      <div className="detail-header">
        <h2>Result</h2>
        <StatusBadge status={job.status} />
      </div>
      <p className="hint">
        Job {job.id} · {job.modelId} · submitted {formatDateTime(job.createdAt)}
        {job.finishedAt && ` · took ${formatDuration(job.startedAt, job.finishedAt)}`}
      </p>

      {isActive(job.status) && (
        <p className="notice" role="status">
          {job.status === 'QUEUED' ? 'Waiting for a free worker...' : 'Running the simulation...'} This page
          updates by itself.
        </p>
      )}

      {job.status === 'FAILED' && (
        <div className="notice notice-error" role="alert">
          The simulation failed: {job.errorMessage}
        </div>
      )}

      {result && (
        <>
          <div className="stats">
            <Stat
              label="Mean"
              value={formatNumber(result.summary.mean)}
              unit={result.outputUnit}
              note={`± ${formatNumber(result.summary.standardError)} (standard error)`}
            />
            <Stat label="Standard deviation" value={formatNumber(result.summary.stdDev)} unit={result.outputUnit} />
            <Stat label="Minimum" value={formatNumber(result.summary.min)} unit={result.outputUnit} />
            <Stat label="Maximum" value={formatNumber(result.summary.max)} unit={result.outputUnit} />
          </div>

          <h3>Distribution of {result.outputName}</h3>
          <Histogram
            edges={result.histogram.binEdges}
            counts={result.histogram.counts}
            unit={result.outputUnit}
            label={result.outputName}
            mean={result.summary.mean}
          />

          <h3>Percentiles</h3>
          <dl className="percentiles">
            {PERCENTILE_KEYS.map((key) => (
              <div key={key}>
                <dt>{key.replace('p', '')}%</dt>
                <dd>{formatNumber(result.percentiles[key])}</dd>
              </div>
            ))}
          </dl>
          <p className="hint">
            Read "5%: x" as: in 5% of the trials the {result.outputName} was below x {result.outputUnit}.
          </p>

          <h3>Run details</h3>
          <p>
            {result.samplesUsed.toLocaleString('en-US')} samples used,{' '}
            {result.samplesRejected.toLocaleString('en-US')} rejected as non-physical · computed in{' '}
            {result.elapsedMillis} ms · seed <code>{result.seed}</code> <CopyButton text={String(result.seed)} />
          </p>
        </>
      )}

      <details>
        <summary>Show the request that was sent</summary>
        <pre>{JSON.stringify(job.request, (key, value) => (value === null ? undefined : value), 2)}</pre>
      </details>
    </section>
  );
}
