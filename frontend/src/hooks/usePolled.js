import { useEffect, useState } from 'react';

/**
 * Loads data with `fetcher`, and keeps reloading it every `intervalMs` while `keepPolling(data)`
 * says so (for example while a job is still QUEUED or RUNNING). It reloads from scratch whenever
 * a value in `deps` changes, and stops cleanly when the component leaves the screen.
 *
 * If the server cannot be reached it tries again after a few seconds, so the page recovers by
 * itself when the backend is restarted. Other errors (404, 400) are shown and not retried.
 *
 * @returns { data, error, loading }
 */
export function usePolled(fetcher, deps, keepPolling = () => false, intervalMs = 2000) {
  const [state, setState] = useState({ data: null, error: null, loading: true });

  useEffect(() => {
    let cancelled = false;
    let timer;

    async function load() {
      try {
        const data = await fetcher();
        if (cancelled) return;
        setState({ data, error: null, loading: false });
        if (keepPolling(data)) timer = setTimeout(load, intervalMs);
      } catch (error) {
        if (cancelled) return;
        // Keep showing the last good data, with the error beside it.
        setState((previous) => ({ data: previous.data, error, loading: false }));
        if (error.status === 0 || error.status >= 500) timer = setTimeout(load, intervalMs * 2.5);
      }
    }

    setState((previous) => ({ ...previous, loading: true }));
    load();

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}
