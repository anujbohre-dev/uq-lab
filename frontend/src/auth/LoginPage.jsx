import { useState } from 'react';
import { login, register } from '../api/client.js';
import { validateCredentials } from './validation.js';

/**
 * Sign in or create an account. onSignedIn receives { accessToken, username }.
 * `notice` is an optional message, e.g. "Your session expired".
 */
export default function LoginPage({ onSignedIn, notice }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [busy, setBusy] = useState(false);

  const creating = mode === 'register';

  function switchMode(next) {
    setMode(next);
    setErrors({});
    setServerError('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError('');
    const found = validateCredentials(username, password, { creating });
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setBusy(true);
    try {
      const result = await (creating ? register : login)(username.trim(), password);
      onSignedIn({ accessToken: result.accessToken, username: result.username });
    } catch (e) {
      if (e.status === 400 && e.errors && Object.keys(e.errors).length > 0) setErrors(e.errors);
      setServerError(e.message);
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <header className="top">
        <h1>UQ Lab</h1>
        <p>Monte Carlo uncertainty quantification for engineering formulas</p>
      </header>

      <section className="card auth-card">
        <div className="tabs" role="tablist" aria-label="Account">
          <button type="button" role="tab" aria-selected={!creating} onClick={() => switchMode('login')}>
            Sign in
          </button>
          <button type="button" role="tab" aria-selected={creating} onClick={() => switchMode('register')}>
            Create account
          </button>
        </div>

        {notice && (
          <div className="notice" role="status">
            {notice}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <label>
            Username
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              maxLength={30}
              aria-invalid={Boolean(errors.username)}
            />
          </label>
          {errors.username && <p className="field-error">{errors.username}</p>}

          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={creating ? 'new-password' : 'current-password'}
              aria-invalid={Boolean(errors.password)}
            />
          </label>
          {errors.password && <p className="field-error">{errors.password}</p>}
          {creating && <p className="hint">At least 8 characters. Usernames are not case-sensitive.</p>}

          {serverError && (
            <div className="notice notice-error" role="alert">
              {serverError}
            </div>
          )}

          <button type="submit" className="primary" disabled={busy}>
            {busy ? 'Please wait...' : creating ? 'Create account' : 'Sign in'}
          </button>
        </form>
      </section>
    </div>
  );
}
