package com.anujbohre.uqlab.domain.montecarlo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatisticsTest {

    @Test
    void meanAndSampleStdDevMatchHandCalculation() {
        double[] v = {2, 4, 4, 4, 5, 5, 7, 9};
        assertEquals(5.0, Statistics.mean(v), 1e-12);
        // sum of squared deviations = 32; sample variance = 32 / 7
        assertEquals(Math.sqrt(32.0 / 7.0), Statistics.sampleStdDev(v), 1e-12);
    }

    @Test
    void percentileUsesLinearInterpolation() {
        double[] odd = {1, 2, 3, 4, 5};
        assertEquals(3.0, Statistics.percentile(odd, 50), 1e-12);
        assertEquals(2.0, Statistics.percentile(odd, 25), 1e-12);
        assertEquals(1.0, Statistics.percentile(odd, 0), 1e-12);
        assertEquals(5.0, Statistics.percentile(odd, 100), 1e-12);
        double[] even = {1, 2, 3, 4};
        assertEquals(2.5, Statistics.percentile(even, 50), 1e-12);
    }

    @Test
    void histogramCountsEverySampleAndPutsMaximumInLastBin() {
        double[] v = {0.0, 1.0, 2.0, 3.0, 4.0};
        double[] edges = Statistics.binEdges(0.0, 4.0, 4);
        long[] counts = Statistics.histogramCounts(v, edges);
        assertEquals(4, counts.length);
        long total = 0;
        for (long c : counts) {
            total += c;
        }
        assertEquals(5L, total);
        assertEquals(2L, counts[3]); // 3.0 and 4.0 (the maximum) are in the last bin
    }

    @Test
    void zeroSpreadGivesSingleBin() {
        double[] v = {2.0, 2.0, 2.0};
        double[] edges = Statistics.binEdges(2.0, 2.0, 10);
        assertEquals(2, edges.length);
        assertEquals(3L, Statistics.histogramCounts(v, edges)[0]);
    }
}
