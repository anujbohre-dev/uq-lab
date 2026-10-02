package com.anujbohre.uqlab.domain.distribution;

import java.util.random.RandomGenerator;

/** Normal (Gaussian) distribution. Values can be negative, even for physical inputs. */
public final class NormalDistribution implements Distribution {

    private final double mean;
    private final double stdDev;

    public NormalDistribution(double mean, double stdDev) {
        if (!Double.isFinite(mean)) {
            throw new IllegalArgumentException("Normal: mean must be a finite number");
        }
        if (!Double.isFinite(stdDev) || stdDev < 0) {
            throw new IllegalArgumentException("Normal: stdDev must be finite and >= 0");
        }
        this.mean = mean;
        this.stdDev = stdDev;
    }

    @Override
    public double sample(RandomGenerator rng) {
        return mean + stdDev * StandardNormal.next(rng);
    }

    @Override
    public double mean() {
        return mean;
    }

    @Override
    public double standardDeviation() {
        return stdDev;
    }

    @Override
    public String describe() {
        return "Normal(mean=" + mean + ", std=" + stdDev + ")";
    }
}
