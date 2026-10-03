import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import Histogram from './Histogram.jsx';

describe('Histogram', () => {
  it('draws one bar per bin, with the tallest bar filling the plot height', () => {
    const { container } = render(
      <Histogram edges={[0, 10, 20, 30]} counts={[5, 20, 10]} unit="N" label="criticalLoad" mean={15} />,
    );
    const bars = container.querySelectorAll('rect.bar');
    expect(bars).toHaveLength(3);
    const heights = [...bars].map((b) => Number(b.getAttribute('height')));
    expect(heights[1]).toBeCloseTo(218, 5); // 280 - 16 - 46 = plot height
    expect(heights[0]).toBeCloseTo(heights[1] / 4, 5);
    expect(heights[2]).toBeCloseTo(heights[1] / 2, 5);
  });

  it('describes itself for screen readers and tooltips', () => {
    render(<Histogram edges={[0, 10, 20]} counts={[3, 7]} unit="Hz" label="naturalFrequency" mean={5} />);
    expect(screen.getByRole('img')).toHaveAccessibleName(/Histogram of naturalFrequency in Hz: 2 bins/);
    expect(screen.getByText('0 to 10 Hz: 3 samples')).toBeInTheDocument();
  });

  it('draws the mean line only when the mean lies inside the range', () => {
    const inside = render(<Histogram edges={[0, 10, 20]} counts={[1, 1]} unit="N" label="x" mean={10} />);
    expect(inside.container.querySelector('.mean-line')).not.toBeNull();
    inside.unmount();
    const outside = render(<Histogram edges={[0, 10, 20]} counts={[1, 1]} unit="N" label="x" mean={99} />);
    expect(outside.container.querySelector('.mean-line')).toBeNull();
  });

  it('shows a zero-spread run as one full-width bar', () => {
    const { container } = render(
      <Histogram edges={[151397.8, 151397.8]} counts={[1000]} unit="N" label="criticalLoad" mean={151397.8} />,
    );
    const bar = container.querySelector('rect.bar');
    expect(Number(bar.getAttribute('width'))).toBe(568); // 640 - 56 - 16
    expect(Number(bar.getAttribute('height'))).toBeGreaterThan(0);
  });
});
