package com.anujbohre.uqlab.domain.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class DistributionTest {

    /** Returns {sampleMean, sampleStdDev} of n draws. */
    private static double[] moments(Distribution d, int n, long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        double sum = 0;
        double sumSq = 0;
        for (int i = 0; i < n; i++) {
            double v = d.sample(rng);
            sum += v;
            sumSq += v * v;
        }
        double mean = sum / n;
        double var = (sumSq - n * mean * mean) / (n - 1);
        return new double[] {mean, Math.sqrt(var)};
    }

    @Test
    void normalSampleMomentsMatchParameters() {
        double[] m = moments(new NormalDistribution(100.0, 10.0), 200_000, 42L);
        assertEquals(100.0, m[0], 0.15);
        assertEquals(10.0, m[1], 0.15);
    }

    @Test
    void uniformStaysInsideBoundsAndHasCorrectMoments() {
        Distribution d = new UniformDistribution(2.0, 6.0);
        SplittableRandom rng = new SplittableRandom(7L);
        for (int i = 0; i < 10_000; i++) {
            double v = d.sample(rng);
            assertTrue(v >= 2.0 && v < 6.0);
        }
        double[] m = moments(d, 200_000, 7L);
        assertEquals(4.0, m[0], 0.02);
        assertEquals(4.0 / Math.sqrt(12.0), m[1], 0.02);
        assertEquals(4.0, d.mean(), 1e-12);
    }

    @Test
    void logNormalIsPositiveAndMomentsMatchTheVariableItself() {
        Distribution d = new LogNormalDistribution(50.0, 10.0);
        SplittableRandom rng = new SplittableRandom(11L);
        for (int i = 0; i < 10_000; i++) {
            assertTrue(d.sample(rng) > 0.0);
        }
        double[] m = moments(d, 400_000, 11L);
        assertEquals(50.0, m[0], 0.1);
        assertEquals(10.0, m[1], 0.2);
    }

    @Test
    void constantAlwaysReturnsItsValueAndHasNoSpread() {
        Distribution d = new ConstantDistribution(3.5);
        SplittableRandom rng = new SplittableRandom(1L);
        assertEquals(3.5, d.sample(rng), 0.0);
        assertEquals(3.5, d.mean(), 0.0);
        assertEquals(0.0, d.standardDeviation(), 0.0);
    }

    @Test
    void sameSeedGivesIdenticalSequence() {
        Distribution d = new NormalDistribution(0.0, 1.0);
        SplittableRandom a = new SplittableRandom(99L);
        SplittableRandom b = new SplittableRandom(99L);
        for (int i = 0; i < 1000; i++) {
            assertEquals(d.sample(a), d.sample(b), 0.0);
        }
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NormalDistribution(0.0, -1.0));
        assertThrows(IllegalArgumentException.class, () -> new UniformDistribution(5.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new LogNormalDistribution(-1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new ConstantDistribution(Double.NaN));
    }
}
