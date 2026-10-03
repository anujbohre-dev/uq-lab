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

// The current login token. App sets it after sign-in and clears it on log out.
let authToken = null;
// Called when the server answers 401 to a request that carried a token (the token has expired).
let unauthorizedHandler = null;

export function setAuthToken(token) {
  authToken = token || null;
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

async function call(path, options = {}) {
  const headers = { ...options.headers };
  if (authToken) headers.Authorization = `Bearer ${authToken}`;
  let response;
  try {
    response = await fetch(BASE + path, { ...options, headers });
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
    // A token was sent but refused: it has expired or is invalid. Sign the user out.
    if (response.status === 401 && authToken && unauthorizedHandler) unauthorizedHandler();
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

/** Creates an account and signs in. Resolves to { accessToken, tokenType, expiresInSeconds, username }. */
export const register = (username, password) =>
  call('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

/** Signs in. Same result shape as register. Throws ApiError with status 401 for bad credentials. */
export const login = (username, password) =>
  call('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

/** The signed-in user, as the server sees the token. */
export const getMe = () => call('/api/auth/me');
