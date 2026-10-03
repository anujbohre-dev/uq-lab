import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App.jsx';
import { installFakeBackend, makeJob, makeResult } from './test/fakeBackend.js';

const posts = (backend) => backend.requests.filter((r) => r.method === 'POST');

describe('App', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads the models and fills the form with example values', async () => {
    installFakeBackend();
    render(<App />);

    expect(await screen.findByRole('heading', { name: 'New simulation' })).toBeInTheDocument();
    expect(screen.getByLabelText('diameter Mean')).toHaveValue('0.05');
    expect(screen.getByLabelText('youngsModulus Mean')).toHaveValue('2e11');
    expect(screen.getByLabelText('effectiveLengthFactor Min')).toHaveValue('0.9');
    expect(screen.getByLabelText('Samples')).toHaveValue('10000');
    expect(await screen.findByText(/No jobs yet/)).toBeInTheDocument();
    expect(screen.getByText('Select a job to see its result.')).toBeInTheDocument();
  });

  it('shows different inputs when another model is chosen', async () => {
    installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByLabelText('diameter Mean');

    await user.selectOptions(screen.getByLabelText('Model'), 'spring-mass-natural-frequency');

    expect(screen.getByLabelText('stiffness Mean')).toHaveValue('1000');
    expect(screen.getByLabelText('mass Min')).toHaveValue('0.95');
    expect(screen.queryByLabelText('diameter Mean')).not.toBeInTheDocument();
  });

  it('changes the number fields when the distribution type changes', async () => {
    installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByLabelText('length Mean');

    await user.selectOptions(screen.getByLabelText('length distribution'), 'CONSTANT');

    expect(screen.queryByLabelText('length Mean')).not.toBeInTheDocument();
    expect(screen.getByLabelText('length Value')).toBeInTheDocument();
  });

  it('sends the right request, then shows the job and, once it finishes, its result', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const backend = installFakeBackend();
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    render(<App />);
    await screen.findByLabelText('diameter Mean');

    await user.click(screen.getByRole('button', { name: 'Run simulation' }));

    await waitFor(() => expect(posts(backend)).toHaveLength(1));
    expect(posts(backend)[0].body).toEqual({
      modelId: 'euler-column-buckling',
      samples: 10000,
      bins: 20,
      inputs: {
        youngsModulus: { type: 'LOGNORMAL', mean: 2e11, stdDev: 1e10 },
        diameter: { type: 'NORMAL', mean: 0.05, stdDev: 0.0005 },
        length: { type: 'NORMAL', mean: 2, stdDev: 0.01 },
        effectiveLengthFactor: { type: 'UNIFORM', min: 0.9, max: 1.1 },
      },
    });

    // The new job is selected and shown as waiting.
    const detail = await screen.findByRole('region', { name: 'Job result' });
    expect(within(detail).getByText('Queued')).toBeInTheDocument();
    expect(within(detail).getByText(/This page updates by itself/)).toBeInTheDocument();

    // Two polling rounds later it has run and finished, with no click from the user.
    await act(() => vi.advanceTimersByTimeAsync(2100));
    await act(() => vi.advanceTimersByTimeAsync(2100));
    await waitFor(() => expect(within(screen.getByRole('region', { name: 'Job result' })).getByText('Done')).toBeInTheDocument());
    const done = screen.getByRole('region', { name: 'Job result' });
    expect(within(done).getByText('153,075', { exact: false })).toBeInTheDocument();
    expect(within(done).getByRole('img')).toHaveAccessibleName(/Histogram of criticalLoad/);
    expect(within(done).getByText('2026', { selector: 'code' })).toBeInTheDocument();
    // The jobs table row shows the same mean.
    expect(within(screen.getByRole('region', { name: 'Jobs' })).getByText('153,075 N')).toBeInTheDocument();
  });

  it('checks the values before sending and marks the wrong fields', async () => {
    const backend = installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByLabelText('diameter Mean');

    await user.clear(screen.getByLabelText('Samples'));
    await user.type(screen.getByLabelText('Samples'), '50');
    await user.clear(screen.getByLabelText('diameter Mean'));
    await user.click(screen.getByRole('button', { name: 'Run simulation' }));

    expect(await screen.findByText('Must be between 100 and 1000000')).toBeInTheDocument();
    expect(screen.getByText('Enter a number')).toBeInTheDocument();
    expect(screen.getByLabelText('diameter Mean')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Some values need attention');
    expect(posts(backend)).toHaveLength(0);
  });

  it('shows the messages when the server rejects a request', async () => {
    const backend = installFakeBackend();
    backend.rejectNext = {
      status: 400,
      body: { title: 'Invalid request', detail: 'Request validation failed', errors: { samples: 'must be at least 100' } },
    };
    const user = userEvent.setup();
    render(<App />);
    await screen.findByLabelText('diameter Mean');

    await user.click(screen.getByRole('button', { name: 'Run simulation' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('samples: must be at least 100');
    expect(screen.getAllByText('must be at least 100').length).toBeGreaterThan(0); // beside the field too
    expect(screen.getByRole('button', { name: 'Run simulation' })).toBeEnabled();
  });

  it('says so when the server cannot be reached, then recovers on its own', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const backend = installFakeBackend();
    backend.down = true;
    render(<App />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');

    backend.down = false;
    await act(() => vi.advanceTimersByTimeAsync(5100));
    expect(await screen.findByRole('heading', { name: 'New simulation' })).toBeInTheDocument();
  });

  it('shows a failed job with its reason', async () => {
    installFakeBackend({
      jobs: [
        makeJob(1, {
          status: 'FAILED',
          result: null,
          errorMessage: "All 1000 samples were outside the model's physical domain",
        }),
      ],
    });
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: /Oct 2026|2026/ }));

    const detail = await screen.findByRole('region', { name: 'Job result' });
    expect(within(detail).getByText('Failed')).toBeInTheDocument();
    expect(within(detail).getByRole('alert')).toHaveTextContent('physical domain');
    expect(within(detail).queryByRole('img')).not.toBeInTheDocument();
  });

  it('pages through the jobs and filters them by status', async () => {
    const jobs = Array.from({ length: 20 }, (_, i) => makeJob(i + 1, i % 5 === 0 ? { status: 'FAILED', result: null, errorMessage: 'x' } : {}));
    const backend = installFakeBackend({ jobs });
    const user = userEvent.setup();
    render(<App />);

    expect(await screen.findByText('Page 1 of 3 · 20 jobs')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(await screen.findByText('Page 2 of 3 · 20 jobs')).toBeInTheDocument();
    expect(backend.requests.some((r) => r.search.includes('page=1&size=8'))).toBe(true);

    await user.selectOptions(screen.getByLabelText('Filter by status'), 'FAILED');
    expect(await screen.findByText('Page 1 of 1 · 4 jobs')).toBeInTheDocument();
    expect(backend.requests.at(-1).search).toContain('status=FAILED');
  });

  it('shows a clear message for a status with no jobs', async () => {
    installFakeBackend({ jobs: [makeJob(1)] });
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText('Page 1 of 1 · 1 jobs');

    await user.selectOptions(screen.getByLabelText('Filter by status'), 'RUNNING');

    expect(await screen.findByText('No jobs with this status.')).toBeInTheDocument();
  });

  it('shows the statistics of a finished job when it is selected from the list', async () => {
    installFakeBackend({ jobs: [makeJob(7, { result: makeResult({ samplesRejected: 12 }) })] });
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: /2026/ }));

    const detail = await screen.findByRole('region', { name: 'Job result' });
    expect(within(detail).getByText('Standard deviation')).toBeInTheDocument();
    expect(within(detail).getByText('20,334.3', { exact: false })).toBeInTheDocument();
    expect(within(detail).getByText('50%')).toBeInTheDocument();
    expect(within(detail).getByText(/12 rejected as non-physical/)).toBeInTheDocument();
  });
});
