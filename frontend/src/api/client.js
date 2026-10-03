// All communication with the Java backend lives in this one file.
// Every function returns the parsed JSON, or throws an ApiError.

// Empty in development (the Vite proxy forwards /api). Set VITE_API_BASE to call another server.
const BASE = import.meta.env.VITE_API_BASE ?? '';

/** An error from the backend (status 400, 404, ...) or status 0 when the server cannot be reached. */
export class ApiError extends Error {
  constructor({ status, title, detail, errors }) {
    super(detail || title || 'Request failed');
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    // Field-by-field messages from a validation failure, e.g. { samples: 'must be >= 100' }.
    this.errors = errors || {};
  }
}

async function call(path, options) {
  let response;
  try {
    response = await fetch(BASE + path, options);
  } catch {
    throw new ApiError({
      status: 0,
      title: 'Server unreachable',
      detail: 'Cannot reach the server. Check that the backend is running on port 8080.',
    });
  }

  const isJson = (response.headers.get('content-type') || '').includes('json');
  const body = isJson ? await response.json() : null;

  if (!response.ok) {
    throw new ApiError({
      status: response.status,
      title: body?.title,
      detail: body?.detail || `Request failed with status ${response.status}`,
      errors: body?.errors,
    });
  }
  return body;
}

export const getModels = () => call('/api/models');

export const getJob = (id) => call(`/api/jobs/${encodeURIComponent(id)}`);

export const submitJob = (request) =>
  call('/api/jobs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

/** Lists jobs, newest first. status may be '' for all jobs. */
export function listJobs({ status = '', page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set('status', status);
  return call(`/api/jobs?${params}`);
}
