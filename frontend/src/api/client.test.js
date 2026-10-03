import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, getJob, listJobs, submitJob } from './client.js';

function respond(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

beforeEach(() => {
  globalThis.fetch = vi.fn();
});

describe('api client', () => {
  it('posts a job as JSON', async () => {
    fetch.mockResolvedValue(respond({ id: 'abc', status: 'QUEUED' }, 202));
    const job = await submitJob({ modelId: 'm', samples: 100 });
    expect(job.id).toBe('abc');
    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe('/api/jobs');
    expect(options.method).toBe('POST');
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(options.body)).toEqual({ modelId: 'm', samples: 100 });
  });

  it('builds the list URL with paging and an optional status', async () => {
    fetch.mockImplementation(async () => respond({ items: [] }));
    await listJobs({ status: 'DONE', page: 2, size: 5 });
    expect(fetch.mock.calls[0][0]).toBe('/api/jobs?page=2&size=5&status=DONE');
    await listJobs({ page: 0, size: 8 });
    expect(fetch.mock.calls[1][0]).toBe('/api/jobs?page=0&size=8');
  });

  it('escapes the id in the job URL', async () => {
    fetch.mockResolvedValue(respond({}));
    await getJob('a/b c');
    expect(fetch.mock.calls[0][0]).toBe('/api/jobs/a%2Fb%20c');
  });

  it('turns a problem-details reply into an ApiError with field messages', async () => {
    fetch.mockResolvedValue(
      respond({ title: 'Invalid request', detail: 'Request validation failed', errors: { samples: 'too small' } }, 400),
    );
    const error = await submitJob({}).catch((e) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.message).toBe('Request validation failed');
    expect(error.errors).toEqual({ samples: 'too small' });
  });

  it('reports an unreachable server as status 0', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'));
    const error = await getJob('x').catch((e) => e);
    expect(error.status).toBe(0);
    expect(error.message).toMatch(/Cannot reach the server/);
  });

  it('copes with an error reply that is not JSON', async () => {
    fetch.mockResolvedValue(new Response('Bad gateway', { status: 502 }));
    const error = await getJob('x').catch((e) => e);
    expect(error.status).toBe(502);
    expect(error.message).toBe('Request failed with status 502');
  });
});
