package com.anujbohre.uqlab.domain.distribution;

import java.util.random.RandomGenerator;

/** Continuous uniform distribution on [min, max). */
public final class UniformDistribution implements Distribution {

    private final double min;
    private final double max;

    public UniformDistribution(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            throw new IllegalArgumentException("Uniform: min and max must be finite numbers");
        }
        if (!(min < max)) {
            throw new IllegalArgumentException("Uniform: min must be less than max");
        }
        this.min = min;
        this.max = max;
    }

    @Override
    public double sample(RandomGenerator rng) {
        return min + (max - min) * rng.nextDouble();
    }

    @Override
    public double mean() {
        return (min + max) / 2.0;
    }

    @Override
    public double standardDeviation() {
        return (max - min) / StrictMath.sqrt(12.0);
    }

    @Override
    public String describe() {
        return "Uniform(min=" + min + ", max=" + max + ")";
    }
}
