import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import App from '../App.jsx';
import { installFakeBackend, signInAs } from '../test/fakeBackend.js';

const savedSession = () => JSON.parse(sessionStorage.getItem('uqlab.session'));

describe('sign in and out', () => {
  it('shows the sign-in page when nobody is signed in, and calls no protected route', async () => {
    const backend = installFakeBackend();
    render(<App />);
    expect(screen.getByRole('tab', { name: 'Sign in', selected: true })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'New simulation' })).not.toBeInTheDocument();
    expect(backend.requests).toHaveLength(0);
  });

  it('signs in, remembers the session, and sends the token with every request', async () => {
    const backend = installFakeBackend();
    const user = userEvent.setup();
    render(<App />);

    await user.type(screen.getByLabelText('Username'), 'Alice');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(document.querySelector('button[type=submit]'));

    expect(await screen.findByRole('heading', { name: 'New simulation' })).toBeInTheDocument();
    expect(screen.getByText('alice')).toBeInTheDocument();
    expect(savedSession()).toEqual({ accessToken: 'fake-token-alice', username: 'alice' });
    const protectedCalls = backend.requests.filter((r) => r.path === '/api/models');
    expect(protectedCalls[0].authorization).toBe('Bearer fake-token-alice');
  });

  it('shows the server message for a wrong password and stays on the page', async () => {
    installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByLabelText('Username'), 'alice');
    await user.type(screen.getByLabelText('Password'), 'wrong-password');
    await user.click(document.querySelector('button[type=submit]'));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password');
    expect(sessionStorage.getItem('uqlab.session')).toBeNull();
  });

  it('creates an account and signs in at once', async () => {
    const backend = installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('tab', { name: 'Create account' }));
    await user.type(screen.getByLabelText('Username'), 'Newcomer');
    await user.type(screen.getByLabelText('Password'), 'goodpassword');
    await user.click(document.querySelector('button[type=submit]'));

    expect(await screen.findByRole('heading', { name: 'New simulation' })).toBeInTheDocument();
    expect(backend.users.newcomer).toBe('goodpassword');
    expect(screen.getByText('newcomer')).toBeInTheDocument();
  });

  it('reports a taken username from the server', async () => {
    installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('tab', { name: 'Create account' }));
    await user.type(screen.getByLabelText('Username'), 'alice');
    await user.type(screen.getByLabelText('Password'), 'goodpassword');
    await user.click(document.querySelector('button[type=submit]'));
    expect(await screen.findByRole('alert')).toHaveTextContent(/already taken/);
  });

  it('checks the form before sending anything', async () => {
    const backend = installFakeBackend();
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('tab', { name: 'Create account' }));
    await user.type(screen.getByLabelText('Username'), 'ab');
    await user.type(screen.getByLabelText('Password'), 'short');
    await user.click(document.querySelector('button[type=submit]'));

    expect(screen.getByText('Username must be 3 to 30 characters.')).toBeInTheDocument();
    expect(screen.getByText('Password must be at least 8 characters.')).toBeInTheDocument();
    expect(backend.requests).toHaveLength(0);
  });

  it('restores a saved session after a reload', async () => {
    installFakeBackend();
    signInAs('alice');
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'New simulation' })).toBeInTheDocument();
  });

  it('logs out: clears the saved session and returns to the sign-in page', async () => {
    installFakeBackend();
    signInAs('alice');
    const user = userEvent.setup();
    render(<App />);
    await screen.findByRole('heading', { name: 'New simulation' });

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    expect(screen.getByRole('tab', { name: 'Sign in' })).toBeInTheDocument();
    expect(sessionStorage.getItem('uqlab.session')).toBeNull();
  });

  it('signs the user out with a message when the token is refused', async () => {
    const backend = installFakeBackend();
    backend.expireTokens = true;
    signInAs('alice');
    render(<App />);

    expect(await screen.findByText('Your session expired. Please sign in again.')).toBeInTheDocument();
    await waitFor(() => expect(sessionStorage.getItem('uqlab.session')).toBeNull());
  });
});
