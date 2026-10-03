import { useState } from 'react';
import { getModels } from './api/client.js';
import JobDetail from './components/JobDetail.jsx';
import JobsPanel from './components/JobsPanel.jsx';
import RunForm from './components/RunForm.jsx';
import { usePolled } from './hooks/usePolled.js';

/**
 * The page. It loads the list of models once, then shows three areas:
 * the form (left), the jobs list and the selected result (right).
 */
export default function App() {
  const { data: models, error, loading } = usePolled(getModels, [], () => false);
  const [selectedId, setSelectedId] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  function handleSubmitted(job) {
    setSelectedId(job.id);
    setRefreshKey((k) => k + 1);
  }

  return (
    <div className="page">
      <header className="top">
        <h1>UQ Lab</h1>
        <p>Monte Carlo uncertainty quantification for engineering formulas</p>
      </header>

      {loading && !models && <p>Loading models...</p>}
      {error && !models && (
        <div className="notice notice-error" role="alert">
          {error.message}
          {error.status === 0 && ' Retrying...'}
        </div>
      )}

      {models && (
        <main className="layout">
          <div className="column">
            <RunForm models={models} onSubmitted={handleSubmitted} />
          </div>
          <div className="column">
            <JobsPanel models={models} selectedId={selectedId} onSelect={setSelectedId} refreshKey={refreshKey} />
            {selectedId ? (
              <JobDetail key={selectedId} jobId={selectedId} />
            ) : (
              <p className="empty">Select a job to see its result.</p>
            )}
          </div>
        </main>
      )}
    </div>
  );
}
