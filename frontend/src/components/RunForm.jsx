import { useState } from 'react';
import { submitJob } from '../api/client.js';
import { buildRequest } from '../lib/requestBuilder.js';
import { initialForm } from '../lib/presets.js';
import DistributionField from './DistributionField.jsx';

const SETTING_KEYS = ['samples', 'seed', 'bins'];

/**
 * The form that describes one simulation and sends it as a job.
 * Props: models (from GET /api/models), onSubmitted(job) called after the server accepts the job.
 */
export default function RunForm({ models, onSubmitted }) {
  const [modelId, setModelId] = useState(models[0].id);
  const model = models.find((m) => m.id === modelId);
  const [form, setForm] = useState(() => initialForm(models[0]));
  const [fieldErrors, setFieldErrors] = useState({});
  const [bannerErrors, setBannerErrors] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  function chooseModel(id) {
    setModelId(id);
    setForm(initialForm(models.find((m) => m.id === id)));
    setFieldErrors({});
    setBannerErrors([]);
  }

  const setInput = (name, spec) => setForm((f) => ({ ...f, inputs: { ...f.inputs, [name]: spec } }));
  const setSetting = (key, text) => setForm((f) => ({ ...f, [key]: text }));

  async function handleSubmit(event) {
    event.preventDefault();
    setBannerErrors([]);

    const result = buildRequest(model, form);
    if (result.errors) {
      setFieldErrors(result.errors);
      setBannerErrors(['Some values need attention. See the fields marked in red.']);
      return;
    }
    setFieldErrors({});

    setSubmitting(true);
    try {
      const job = await submitJob(result.request);
      onSubmitted(job);
    } catch (error) {
      // The backend lists every failing field in error.errors; show them all.
      const details = Object.entries(error.errors).map(([field, message]) => `${field}: ${message}`);
      setBannerErrors(details.length > 0 ? details : [error.message]);
      const settingErrors = {};
      SETTING_KEYS.forEach((key) => {
        if (error.errors[key]) settingErrors[key] = error.errors[key];
      });
      setFieldErrors(settingErrors);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <h2>New simulation</h2>

      <label className="stacked">
        <span>Model</span>
        <select aria-label="Model" value={modelId} onChange={(e) => chooseModel(e.target.value)}>
          {models.map((m) => (
            <option key={m.id} value={m.id}>
              {m.title}
            </option>
          ))}
        </select>
      </label>
      <p className="hint">
        Output: <strong>{model.outputName}</strong> [{model.outputUnit}]. Give each input a probability
        distribution; the server draws random values and evaluates the formula for each draw.
      </p>

      {model.inputs.map((input) => (
        <DistributionField
          key={`${model.id}-${input.name}`}
          input={input}
          spec={form.inputs[input.name]}
          errors={fieldErrors}
          prefix={`inputs.${input.name}`}
          onChange={(spec) => setInput(input.name, spec)}
        />
      ))}

      <fieldset className="settings">
        <legend>Run settings</legend>
        {[
          ['samples', 'Samples', '100 to 1,000,000'],
          ['seed', 'Seed (optional)', 'same seed, same result'],
          ['bins', 'Histogram bins', '2 to 200'],
        ].map(([key, label, hint]) => (
          <label key={key} className="stacked">
            <span>{label}</span>
            <input
              type="text"
              inputMode="numeric"
              aria-label={label}
              aria-invalid={fieldErrors[key] ? 'true' : undefined}
              value={form[key]}
              onChange={(e) => setSetting(key, e.target.value)}
            />
            <span className="hint">{fieldErrors[key] ? <span className="field-error">{fieldErrors[key]}</span> : hint}</span>
          </label>
        ))}
      </fieldset>

      {bannerErrors.length > 0 && (
        <div className="notice notice-error" role="alert">
          {bannerErrors.map((line) => (
            <div key={line}>{line}</div>
          ))}
        </div>
      )}

      <div className="actions">
        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Submitting...' : 'Run simulation'}
        </button>
        <button type="button" onClick={() => chooseModel(modelId)} disabled={submitting}>
          Reset values
        </button>
      </div>
    </form>
  );
}
