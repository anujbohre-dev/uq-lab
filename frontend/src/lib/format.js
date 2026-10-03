// Small helpers that turn raw values into text for the screen.

const numberFormat = new Intl.NumberFormat('en-US', { maximumSignificantDigits: 6 });

/** 153074.60387 -> "153,075"; 0.0054324887 -> "0.00543249". Six significant digits. */
export function formatNumber(value) {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  return numberFormat.format(value);
}

/** "2026-10-03T02:48:03Z" -> a date and time in the viewer's own time zone. */
export function formatDateTime(iso) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'medium' });
}

/** Milliseconds between two ISO timestamps, as "0.4 s", or "-" if either is missing. */
export function formatDuration(startIso, endIso) {
  if (!startIso || !endIso) return '-';
  const ms = new Date(endIso) - new Date(startIso);
  return ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1)} s`;
}

export const STATUSES = ['QUEUED', 'RUNNING', 'DONE', 'FAILED'];

/** A job still moving through the queue: the screen keeps asking for news about it. */
export const isActive = (status) => status === 'QUEUED' || status === 'RUNNING';

export const PERCENTILE_KEYS = ['p1', 'p5', 'p25', 'p50', 'p75', 'p95', 'p99'];
