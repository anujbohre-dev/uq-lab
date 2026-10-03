import { useEffect, useState } from 'react';
import { listJobs } from '../api/client.js';
import { usePolled } from '../hooks/usePolled.js';
import { STATUSES, formatDateTime, formatNumber, isActive } from '../lib/format.js';
import StatusBadge from './StatusBadge.jsx';

const PAGE_SIZE = 8;

/**
 * The list of jobs, newest first, with a status filter and page buttons.
 * It reloads itself every two seconds while any job on the page is still active.
 *
 * Props: models (to show titles), selectedId, onSelect(id), refreshKey (changes when a job is submitted)
 */
export default function JobsPanel({ models, selectedId, onSelect, refreshKey }) {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  // A newly submitted job is the newest, so go back to the first page to show it.
  useEffect(() => setPage(0), [refreshKey]);

  const { data, error, loading } = usePolled(
    () => listJobs({ status, page, size: PAGE_SIZE }),
    [status, page, refreshKey],
    (d) => d.items.some((job) => isActive(job.status)),
  );

  const titleOf = (modelId) => models.find((m) => m.id === modelId)?.title ?? modelId;

  return (
    <section className="card" aria-label="Jobs">
      <div className="detail-header">
        <h2>Jobs</h2>
        <label className="inline">
          <span>Show</span>
          <select
            aria-label="Filter by status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All jobs</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && (
        <div className="notice notice-error" role="alert">
          {error.message}
          {error.status === 0 && ' Retrying...'}
        </div>
      )}
      {loading && !data && <p>Loading jobs...</p>}

      {data && data.items.length === 0 && (
        <p className="empty">
          {status ? 'No jobs with this status.' : 'No jobs yet. Fill in the form and press "Run simulation".'}
        </p>
      )}

      {data && data.items.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th>Submitted</th>
                <th>Model</th>
                <th>Status</th>
                <th className="num">Mean</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((job) => (
                <tr key={job.id} className={job.id === selectedId ? 'selected' : undefined}>
                  <td>
                    <button type="button" className="link-button" onClick={() => onSelect(job.id)}>
                      {formatDateTime(job.createdAt)}
                    </button>
                  </td>
                  <td>{titleOf(job.modelId)}</td>
                  <td>
                    <StatusBadge status={job.status} />
                  </td>
                  <td className="num">
                    {job.result ? `${formatNumber(job.result.summary.mean)} ${job.result.outputUnit}` : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pager">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span>
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalItems} jobs
            </span>
            <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        </>
      )}
    </section>
  );
}
