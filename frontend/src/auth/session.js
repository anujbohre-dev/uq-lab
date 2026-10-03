// Where the signed-in user is remembered between page reloads.
//
// The token is kept in sessionStorage: it survives a reload, but disappears when the tab is
// closed. Trade-off, stated honestly: JavaScript on the page can read sessionStorage, so a
// cross-site-scripting (XSS) bug could steal the token. A httpOnly cookie avoids that and is a
// possible future improvement. Every access is wrapped in try/catch because browsers can block
// storage (private windows, strict settings); the app then simply works without remembering.

const KEY = 'uqlab.session';

/** Returns { accessToken, username } or null. */
export function loadSession() {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const s = JSON.parse(raw);
    return s && typeof s.accessToken === 'string' && typeof s.username === 'string' ? s : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ accessToken: session.accessToken, username: session.username }));
  } catch {
    /* storage unavailable: the user stays signed in until the page is reloaded */
  }
}

export function clearSession() {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    /* nothing to clear */
  }
}
