package com.anujbohre.uqlab.domain.montecarlo;

import java.util.Arrays;

/** Pure statistical helpers: no randomness, no framework. */
final class Statistics {

    private Statistics() {}

    /** Arithmetic mean using Welford's running update (numerically stable). */
    static double mean(double[] v) {
        double mean = 0.0;
        for (int i = 0; i < v.length; i++) {
            mean += (v[i] - mean) / (i + 1);
        }
        return mean;
    }

    /** Sample standard deviation (divisor n-1) using Welford's algorithm. Returns 0 for n &lt; 2. */
    static double sampleStdDev(double[] v) {
        if (v.length < 2) {
            return 0.0;
        }
        double mean = 0.0;
        double m2 = 0.0;
        for (int i = 0; i < v.length; i++) {
            double delta = v[i] - mean;
            mean += delta / (i + 1);
            m2 += delta * (v[i] - mean);
        }
        return Math.sqrt(m2 / (v.length - 1));
    }

    /**
     * Percentile with linear interpolation between order statistics (the default "type 7" method of
     * NumPy and R).
     *
     * @param sorted values in ascending order, non-empty
     * @param p percentile in [0, 100]
     */
    static double percentile(double[] sorted, double p) {
        int n = sorted.length;
        double h = (n - 1) * p / 100.0;
        int lo = (int) Math.floor(h);
        int hi = Math.min(lo + 1, n - 1);
        return sorted[lo] + (h - lo) * (sorted[hi] - sorted[lo]);
    }

    /** Histogram bin edges (length bins+1) for equal-width bins between min and max. */
    static double[] binEdges(double min, double max, int bins) {
        if (min == max) {
            return new double[] {min, max}; // zero spread: a single bin
        }
        double[] edges = new double[bins + 1];
        double width = (max - min) / bins;
        for (int i = 0; i < bins; i++) {
            edges[i] = min + i * width;
        }
        edges[bins] = max;
        return edges;
    }

    /** Counts per bin for the edges produced by {@link #binEdges}. The maximum falls in the last bin. */
    static long[] histogramCounts(double[] values, double[] edges) {
        int bins = edges.length - 1;
        long[] counts = new long[bins];
        if (bins == 1) {
            counts[0] = values.length;
            return counts;
        }
        double min = edges[0];
        double width = (edges[bins] - edges[0]) / bins;
        for (double v : values) {
            int idx = (int) ((v - min) / width);
            if (idx < 0) {
                idx = 0;
            }
            if (idx >= bins) {
                idx = bins - 1;
            }
            counts[idx]++;
        }
        return counts;
    }

    /** Returns a sorted copy of the values. */
    static double[] sortedCopy(double[] v) {
        double[] copy = Arrays.copyOf(v, v.length);
        Arrays.sort(copy);
        return copy;
    }
}
