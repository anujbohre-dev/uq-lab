package com.anujbohre.uqlab.domain.distribution;

import java.util.random.RandomGenerator;

/**
 * A probability distribution for one uncertain input.
 *
 * <p>Implementations must draw randomness ONLY through {@code rng.nextDouble()}, so that a given
 * seed always produces the same sequence of samples.
 */
public interface Distribution {

    /** Draws one random value. */
    double sample(RandomGenerator rng);

    /** Theoretical mean of the distribution. */
    double mean();

    /** Theoretical standard deviation of the distribution. */
    double standardDeviation();

    /** Short human-readable description, e.g. "Normal(mean=200, std=10)". */
    String describe();
}
