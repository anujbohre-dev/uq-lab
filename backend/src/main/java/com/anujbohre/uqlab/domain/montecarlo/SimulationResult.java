package com.anujbohre.uqlab.domain.montecarlo;

import java.util.List;
import java.util.Map;

/**
 * Outcome of one Monte Carlo run.
 *
 * @param samplesRequested number of samples asked for
 * @param samplesUsed samples that produced a valid (finite) output
 * @param samplesRejected samples discarded because the model returned NaN or infinity
 * @param seed seed that produced this run (re-running with it reproduces the result)
 * @param mean sample mean of the output
 * @param stdDev sample standard deviation (divisor n-1)
 * @param standardError standard error of the mean, stdDev / sqrt(samplesUsed)
 * @param min smallest output
 * @param max largest output
 * @param percentiles percentile value, keyed by percentile (1, 5, 25, 50, 75, 95, 99)
 * @param binEdges histogram bin edges (length = bins + 1)
 * @param binCounts histogram counts (length = bins)
 */
public record SimulationResult(
        int samplesRequested,
        int samplesUsed,
        int samplesRejected,
        long seed,
        double mean,
        double stdDev,
        double standardError,
        double min,
        double max,
        Map<Integer, Double> percentiles,
        List<Double> binEdges,
        List<Long> binCounts) {}
