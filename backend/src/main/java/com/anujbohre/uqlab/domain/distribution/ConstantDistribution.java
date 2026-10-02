package com.anujbohre.uqlab.domain.distribution;

import java.util.random.RandomGenerator;

/** A fixed, certain value (zero spread). Used for inputs that are not uncertain. */
public final class ConstantDistribution implements Distribution {

    private final double value;

    public ConstantDistribution(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Constant: value must be a finite number");
        }
        this.value = value;
    }

    @Override
    public double sample(RandomGenerator rng) {
        return value; // consumes no randomness
    }

    @Override
    public double mean() {
        return value;
    }

    @Override
    public double standardDeviation() {
        return 0.0;
    }

    @Override
    public String describe() {
        return "Constant(value=" + value + ")";
    }
}
