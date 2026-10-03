import { vi } from 'vitest';

// A stand-in for the Java backend, used only in tests. It answers the same URLs with the same
// JSON shapes, keeps jobs in memory, and records every request so tests can inspect it.

export const MODELS = [
  {
    id: 'euler-column-buckling',
    title: 'Euler column buckling (solid circular section)',
    outputName: 'criticalLoad',
    outputUnit: 'N',
    inputs: [
      { name: 'youngsModulus', unit: 'Pa', description: "Young's modulus E" },
      { name: 'diameter', unit: 'm', description: 'Outer diameter d' },
      { name: 'length', unit: 'm', description: 'Column length L' },
      { name: 'effectiveLengthFactor', unit: '-', description: 'Effective-length factor K' },
    ],
  },
  {
    id: 'spring-mass-natural-frequency',
    title: 'Spring-mass natural frequency',
    outputName: 'naturalFrequency',
    outputUnit: 'Hz',
    inputs: [
      { name: 'stiffness', unit: 'N/m', description: 'Spring stiffness k' },
      { name: 'mass', unit: 'kg', description: 'Mass m' },
    ],
  },
];

export function makeResult(overrides = {}) {
  return {
    modelId: 'euler-column-buckling',
    outputName: 'criticalLoad',
    outputUnit: 'N',
    samplesRequested: 10000,
    samplesUsed: 10000,
    samplesRejected: 0,
    seed: 2026,
    elapsedMillis: 41,
    summary: { mean: 153074.60387277894, stdDev: 20334.2548, standardError: 203.34, min: 94408.5, max: 237987.5 },
    percentiles: { p1: 115656, p5: 122979, p25: 137031, p50: 151479, p75: 167891, p95: 188156, p99: 200212 },
    histogram: { binEdges: [94408.5, 142198, 189988, 237987.5], counts: [900, 6200, 2900] },
    ...overrides,
  };
}

export function makeJob(n, overrides = {}) {
  const minute = String(n % 60).padStart(2, '0');
  return {
    id: `job-${n}`,
    modelId: 'euler-column-buckling',
    status: 'DONE',
    createdAt: `2026-10-03T08:${minute}:00Z`,
    startedAt: `2026-10-03T08:${minute}:01Z`,
    finishedAt: `2026-10-03T08:${minute}:02Z`,
    errorMessage: null,
    request: { modelId: 'euler-column-buckling', samples: 10000 },
    result: makeResult(),
    ...overrides,
  };
}

/** The token the fake server hands out; sessionFor() below builds the matching saved session. */
function tokenFor(username) {
  return { accessToken: `fake-token-${username}`, tokenType: 'Bearer', expiresInSeconds: 3600, username };
}

/** Puts a signed-in user into sessionStorage, as if they had logged in earlier. */
export function signInAs(username = 'alice') {
  sessionStorage.setItem('uqlab.session', JSON.stringify({ accessToken: `fake-token-${username}`, username }));
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

/**
 * Creates the fake server and installs it as the global fetch.
 * Options: jobs (jobs that already exist). Returns an object to inspect and steer it:
 *   backend.requests      every request received: { method, path, search, body }
 *   backend.down = true   makes every call fail like an unreachable server
 *   backend.rejectNext    set to { status, body } to make the next job submission fail
 *   backend.users         known accounts { username: password }
 *   backend.expireTokens  when true, protected calls answer 401
 *   backend.advance       when true, a job moves QUEUED -> RUNNING -> DONE each time it is fetched
 */
export function installFakeBackend({ jobs = [], users = { alice: 'password123' } } = {}) {
  const backend = {
    jobs: [...jobs],
    users: { ...users }, // username -> password
    requests: [],
    down: false,
    rejectNext: null,
    advance: true,
    counter: 1000,
    expireTokens: false, // when true, every token is refused with 401, like an expired one
  };

  globalThis.fetch = vi.fn(async (url, options = {}) => {
    if (backend.down) throw new TypeError('Failed to fetch');
    const u = new URL(url, 'http://localhost');
    const method = options.method || 'GET';
    backend.requests.push({
      method,
      path: u.pathname,
      search: u.search,
      body: options.body ? JSON.parse(options.body) : null,
      authorization: options.headers?.Authorization ?? null,
    });

    // --- accounts (open routes) ---
    if (method === 'POST' && u.pathname === '/api/auth/register') {
      const { username, password } = JSON.parse(options.body);
      const name = username.trim().toLowerCase();
      if (backend.users[name] !== undefined)
        return json({ title: 'Username already taken', detail: `The username '${name}' is already taken` }, 409);
      backend.users[name] = password;
      return json(tokenFor(name), 201);
    }
    if (method === 'POST' && u.pathname === '/api/auth/login') {
      const { username, password } = JSON.parse(options.body);
      const name = username.trim().toLowerCase();
      if (backend.users[name] === undefined || backend.users[name] !== password)
        return json({ title: 'Sign-in failed', detail: 'Invalid username or password' }, 401);
      return json(tokenFor(name), 200);
    }

    // --- everything else needs a valid token ---
    const bearer = (options.headers?.Authorization ?? '').match(/^Bearer fake-token-(.+)$/);
    if (backend.expireTokens || !bearer || backend.users[bearer[1]] === undefined)
      return json({ title: 'Unauthorized', detail: 'Authentication is required' }, 401);

    if (method === 'GET' && u.pathname === '/api/auth/me')
      return json({ id: `id-${bearer[1]}`, username: bearer[1] });

    if (method === 'GET' && u.pathname === '/api/models') return json(MODELS);

    if (method === 'POST' && u.pathname === '/api/jobs') {
      if (backend.rejectNext) {
        const { status, body } = backend.rejectNext;
        backend.rejectNext = null;
        return json(body, status);
      }
      const job = makeJob(backend.counter++, {
        status: 'QUEUED',
        startedAt: null,
        finishedAt: null,
        result: null,
        request: JSON.parse(options.body),
        createdAt: new Date().toISOString(),
      });
      backend.jobs.unshift(job);
      return json(job, 202);
    }

    if (method === 'GET' && u.pathname === '/api/jobs') {
      const status = u.searchParams.get('status');
      const page = Number(u.searchParams.get('page') ?? 0);
      const size = Number(u.searchParams.get('size') ?? 20);
      const matching = backend.jobs.filter((j) => !status || j.status === status);
      return json({
        items: matching.slice(page * size, page * size + size),
        page,
        size,
        totalItems: matching.length,
        totalPages: Math.ceil(matching.length / size),
      });
    }

    const match = u.pathname.match(/^\/api\/jobs\/(.+)$/);
    if (method === 'GET' && match) {
      const job = backend.jobs.find((j) => j.id === match[1]);
      if (!job) return json({ title: 'Job not found', detail: `No job with id '${match[1]}'` }, 404);
      const snapshot = { ...job };
      if (backend.advance && job.status === 'QUEUED') job.status = 'RUNNING';
      else if (backend.advance && job.status === 'RUNNING') {
        job.status = 'DONE';
        job.result = makeResult();
        job.startedAt = job.createdAt;
        job.finishedAt = job.createdAt;
      }
      return json(snapshot);
    }

    return json({ title: 'Not found', detail: 'unknown test route' }, 404);
  });

  return backend;
}
