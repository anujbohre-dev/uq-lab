import { DISTRIBUTIONS, PARAM_LABELS } from '../lib/requestBuilder.js';

/**
 * The controls for ONE model input: a distribution type menu plus the numbers that type needs.
 * It owns no state; the parent form passes the current value and receives every change.
 *
 * Props: input {name, unit, description}, spec (the current values), errors (message per key),
 * prefix (key prefix used in errors), onChange(newSpec)
 */
export default function DistributionField({ input, spec, errors, prefix, onChange }) {
  const params = DISTRIBUTIONS[spec.type].params;
  const setValue = (param, text) => onChange({ ...spec, [param]: text });

  return (
    <div className="input-row">
      <div className="input-label">
        <span className="input-name">{input.name}</span>
        <span className="input-unit"> [{input.unit}]</span>
        <p className="hint">{input.description}</p>
      </div>
      <div className="input-controls">
        <select
          aria-label={`${input.name} distribution`}
          value={spec.type}
          onChange={(e) => onChange({ ...spec, type: e.target.value })}
        >
          {Object.entries(DISTRIBUTIONS).map(([type, { label }]) => (
            <option key={type} value={type}>
              {label}
            </option>
          ))}
        </select>
        {params.map((param) => {
          const error = errors[`${prefix}.${param}`];
          return (
            <label key={param} className="param">
              <span>{PARAM_LABELS[param]}</span>
              <input
                type="text"
                inputMode="decimal"
                aria-label={`${input.name} ${PARAM_LABELS[param]}`}
                aria-invalid={error ? 'true' : undefined}
                value={spec[param]}
                onChange={(e) => setValue(param, e.target.value)}
              />
              {error && <span className="field-error">{error}</span>}
            </label>
          );
        })}
      </div>
    </div>
  );
}
