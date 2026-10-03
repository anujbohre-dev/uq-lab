import { describe, expect, it } from 'vitest';
import { validateCredentials } from './validation.js';

describe('validateCredentials', () => {
  it('accepts a good new account', () => {
    expect(validateCredentials('anuj_b', 'longenough1', { creating: true })).toEqual({});
  });
  it('asks for both fields', () => {
    expect(validateCredentials('  ', '', { creating: false })).toEqual({
      username: 'Enter a username.',
      password: 'Enter a password.',
    });
  });
  it('applies length and character rules only when creating an account', () => {
    expect(validateCredentials('ab', 'x', { creating: false })).toEqual({});
    expect(validateCredentials('ab', 'x', { creating: true })).toEqual({
      username: 'Username must be 3 to 30 characters.',
      password: 'Password must be at least 8 characters.',
    });
    expect(validateCredentials('bad name!', 'longenough1', { creating: true }).username).toMatch(/letters, digits/);
  });
  it('rejects passwords over 72 bytes, counting multi-byte characters', () => {
    expect(validateCredentials('abc', 'a'.repeat(72), { creating: true })).toEqual({});
    expect(validateCredentials('abc', 'a'.repeat(73), { creating: true }).password).toMatch(/72 bytes/);
    expect(validateCredentials('abc', 'é'.repeat(37), { creating: true }).password).toMatch(/72 bytes/);
  });
});
