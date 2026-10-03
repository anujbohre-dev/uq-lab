import { describe, expect, it } from 'vitest';
import { initialForm } from './presets.js';
import { blankField, buildRequest, parseNumber } from './requestBuilder.js';
import { MODELS } from '../test/fakeBackend.js';

const euler = MODELS[0];

describe('parseNumber', () => {
  it('accepts plain, decimal and scientific notation', () => {
    expect(parseNumber('42')).toBe(42);
    expect(parseNumber(' 0.05 ')).toBe(0.05);
    expect(parseNumber('2e11')).toBe(200000000000);
    expect(parseNumber('-1.5E-3')).toBe(-0.0015);
    expect(parseNumber('.5')).toBe(0.5);
  });

  it('rejects empty and non-numeric text', () => {
    expect(parseNumber('')).toBeNull();
    expect(parseNumber('abc')).toBeNull();
    expect(parseNumber('1,000')).toBeNull();
    expect(parseNumber('0x10')).toBeNull();
    expect(parseNumber('Infinity')).toBeNull();
    expect(parseNumber('1e999')).toBeNull();
  });
});

describe('buildRequest', () => {
  it('turns the preset form into the JSON the backend expects', () => {
    const { request, errors } = buildRequest(euler, initialForm(euler));
    expect(errors).toBeUndefined();
    expect(request).toEqual({
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
  });

  it('includes the seed only when one is given', () => {
    const form = { ...initialForm(euler), seed: '2026' };
    expect(buildRequest(euler, form).request.seed).toBe(2026);
    expect('seed' in buildRequest(euler, initialForm(euler)).request).toBe(false);
  });

  it('sends only the numbers that the chosen distribution type uses', () => {
    const form = initialForm(euler);
    form.inputs.length = { ...blankField('CONSTANT'), value: '2', mean: 'stale text from a previous choice' };
    expect(buildRequest(euler, form).request.inputs.length).toEqual({ type: 'CONSTANT', value: 2 });
  });

  it('reports every problem at once, keyed by field', () => {
    const form = initialForm(euler);
    form.inputs.diameter.mean = '';
    form.inputs.length.stdDev = '-1';
    form.inputs.effectiveLengthFactor.min = '2';
    form.inputs.youngsModulus.mean = '0';
    form.samples = '50';
    form.bins = '1.5';
    form.seed = '-3';
    const { errors, request } = buildRequest(euler, form);
    expect(request).toBeUndefined();
    expect(errors['inputs.diameter.mean']).toBe('Enter a number');
    expect(errors['inputs.length.stdDev']).toBe('Must be 0 or more');
    expect(errors['inputs.effectiveLengthFactor.max']).toBe('Must be greater than min');
    expect(errors['inputs.youngsModulus.mean']).toBe('Must be greater than 0');
    expect(errors.samples).toBe('Must be between 100 and 1000000');
    expect(errors.bins).toBe('Enter a whole number');
    expect(errors.seed).toBe('Must be at least 0');
  });
});

describe('initialForm', () => {
  it('gives blank fields to a model that has no preset', () => {
    const unknown = { id: 'new-model', inputs: [{ name: 'x', unit: 'm', description: '' }] };
    const form = initialForm(unknown);
    expect(form.inputs.x).toEqual(blankField('NORMAL'));
    expect(buildRequest(unknown, form).errors['inputs.x.mean']).toBe('Enter a number');
  });
});
