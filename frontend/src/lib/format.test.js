import { describe, expect, it } from 'vitest';
import { formatDuration, formatNumber, isActive } from './format.js';

describe('formatNumber', () => {
  it('keeps six significant digits and adds thousands separators', () => {
    expect(formatNumber(153074.60387277894)).toBe('153,075');
    expect(formatNumber(151397.8353530265)).toBe('151,398');
  });

  it('keeps small values readable instead of rounding them to zero', () => {
    expect(formatNumber(0.0054324887242033605)).toBe('0.00543249');
    expect(formatNumber(5.032921210448704)).toBe('5.03292');
  });

  it('shows a dash for missing values', () => {
    expect(formatNumber(null)).toBe('-');
    expect(formatNumber(undefined)).toBe('-');
    expect(formatNumber(Number.NaN)).toBe('-');
  });
});

describe('formatDuration', () => {
  it('uses milliseconds below one second and seconds above', () => {
    expect(formatDuration('2026-10-03T02:48:05.000Z', '2026-10-03T02:48:05.406Z')).toBe('406 ms');
    expect(formatDuration('2026-10-03T02:48:05Z', '2026-10-03T02:48:07.5Z')).toBe('2.5 s');
  });

  it('shows a dash when either time is missing', () => {
    expect(formatDuration(null, '2026-10-03T02:48:07Z')).toBe('-');
  });
});

describe('isActive', () => {
  it('is true only for jobs that have not finished', () => {
    expect(isActive('QUEUED')).toBe(true);
    expect(isActive('RUNNING')).toBe(true);
    expect(isActive('DONE')).toBe(false);
    expect(isActive('FAILED')).toBe(false);
  });
});
