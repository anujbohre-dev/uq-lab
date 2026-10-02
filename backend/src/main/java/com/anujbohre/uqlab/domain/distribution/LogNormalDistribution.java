package com.anujbohre.uqlab.domain.distribution;

import java.util.random.RandomGenerator;

/**
 * Log-normal distribution, specified by the mean and standard deviation of the variable ITSELF
 * (not of its logarithm). Always positive, so it suits quantities such as modulus or length.
 *
 * <p>If X is log-normal with mean m and standard deviation s, then ln X is normal with
 * sigma^2 = ln(1 + (s/m)^2) and mu = ln(m) - sigma^2 / 2.
 */
public final class LogNormalDistribution implements Distribution {

    private final double mean;
    private final double stdDev;
    private final double mu;
    private final double sigma;

    public LogNormalDistribution(double mean, double stdDev) {
        if (!Double.isFinite(mean) || mean <= 0) {
            throw new IllegalArgumentException("LogNormal: mean must be finite and > 0");
        }
        if (!Double.isFinite(stdDev) || stdDev < 0) {
            throw new IllegalArgumentException("LogNormal: stdDev must be finite and >= 0");
        }
        this.mean = mean;
        this.stdDev = stdDev;
        double ratio = stdDev / mean;
        double sigma2 = StrictMath.log(1.0 + ratio * ratio);
        this.sigma = StrictMath.sqrt(sigma2);
        this.mu = StrictMath.log(mean) - sigma2 / 2.0;
    }

    @Override
    public double sample(RandomGenerator rng) {
        return StrictMath.exp(mu + sigma * StandardNormal.next(rng));
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
        return "LogNormal(mean=" + mean + ", std=" + stdDev + ")";
    }
}
