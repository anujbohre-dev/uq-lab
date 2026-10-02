package com.anujbohre.uqlab.domain.distribution;

import java.util.random.RandomGenerator;

/**
 * Standard normal N(0,1) variates by the Box-Muller transform.
 *
 * <p>StrictMath is used instead of Math so results are bit-for-bit identical on every JVM and
 * operating system, which makes "same seed, same result" hold everywhere.
 */
final class StandardNormal {

    private StandardNormal() {}

    static double next(RandomGenerator rng) {
        double u1;
        do {
            u1 = rng.nextDouble(); // in [0,1); log(0) is undefined, so redraw on exactly 0
        } while (u1 == 0.0);
        double u2 = rng.nextDouble();
        return StrictMath.sqrt(-2.0 * StrictMath.log(u1)) * StrictMath.cos(2.0 * Math.PI * u2);
    }
}
