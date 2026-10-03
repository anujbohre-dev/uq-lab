import { useCallback, useEffect, useState } from 'react';
import { getModels, setAuthToken, setUnauthorizedHandler } from './api/client.js';
import LoginPage from './auth/LoginPage.jsx';
import { clearSession, loadSession, saveSession } from './auth/session.js';
import JobDetail from './components/JobDetail.jsx';
import JobsPanel from './components/JobsPanel.jsx';
import RunForm from './components/RunForm.jsx';
import { usePolled } from './hooks/usePolled.js';

/**
 * Decides between the sign-in page and the workspace. The token is handed to the API client
 * before the first render of the workspace, so no request is ever sent without it.
 */
export default function App() {
  const [session, setSession] = useState(() => {
    const saved = loadSession();
    setAuthToken(saved?.accessToken);
    return saved;
  });
  const [notice, setNotice] = useState('');

  const logOut = useCallback((message = '') => {
    setAuthToken(null);
    clearSession();
    setSession(null);
    setNotice(message);
  }, []);

  // An expired or invalid token anywhere in the app ends the session.
  useEffect(() => {
    setUnauthorizedHandler(() => logOut('Your session expired. Please sign in again.'));
    return () => setUnauthorizedHandler(null);
  }, [logOut]);

  function handleSignedIn(next) {
    setAuthToken(next.accessToken);
    saveSession(next);
    setNotice('');
    setSession(next);
  }

  if (!session) return <LoginPage onSignedIn={handleSignedIn} notice={notice} />;
  return <Workspace username={session.username} onLogOut={() => logOut()} />;
}

/**
 * The page. It loads the list of models once, then shows three areas:
 * the form (left), the jobs list and the selected result (right).
 */
function Workspace({ username, onLogOut }) {
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
        <div className="top-row">
          <h1>UQ Lab</h1>
          <div className="user-box">
            <span>
              Signed in as <strong>{username}</strong>
            </span>
            <button type="button" onClick={onLogOut}>
              Log out
            </button>
          </div>
        </div>
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
