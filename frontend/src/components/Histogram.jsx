import { formatNumber } from '../lib/format.js';

const WIDTH = 640;
const HEIGHT = 280;
const MARGIN = { top: 16, right: 16, bottom: 46, left: 56 };

/**
 * A bar chart drawn directly as SVG (no chart library), so every number on it is traceable.
 *
 * Props: edges (bins + 1 numbers), counts (bins numbers), unit, label, mean (draws a vertical line)
 */
export default function Histogram({ edges, counts, unit, label, mean }) {
  const plotW = WIDTH - MARGIN.left - MARGIN.right;
  const plotH = HEIGHT - MARGIN.top - MARGIN.bottom;
  const lo = edges[0];
  const hi = edges[edges.length - 1];
  const maxCount = Math.max(...counts, 1);
  // A run with no spread has every sample in one bin of zero width: draw it across the whole plot.
  const flat = hi === lo;

  // Maps a data value to a pixel position, and a count to a bar height.
  const x = (value) => MARGIN.left + ((value - lo) / (hi - lo || 1)) * plotW;
  const barHeight = (count) => (count / maxCount) * plotH;
  const baseline = MARGIN.top + plotH;

  const xTicks = [lo, (lo + hi) / 2, hi];
  const yTicks = [0, maxCount / 2, maxCount];

  return (
    <svg
      className="histogram"
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      role="img"
      aria-label={`Histogram of ${label} in ${unit}: ${counts.length} bins, most samples in the tallest bar`}
    >
      {yTicks.map((tick) => {
        const y = baseline - barHeight(tick);
        return (
          <g key={`y-${tick}`}>
            <line x1={MARGIN.left} x2={WIDTH - MARGIN.right} y1={y} y2={y} className="grid-line" />
            <text x={MARGIN.left - 8} y={y + 4} textAnchor="end" className="axis-text">
              {Math.round(tick).toLocaleString('en-US')}
            </text>
          </g>
        );
      })}

      {counts.map((count, i) => (
        <rect
          key={i}
          className="bar"
          x={flat ? MARGIN.left : x(edges[i]) + 0.5}
          y={baseline - barHeight(count)}
          width={flat ? plotW : Math.max(x(edges[i + 1]) - x(edges[i]) - 1, 0)}
          height={barHeight(count)}
        >
          <title>{`${formatNumber(edges[i])} to ${formatNumber(edges[i + 1])} ${unit}: ${count.toLocaleString('en-US')} samples`}</title>
        </rect>
      ))}

      <line x1={MARGIN.left} x2={WIDTH - MARGIN.right} y1={baseline} y2={baseline} className="axis-line" />

      {xTicks.map((tick, i) => (
        <text
          key={`x-${i}`}
          x={x(tick)}
          y={baseline + 18}
          textAnchor={i === 0 ? 'start' : i === 2 ? 'end' : 'middle'}
          className="axis-text"
        >
          {formatNumber(tick)}
        </text>
      ))}
      <text x={MARGIN.left + plotW / 2} y={HEIGHT - 6} textAnchor="middle" className="axis-text">
        {label} [{unit}]
      </text>

      {mean !== undefined && mean >= lo && mean <= hi && (
        <g>
          <line x1={x(mean)} x2={x(mean)} y1={MARGIN.top} y2={baseline} className="mean-line" />
          <text x={x(mean) + 4} y={MARGIN.top + 10} className="mean-text">
            mean
          </text>
        </g>
      )}
    </svg>
  );
}
