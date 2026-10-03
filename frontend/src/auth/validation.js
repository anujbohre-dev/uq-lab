// Mirrors the backend rules so mistakes are caught before a request is sent.
// The server still checks everything again: this is for convenience, not security.

export const USERNAME_PATTERN = /^[A-Za-z0-9_.-]+$/;

/** Returns { username?: message, password?: message }. Empty object means valid. */
export function validateCredentials(username, password, { creating }) {
  const errors = {};
  const name = username.trim();
  if (!name) errors.username = 'Enter a username.';
  else if (creating) {
    if (name.length < 3 || name.length > 30) errors.username = 'Username must be 3 to 30 characters.';
    else if (!USERNAME_PATTERN.test(name))
      errors.username = 'Use only letters, digits, dot, dash and underscore.';
  }
  if (!password) errors.password = 'Enter a password.';
  else if (creating) {
    if (password.length < 8) errors.password = 'Password must be at least 8 characters.';
    else if (new TextEncoder().encode(password).length > 72)
      errors.password = 'Password must be at most 72 bytes (about 72 characters).';
  }
  return errors;
}
