// Starting values for the form, so a first run needs one click. Each preset describes the model's
// inputs the way an engineer would: a nominal value with a small spread.
// A model that is not listed here gets blank "Normal" fields.

import { blankField } from './requestBuilder.js';

const normal = (mean, stdDev) => ({ ...blankField('NORMAL'), mean: String(mean), stdDev: String(stdDev) });
const logNormal = (mean, stdDev) => ({ ...blankField('LOGNORMAL'), mean: String(mean), stdDev: String(stdDev) });
const uniform = (min, max) => ({ ...blankField('UNIFORM'), min: String(min), max: String(max) });

const PRESETS = {
  'euler-column-buckling': {
    youngsModulus: logNormal('2e11', '1e10'),
    diameter: normal(0.05, 0.0005),
    length: normal(2, 0.01),
    effectiveLengthFactor: uniform(0.9, 1.1),
  },
  'cantilever-tip-deflection': {
    load: normal(1000, 100),
    length: normal(1, 0.005),
    youngsModulus: logNormal('2e11', '1e10'),
    diameter: normal(0.05, 0.0005),
  },
  'spring-mass-natural-frequency': {
    stiffness: normal(1000, 50),
    mass: uniform(0.95, 1.05),
  },
};

export const DEFAULT_SETTINGS = { samples: '10000', seed: '', bins: '20' };

/** A complete form state for the model: its preset inputs (or blanks) plus the default settings. */
export function initialForm(model) {
  const preset = PRESETS[model.id] || {};
  const inputs = {};
  for (const input of model.inputs) {
    inputs[input.name] = preset[input.name] ? { ...preset[input.name] } : blankField('NORMAL');
  }
  return { inputs, ...DEFAULT_SETTINGS };
}
