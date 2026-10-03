// Turns what the user typed into the JSON the backend expects, and checks it first.
// The checks repeat the backend's rules so mistakes show up beside the field at once. The
// backend still checks everything again, because it must never trust the browser.

/** Which numbers each distribution type needs, and what to call them on screen. */
export const DISTRIBUTIONS = {
  NORMAL: { label: 'Normal', params: ['mean', 'stdDev'] },
  LOGNORMAL: { label: 'Log-normal', params: ['mean', 'stdDev'] },
  UNIFORM: { label: 'Uniform', params: ['min', 'max'] },
  CONSTANT: { label: 'Constant (known exactly)', params: ['value'] },
};

export const PARAM_LABELS = {
  mean: 'Mean',
  stdDev: 'Std dev',
  min: 'Min',
  max: 'Max',
  value: 'Value',
};

const NUMBER = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/;

/** "2e11" -> 200000000000. Returns null for empty or non-numeric text. */
export function parseNumber(text) {
  const trimmed = String(text ?? '').trim();
  if (!NUMBER.test(trimmed)) return null;
  const value = Number(trimmed);
  return Number.isFinite(value) ? value : null;
}

/** A blank field definition: everything stored as text, because that is what the user types. */
export function blankField(type = 'NORMAL') {
  return { type, mean: '', stdDev: '', min: '', max: '', value: '' };
}

function checkDistribution(spec, errors, prefix) {
  const numbers = {};
  for (const param of DISTRIBUTIONS[spec.type].params) {
    const n = parseNumber(spec[param]);
    if (n === null) errors[`${prefix}.${param}`] = 'Enter a number';
    else numbers[param] = n;
  }
  const { mean, stdDev, min, max } = numbers;
  if (spec.type === 'NORMAL' || spec.type === 'LOGNORMAL') {
    if (stdDev !== undefined && stdDev < 0) errors[`${prefix}.stdDev`] = 'Must be 0 or more';
  }
  if (spec.type === 'LOGNORMAL' && mean !== undefined && mean <= 0) {
    errors[`${prefix}.mean`] = 'Must be greater than 0';
  }
  if (spec.type === 'UNIFORM' && min !== undefined && max !== undefined && min >= max) {
    errors[`${prefix}.max`] = 'Must be greater than min';
  }
  return numbers;
}

function checkInteger(text, label, { min, max, optional }) {
  if (String(text).trim() === '') {
    return optional ? { value: undefined } : { error: 'Enter a whole number' };
  }
  const n = parseNumber(text);
  if (n === null || !Number.isInteger(n)) return { error: 'Enter a whole number' };
  if (n < min || (max !== undefined && n > max)) {
    return { error: max === undefined ? `Must be at least ${min}` : `Must be between ${min} and ${max}` };
  }
  return { value: n };
}

/**
 * @param model the model description from GET /api/models
 * @param form { inputs: { [name]: {type, mean, ...} }, samples, seed, bins }
 * @returns { request } when valid, or { errors } with one message per field key
 */
export function buildRequest(model, form) {
  const errors = {};
  const inputs = {};

  for (const input of model.inputs) {
    const spec = form.inputs[input.name];
    const numbers = checkDistribution(spec, errors, `inputs.${input.name}`);
    inputs[input.name] = { type: spec.type, ...numbers };
  }

  const samples = checkInteger(form.samples, 'samples', { min: 100, max: 1_000_000 });
  const bins = checkInteger(form.bins, 'bins', { min: 2, max: 200 });
  const seed = checkInteger(form.seed, 'seed', { min: 0, optional: true });
  if (samples.error) errors.samples = samples.error;
  if (bins.error) errors.bins = bins.error;
  if (seed.error) errors.seed = seed.error;

  if (Object.keys(errors).length > 0) return { errors };

  const request = { modelId: model.id, samples: samples.value, bins: bins.value, inputs };
  if (seed.value !== undefined) request.seed = seed.value;
  return { request };
}
